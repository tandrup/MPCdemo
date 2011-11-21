package dk.au.daimi.tandrup.MPC.net.ssl;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;

import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.SecurityManager;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.net.messages.TransferMessage;
import dk.au.daimi.tandrup.MPC.net.ssl.messages.AbortSessionRequest;
import dk.au.daimi.tandrup.MPC.net.ssl.messages.ConfirmSessionID;
import dk.au.daimi.tandrup.MPC.net.ssl.messages.ControlMessage;
import dk.au.daimi.tandrup.MPC.net.ssl.messages.SessionRequest;
import dk.au.daimi.tandrup.MPC.net.ssl.messages.VerificationError;

public class SSLCommunicationChannel implements CommunicationChannel,
ConnectionHandler {
	private static final Logger logger = Logger.getLogger(SSLCommunicationChannel.class.getName());

	private ConnectionListener listener = null;
	private LinkedListMessageQueue receiveQueue = new LinkedListMessageQueue();
	private KeyStore keyStore;

	public LinkedListMessageQueue getReceiveQueue() {
		return receiveQueue;
	}
	
	private SortedSet<Endpoint> validEndpoints;
	private Set<Peer> peers = new HashSet<Peer>();
	private Set<Peer> connectedPeers = new HashSet<Peer>();

	private PrivateKey userKey;
	private X509Certificate userCert;

	private Random random;

	private byte[] sessionID;

	private boolean initialized = false;

	private long timeout = 1000;

	public final Activity baseActivity = new Activity("SSLChannel");

	private SSLParticipant localParticipant;

	public SSLCommunicationChannel(int listenPort, KeyStore keyStore, Random random) throws IOException, GeneralSecurityException {
		this.keyStore = keyStore;
		listener = new ConnectionListener(listenPort, keyStore, this);
		this.random = random;
	}

	public byte[] getSessionID() {
		return sessionID;
	}

	/**
	 * Initialize Communication channel
	 * @param endpoints Other endpoints in the network
	 * @param inuserCert The certificate this server should use
	 * @param userKey  The private key this server should use
	 * @param timeout  The time in millisecond to spend on each atomic network operation
	 */
	public void init(Collection<Endpoint> endpoints, X509Certificate inuserCert,
			PrivateKey userKey, long timeout) throws IOException, GeneralSecurityException {
		synchronized (peers) {
			this.userCert = inuserCert;
			this.userKey = userKey;
			this.timeout = timeout;

			localParticipant = new LocalSSLParticipant(userCert);

			validEndpoints = new TreeSet<Endpoint>(getEndpointComparator());
			validEndpoints.addAll(endpoints);

			if (validEndpoints.size() != endpoints.size())
				throw new IllegalArgumentException("Participants contains duplicate entries");

			// Make set inmutable
			validEndpoints = Collections.unmodifiableSortedSet(validEndpoints);

			initialized = true;

			// Verify peers already connected
			Collection<Peer> invalidPeers = new ArrayList<Peer>();
			for (Peer peer : peers) {
				if (!isValidClient(peer.getCertificate())) {
					logger.info("Removing: " + peer);
					invalidPeers.add(peer);
					sendNoSession(peer, new VerificationError("Not a valid certificate: " + peer.getCertificate()));
				}
			}
			for (Peer peer : invalidPeers) {
				peer.port.stop();
			}
			peers.removeAll(invalidPeers);

			// Connect to servers
			for (Endpoint endpoint : getValidServers()) {
				ClientPort client = new ClientPort(endpoint.getAddress(), 
						endpoint.getPort(), 
						keyStore, 
						this);
				client.setReceiveQueue(receiveQueue);
				client.start();
				peers.add(new Peer(client.getCertificateChain(), client, endpoint.getAddress(), endpoint.getPort()));
			}
		}

		// Add connected peers to set of connected peers
		synchronized (connectedPeers) {
			synchronized (peers) {
				for (Peer peer : peers) {
					if (peer.port.isConnected())
						connectedPeers.add(peer);
				}
			}
			connectedPeers.notifyAll();
		}
	}

	public void startSession() throws IOException, GeneralSecurityException {
		SignedMessage sMsg;
		sessionID = null;

		// Wait for all connections to come up
		logger.config("Waiting for all connections to come up");
		synchronized (connectedPeers) {
			logger.finer("Waiting for all connections to come up");
			while (connectedPeers.size() < validEndpoints.size()) {
				logger.fine("Missing " + (validEndpoints.size() - connectedPeers.size()) + " connections");
				try {
					connectedPeers.wait();
				} catch (InterruptedException e) {
					sessionID = null;
					throw new IOException("Interrupted while waiting for all participants" + e);
				}
			}
		}

		// Creating sorted participant set
		logger.config("Creating sorted participant set");
		SortedSet<SSLParticipant> sortedPeers = new TreeSet<SSLParticipant>(getParticipantComparator());
		synchronized (peers) {
			sortedPeers.addAll(peers);
		}
		sortedPeers.add(localParticipant);

		// Setting peer ID's according to ordering
		{			
			int peerID = 0;
			for (SSLParticipant participant : sortedPeers) {
				participant.setID(peerID+1);
				peerID++;
			}
		}

		// Send session ID part
		logger.config("Sending session ID part");
		byte[] mySessionIDInitPart = new byte[SecurityManager.getSessionIDPartLength()];
		random.nextBytes(mySessionIDInitPart);
		SessionRequest msg = new SessionRequest(mySessionIDInitPart);
		broadcastNoSession(baseActivity.subActivity("SessionRequest"), msg);

		// Receive session ID from other parties
		logger.fine(localParticipant.getID() + ": Receiving session ID from other parties");
		byte[] sessionIDInitVector = new byte[SecurityManager.getSessionIDPartLength() * (peers.size()+1)];

		// Receiving messages
		for (SSLParticipant participant : sortedPeers) {			
			X509Certificate certificate = participant.getCertificate();
			byte[] sessionIDInitPart = null;
			if (certificate == userCert) {
				sessionIDInitPart = mySessionIDInitPart;
			} else {
				logger.finer(localParticipant.getID() + ": Waiting for: " + participant);
				sMsg = receiveQueue.receive(participant, baseActivity.subActivity("SessionRequest"), timeout);
				try {
					SessionRequest req = (SessionRequest)sMsg.getObject();
					sessionIDInitPart = req.getSessionIDPart();
				} catch (ClassNotFoundException e) {
					sessionID = null;
					throw new IOException("Should never happen: " + e);
				}
			}

			for (int j = 0; j < SecurityManager.getSessionIDPartLength(); j++)
				sessionIDInitVector[(participant.getID()-1) * SecurityManager.getSessionIDPartLength() + j] = sessionIDInitPart[j];
		}

		MessageDigest digest = MessageDigest.getInstance(SecurityManager.getMsgDigestAlgorithm());
		digest.update(sessionIDInitVector);
		sessionID = digest.digest();

		// Distribute newly selected session ID
		logger.fine(localParticipant.getID() + ": Distributing newly selected session ID");
		ConfirmSessionID startSessionMsg = new ConfirmSessionID(sessionID);
		broadcastNoSession(baseActivity.subActivity("ConfirmSessionID"), startSessionMsg);

		// Collect session ID and compare
		for (Peer peer : peers) {
			try {
				logger.fine(localParticipant.getID() + ": Waiting for session ID confirm from " + peer);
				sMsg = receiveQueue.receive(peer, baseActivity.subActivity("ConfirmSessionID"), timeout);
				logger.fine(localParticipant.getID() + ": Received new session ID from " + peer);
				ConfirmSessionID peerConfirm = (ConfirmSessionID)sMsg.getObject();
				if (!Arrays.equals(sessionID, peerConfirm.getSessionID())) {
					AbortSessionRequest abortMsg = new AbortSessionRequest(sessionID, sMsg);
					broadcastNoSession(baseActivity.subActivity("AbortSessionRequest"), abortMsg);
					sessionID = null;
					throw new GeneralSecurityException("SessionID mismatch with peer " + peer);
				}
			} catch (ClassNotFoundException e) {
				sessionID = null;
				IOException ioex = new IOException("Should never happen: " + e);
				logger.throwing(localParticipant.getID() + ": SSLCommunicationChannel", "startSession", ioex);
				throw ioex;
			}
		}

		logger.info(localParticipant.getID() + ": Session started");
	}

	public void handleControlMessage(ControlMessage abort) {
		logger.fine("Incoming control message: " + abort);
		if (abort instanceof AbortSessionRequest) {
			sessionID = null;
			logger.warning("Dropped session due to: " + abort);
		}
	}

	public void close()  {
		try {
			listener.close();
		} catch (IOException e) {
			logger.throwing(this.getClass().getName(), "close", e);
		}
		synchronized (peers) {
			for (Peer peer : peers) {
				peer.port.stop();
			}
		}
	}

	public synchronized SortedSet<? extends Participant> listConnectedParticipants() {
		SortedSet<SSLParticipant> sortedPeers = new TreeSet<SSLParticipant>(getParticipantComparator());
		sortedPeers.addAll(peers);
		sortedPeers.add(localParticipant);

		return sortedPeers;
	}

	public IChannelData receive(Activity activity) throws IOException, GeneralSecurityException {
		return receiveQueue.receive(activity, timeout);
	}

	public IChannelData receive(Activity activity, Participant participant) throws IOException, GeneralSecurityException {
		return receiveQueue.receive(participant, activity, timeout);
	}

	public Collection<IChannelData> receiveFromEachParticipant(Activity activity) throws IOException, GeneralSecurityException {
		return receiveFromEachParticipant(activity, new ArrayList<Participant>(peers));
	}

	public Collection<IChannelData> receiveFromEachParticipant(Activity activity, Collection<? extends Participant> participants) throws IOException, GeneralSecurityException {
		Collection<IChannelData> sMsgs = new ArrayList<IChannelData>(participants.size());
		for (Participant participant : participants) {
			SignedMessage sMsg = receiveQueue.receive(participant, activity, timeout);
			sMsgs.add(sMsg);
		}
		return sMsgs;
	}

	private void sendNoSession(Participant recv, Serializable obj) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, IOException {
		SignedMessage sObj = new SignedMessage(null, obj, userKey);

		Peer peer = lookupPeer(recv);
		peer.port.sendMessage(sObj);
	}

	public void send(Participant recv, Activity activity, Serializable obj) throws IOException, GeneralSecurityException {
		if (sessionID == null)
			throw new IllegalStateException("No session started");

		SignedMessage sMsg = new SignedMessage(activity, sessionID, obj, userKey);
		if (isLocal(recv)) {
			loopBack(sMsg);
		} else {
			Peer peer = lookupPeer(recv);
			peer.port.sendMessage(sMsg);
		}
	}

	private boolean isLocal(Participant recv) {
		return recv.equals(localParticipant);
	}

	private void loopBack(SignedMessage sMsg) {
		sMsg.setParticipant(localParticipant);
		receiveQueue.add(sMsg);
	}

	private void broadcast(SignedMessage sObj) {
		logger.entering(this.getClass().getName(), "broadcast", sObj);

		for (Peer peer : peers) {
			peer.port.sendMessage(sObj);
		}

		logger.exiting(this.getClass().getName(), "broadcast");
	}

	private void broadcastNoSession(Activity activity, Serializable obj) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, IOException {
		logger.entering(this.getClass().getName(), "broadcastNoSession", new Object[] {activity, obj});

		SignedMessage sObj = new SignedMessage(activity, obj, userKey);

		broadcast(sObj);

		logger.exiting(this.getClass().getName(), "broadcastNoSession");
	}

	public void broadcast(Activity activity, Serializable obj) throws IOException, GeneralSecurityException {
		logger.entering(this.getClass().getName(), "broadcastSession", new Object[] {activity, obj});

		SignedMessage sObj = new SignedMessage(activity, sessionID, obj, userKey);

		broadcast(sObj);

		logger.exiting(this.getClass().getName(), "broadcastSession");

	}

	public void transfer(Participant recv, Activity activity, IChannelData msg) throws IOException, GeneralSecurityException {
		send(recv, activity, new TransferMessage((SignedMessage)msg));
	}

	public void incomingConnection(SSLSocket socket) {
		try {
			logger.entering(this.getClass().getName(), "incomingConnection", socket);

			X509Certificate[] certChain = (X509Certificate[])socket.getSession().getPeerCertificates();

			logger.config("Incoming connection from : " + certChain[0]);

			// Check certificate is in valid client list
			if (initialized && !isValidClient(certChain[0]))
				throw new IllegalArgumentException("Not a valid client");

			Peer newPeer = null;
			
			synchronized (peers) {
				
				// Search for peer with already registered server port
				for (Peer peer : peers) {
					if (peer.port instanceof ServerPort &&
							peer.certificateChain[0].equals(certChain[0])) {
						logger.config("Incoming connection matched with port");
						ServerPort port = (ServerPort)(peer.port);
						port.setSocket(socket);
						return;
					}
				}

				// Create new server port
				logger.config("Incoming connection resulted in new port");
				ServerPort port = new ServerPort(keyStore, this, socket);
				port.setReceiveQueue(receiveQueue);
				port.start();

				// Create new peer
				newPeer = new Peer(certChain, port, socket.getInetAddress(), socket.getPort());
				
				// Add peer
				peers.add(newPeer);
				peers.notifyAll();
			}

			if (newPeer.port.isConnected()) {
				synchronized (connectedPeers) {
					connectedPeers.add(newPeer);
					connectedPeers.notifyAll();
				}				
			}
			
		} catch (Exception ex) {
			logger.warning("Error handling incomming connection from " + socket.getInetAddress() + ":" + socket.getPort() + " due to " + ex);
		}
	}

	private Peer lookupPeer(Participant participant) {
		synchronized (peers) {
			for (Peer peer : peers) {
				if (peer.getCertificateSubjectDN().equals(participant.getCertificateSubjectDN()))
					return peer;
			}
		}
		throw new IllegalArgumentException("Could not find participant: " + participant);
	}

	private Peer lookupPeer(Port port) {
		synchronized (peers) {
			for (Peer peer : peers) {
				if (peer.port == port) 
					return peer;
			}
		}
		return null;
	}

	private boolean isValidClient(X509Certificate cert) {
		if (!initialized)
			return false;

		for (Endpoint endpoint : getValidClients()) {
			logger.finest("Validating: " + endpoint.getCertificate());
			if (endpoint.getCertificate().equals(cert.getSubjectX500Principal().getName()))
				return true;
		}
		logger.info("Couldn't validate certificate: " + cert.getSubjectDN().toString() + " with " + getValidClients().toString());
		return false;
	}

	private SortedSet<Endpoint> getValidClients() {
		if (!initialized)
			return null;
		else
			return validEndpoints.headSet(getLocalEndpoint());
	}

	private SortedSet<Endpoint> getValidServers() {
		if (!initialized)
			return null;
		else
			return validEndpoints.tailSet(getLocalEndpoint());
	}

	private Endpoint getLocalEndpoint() {
		return new Endpoint(null, -1, userCert.getSubjectX500Principal().getName());
	}

	private Comparator<X509Certificate> getX509Comparator() {
		return new Comparator<X509Certificate>() {
			public int compare(X509Certificate cert1, X509Certificate cert2) {
				String dn1 = cert1.getSubjectX500Principal().getName("RFC2253");
				String dn2 = cert2.getSubjectX500Principal().getName("RFC2253");
				return dn1.compareTo(dn2);
			}
		};
	}

	private Comparator<Participant> getParticipantComparator() {
		return new Comparator<Participant>() {
			public int compare(Participant part1, Participant part2) {
				return getX509Comparator().compare(part1.getCertificate(), 
						part2.getCertificate());
			}
		};
	}

	private Comparator<Endpoint> getEndpointComparator() {
		return new Comparator<Endpoint>() {
			public int compare(Endpoint end1, Endpoint end2) {
				return end1.getCertificate().compareTo(end2.getCertificate());
			}
		};
	}

	public Participant localParticipant() {
		return localParticipant;
	}

	void portConnected(Port port) {
		logger.fine("Port connected " + port);
		synchronized (connectedPeers) {
			Peer peer = lookupPeer(port);

			if (peer != null) {
				logger.fine("Peer connected " + peer);

				connectedPeers.add(peer);
				connectedPeers.notifyAll();
			}
		}
	}

	void portDisconnected(Port port) {
		logger.fine("Port disconnected " + port);
		synchronized (connectedPeers) {
			Peer peer = lookupPeer(port);

			if (peer != null) {
				logger.fine("Peer disconnected " + peer);

				connectedPeers.remove(peer);
				connectedPeers.notifyAll();
			}
		}
	}
}

class LocalSSLParticipant implements SSLParticipant, Serializable {
	protected transient static final Logger logger = Logger.getLogger(LocalSSLParticipant.class.getName());

	private static final long serialVersionUID = 1L;

	private X509Certificate userCert;
	private int id;

	public LocalSSLParticipant(X509Certificate userCert) {
		super();
		this.userCert = userCert;
	}

	public X509Certificate getCertificate() { return userCert; }
	public String getCertificateSubjectDN() { return userCert.getSubjectX500Principal().getName("RFC2253"); }
	public InetAddress getInetAddress() { throw new RuntimeException("Not implemented"); }
	public int getPort() { throw new RuntimeException("Not implemented"); }
	public int getID() { return id; }
	public void setID(int id) { this.id = id; }
	public String toString() { return "LocalPeer" + id + " " + getCertificateSubjectDN(); }

	public boolean equals(Participant other) {
		return this.getCertificateSubjectDN().equals(other.getCertificateSubjectDN());
	}

	public int compareTo(Participant other) {
		return this.getCertificateSubjectDN().compareTo(other.getCertificateSubjectDN());
	}
}

class Peer implements SSLParticipant, Serializable {
	protected transient static final Logger logger = Logger.getLogger(Peer.class.getName());

	private static final long serialVersionUID = 1L;

	public X509Certificate[] certificateChain;
	public transient Port port;
	public InetAddress remoteAddress;
	public int remotePort;

	public int id;

	public Peer(X509Certificate[] certificateChain, AbstractPort port, InetAddress remoteAddress, int remotePort) {
		super();
		this.certificateChain = certificateChain;
		this.port = port;
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
		port.setParticipant(this);
	}

	public X509Certificate getCertificate() {
		return certificateChain[0];
	}

	public String getCertificateSubjectDN() {
		return getCertificate().getSubjectX500Principal().getName();
	}

	public InetAddress getInetAddress() {
		return remoteAddress;
	}

	public int getPort() {
		return remotePort;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + Arrays.hashCode(certificateChain);
		result = PRIME * result + ((remoteAddress == null) ? 0 : remoteAddress.hashCode());
		result = PRIME * result + remotePort;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof Participant) {
			return equals((Participant)obj);
		}
		return false;
	}

	public boolean equals(Participant other) {
		return this.getCertificateSubjectDN().equals(other.getCertificateSubjectDN());
	}

	public int compareTo(Participant other) {
		return this.getCertificateSubjectDN().compareTo(other.getCertificateSubjectDN());
	}

	@Override
	public String toString() {
		return "Peer" + id + " " + certificateChain[0].getSubjectX500Principal() + " (" + remoteAddress.toString() + ":" + remotePort + ")";
	}

	public int getID() {
		return id;
	}

	public void setID(int id) {
		if (id < 1) {
			RuntimeException ex = new IllegalArgumentException("ID should be greater than or equal to 1");
			logger.throwing("Peer", "setID", ex);
			throw ex;
		}

		logger.finer("Setting ID to " + id);

		this.id = id;
	}
}
