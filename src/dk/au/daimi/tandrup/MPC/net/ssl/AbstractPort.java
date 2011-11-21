package dk.au.daimi.tandrup.MPC.net.ssl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;

import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.SecurityManager;
import dk.au.daimi.tandrup.MPC.net.ssl.messages.ControlMessage;

public abstract class AbstractPort implements Port {
	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	
	private Participant participant;
	protected SSLSocket socket;
	private Queue<SignedMessage> outQueue = new LinkedList<SignedMessage>();
	protected boolean running;
	protected Thread receiver, sender;
	protected SignedMessageQueue inQueue;
	protected KeyStore keyStore;

	protected SSLCommunicationChannel channel;
	
	protected final Signature sign;

	public AbstractPort(KeyStore keyStore, SSLCommunicationChannel channel) throws NoSuchAlgorithmException {
		super();
		this.keyStore = keyStore;
		this.channel = channel;
		this.sign = Signature.getInstance(SecurityManager.getSigningAlgorithm());
	}

	protected void connected() {
		channel.portConnected(this);
	}
	
	protected void disconnected() {
		channel.portDisconnected(this);
	}
	
	public boolean isConnected() {
		if (socket != null)
			return socket.isConnected();
		return false;
	}
	
	public void setParticipant(Participant participant) {
		this.participant = participant;
	}

	public void sendMessage(SignedMessage msg) {
		synchronized (outQueue) {
			outQueue.add(msg);
			outQueue.notifyAll();
		}
	}
	
	protected SignedMessage peekMessage() throws InterruptedException {
		synchronized (outQueue) {
			while (outQueue.size() < 1)
				outQueue.wait();
			return outQueue.peek();
		}
	}
	
	protected void removeMessage(SignedMessage msg) throws InterruptedException {
		synchronized (outQueue) {
			outQueue.remove(msg);
		}
	}

	public void setReceiveQueue(SignedMessageQueue queue) {
		this.inQueue = queue;
	}
	
	public X509Certificate getCertificate() {
		return getCertificateChain()[0];
	}

	public X509Certificate[] getCertificateChain() {
		try {
			return (X509Certificate[])(socket.getSession().getPeerCertificates());
		} catch (SSLPeerUnverifiedException e) {
			// Should never happen
			logger.throwing(this.getClass().getName(), "getCertificateChain", e);
			throw new IllegalStateException("Peer Unverified");
		}
	}
	
	private long incomingMessageCount = 0,
				 incomingSignatureErrors = 0, 
				 incomingSessionIDErrors = 0,
				 incomingMessageIDErrors = 0,
				 incomingCertificateErrors = 0, 
				 incomingExceptions = 0,
				 incomingNonSessionErrors = 0;

	private long lastMessageID = -1;

	private String getMessageDescrip(SignedMessage msg) {
		try {
			return msg.toString() + " (" + participant.getID() + ">" + channel.localParticipant().getID() + ") MsgID=" + msg.getMessageID();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	public void logMessage() {
		incomingMessageCount++;
	}

	public long getMessageCount() {
		return incomingMessageCount;
	}
	
	public void logException(Exception e, SignedMessage msg) {
		incomingExceptions++;
		logger.warning(channel.localParticipant().getID() + ": Dropped message: " + getMessageDescrip(msg) + " due to: " + e);		
	}

	public void logSessionIDError(SignedMessage sMsg) throws IOException, ClassNotFoundException {
		incomingSessionIDErrors++;
		logger.warning(channel.localParticipant().getID() + ": Dropped " + getMessageDescrip(sMsg) + " with invalid session id");
	}
	
	public void logMessageIDError(SignedMessage sMsg) throws IOException, ClassNotFoundException {
		//		System.out.println(participant.getID() + ">" + channel.localParticipant().getID() + ": Received message id: " + sMsg.getMessageID() + ", last: " + lastMessageID);
		incomingMessageIDErrors++;
		logger.warning(channel.localParticipant().getID() + ": Dropped " + getMessageDescrip(sMsg) + " with invalid message id: " + sMsg.getMessageID() + ", expected >= " + (lastMessageID+1));
	}
	
	public void logSignatureError(SignedMessage sMsg) throws IOException, ClassNotFoundException {
		incomingSignatureErrors++;
		logger.warning(channel.localParticipant().getID() + ": Verification failed of message " + getMessageDescrip(sMsg) + " using algorithm " + sMsg.getAlgorithm() + " and certificate " + participant.getCertificate().getSubjectDN());
	}

	public void logCertificateError() {
		incomingCertificateErrors++;
		logger.warning(channel.localParticipant().getID() + ": Certificate object is invalid: " + getCertificate());
	}
	
	public void logNonSessionError(SignedMessage sMsg) throws IOException, ClassNotFoundException {
		incomingNonSessionErrors++;
		logger.warning(channel.localParticipant().getID() + ": Dropped " + getMessageDescrip(sMsg) + " out of session, but with activity: " + sMsg.getActivity());
	}
	
	public String getStatMsg() {
		return "Incoming message stat. Count: " + incomingMessageCount+ ", SignatureErrors: " + incomingSignatureErrors + ", CertificateErrors: " + incomingCertificateErrors + ", Exceptions: " + incomingExceptions;
	}

	protected void handleIncommingMessage(SignedMessage sMsg) {
		try {
			logMessage();
			
			// Validate certificate from TLS tunnel
			/*
			if (!SecurityManager.validate(keyStore, getCertificate())) {
				logCertificateError();
				return;
			}*/
			
			// Verify signature
			if (!sMsg.verify(participant.getCertificate().getPublicKey(), sign)) {
				logSignatureError(sMsg);
				return;
			}
			sMsg.setParticipant(participant);
			
			//logger.finer(channel.localParticipant().getID() + ": Incoming message no. " + incomingMessageCount + " (" + sMsg.getActivity() + ") " + sMsg.getObject() + " from " + participant);
			
			if (incomingMessageCount % 100 == 0)
				logger.info(channel.localParticipant().getID() + ": " + getStatMsg());
			else
				logger.finest(channel.localParticipant().getID() + ": " + getStatMsg());
			
			// Verify session ID
			if (sMsg.isInSession()) {
				if (!Arrays.equals(channel.getSessionID(), sMsg.getSessionID())) {
					logSessionIDError(sMsg);
					return;
				}
			}

			// Verify message ID
			/*
			long currMessageID = sMsg.getMessageID();
			if (currMessageID <= lastMessageID) {
				logMessageIDError(sMsg);
				return;
			}
			if (currMessageID > lastMessageID +1) {
				logger.warning("Missing message between " + lastMessageID + " and " + currMessageID + ". Indicated by msg: " + getMessageDescrip(sMsg));
			}
			lastMessageID = currMessageID;
*/
			// Handle control messages, if we have a channel
			if (channel != null && sMsg.isInSession() && sMsg.getObject() instanceof ControlMessage) {
				channel.handleControlMessage((ControlMessage)sMsg.getObject());
				return;
			} 
			
			// If message is not in session, check that it is in the channel activity
			if (!sMsg.isInSession()) {
				if (!channel.baseActivity.isParentActivityOf(sMsg.getActivity())) {
					logNonSessionError(sMsg);
					return;
				}
			}
			
			// Add message to queue
			inQueue.add(sMsg);

		} catch (GeneralSecurityException e) {
			logException(e, sMsg);
		} catch (IOException e) {
			logException(e, sMsg);
		} catch (ClassNotFoundException e) {
			logException(e, sMsg);
		}
	}
	
	protected void receiverLoop() throws IOException {
		try {
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			while (running) {
				try {
					Object obj = in.readObject();
					
					if (obj instanceof SignedMessage) {
						handleIncommingMessage((SignedMessage)obj);
					} else {
						logger.warning(channel.localParticipant().getID() + ": ReceiverThread: Non signed object received: " + obj);
					}

				} catch (ClassNotFoundException ex) {
					logger.warning(channel.localParticipant().getID() + ": ReceiverThread: Unknown class received: " + ex);
				}
			}
		} catch (IOException ex) {
			logger.throwing(this.getClass().getName(), "receiverLoop()", ex);
			throw ex;
		}
	}
	
	private long messageID = 0;

	protected synchronized void senderLoop() throws IOException, InterruptedException, InvalidKeyException, SignatureException, ClassNotFoundException {
		try {
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			while (running) {
				SignedMessage msg = peekMessage();
				msg.setMessageID(messageID++);
				out.writeObject(msg);
				if (messageID % 50 == 0)
					out.reset();
				
				//try { logger.finest(channel.localParticipant().getID() + ": Outgoing message (" + msg.getActivity() + ") " + msg.getObject()); } catch (ClassNotFoundException e) { }
				removeMessage(msg);
			}
		} catch (IOException ex) {
			logger.throwing(this.getClass().getName(), "senderLoop()", ex);
			throw ex;
		} catch (InvalidKeyException ex) {
			logger.throwing(this.getClass().getName(), "senderLoop()", ex);
			throw ex;
		} catch (SignatureException ex) {
			logger.throwing(this.getClass().getName(), "senderLoop()", ex);
			throw ex;
		} catch (ClassNotFoundException ex) {
			logger.throwing(this.getClass().getName(), "senderLoop()", ex);
			throw ex;
		} catch (RuntimeException ex) {
			logger.throwing(this.getClass().getName(), "senderLoop()", ex);
			throw ex;
		}
	}

	@Override
	public String toString() {
		int remoteID = -1, localID = -1;
		
		if (participant != null)
			remoteID = participant.getID();
		
		if (channel.localParticipant() != null)
			localID = channel.localParticipant().getID();
		
		return "Port RemoteID: " + remoteID + ", LocalID: " + localID;
	}
}
