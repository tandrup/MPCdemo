package dk.au.daimi.tandrup.MPC.net.ssl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.util.Arrays;

import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.SecurityManager;

public final class SignedMessage implements IChannelData, Externalizable {
	private static final long serialVersionUID = 1L;

	private SignedObject sobject = null;
	private transient Participant participant;
	private transient PrivateKey key;
	private transient MsgWrapper data;

	public SignedMessage() {
	
	}

	public SignedMessage(Activity activity, Serializable msg, PrivateKey key) throws InvalidKeyException, SignatureException, IOException {
		this(activity, null, msg, key);
	}

	public SignedMessage(Activity activity, byte[] sessionID, Serializable msg, PrivateKey key) throws InvalidKeyException, SignatureException, IOException {
		this(activity, -1, sessionID, msg, key);
	}
	
	SignedMessage(Activity activity, long messageID, Serializable msg, PrivateKey key) throws InvalidKeyException, SignatureException, IOException {
		this(activity, messageID, null, msg, key);
	}
	
	SignedMessage(Activity activity, long messageID, byte[] sessionID, Serializable msg, PrivateKey key) throws InvalidKeyException, SignatureException, IOException {
		super();
		this.key = key;
		data = new MsgWrapper(activity, messageID, sessionID, msg);
	}

	private void generateSignedObject(MsgWrapper msgWrap) throws InvalidKeyException, SignatureException, IOException {
		try {
			this.sobject = new SignedObject(msgWrap, key, Signature.getInstance(SecurityManager.getSigningAlgorithm()));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	private final void loadData() throws IOException, ClassNotFoundException {
		if (data == null)
			data = (MsgWrapper)sobject.getObject();
	}
	
	public Serializable getObject() throws IOException, ClassNotFoundException {
		loadData();
		return data.getMsg();
	}

	public Activity getActivity() throws IOException, ClassNotFoundException {
		loadData();
		return data.getActivity();
	}

	public Participant getSender() {
		return participant;
	}

	public boolean isInSession() throws IOException, ClassNotFoundException {
		loadData();
		return data.getSessionID() != null;
	}

	public byte[] getSessionID() throws IOException, ClassNotFoundException {
		loadData();
		if (data.getSessionID() != null)
			return data.getSessionID();
		else
			throw new IllegalStateException("Not in a session");
	}

	public long getMessageID() throws IOException, ClassNotFoundException {
		loadData();
		return data.getMessageID();
	}

	void setMessageID(long messageID) throws IOException, ClassNotFoundException, InvalidKeyException, SignatureException {
		if (key == null)
			throw new IllegalStateException("No key found");
		loadData();
		data.setMessageID(messageID);
		sobject = null;
	}

	public boolean verify(PublicKey verificationKey, Signature verificationEngine) throws InvalidKeyException, SignatureException {
		return sobject.verify(verificationKey, verificationEngine);
	}

	public byte[] getSignature() {
		return sobject.getSignature();
	}

	public String getAlgorithm() {
		return sobject.getAlgorithm();
	}

	public void setParticipant(Participant participant) {
		this.participant = participant;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		
		if (obj instanceof SignedMessage) {
			SignedMessage other = (SignedMessage)obj;
			
			try {
				this.loadData();
				other.loadData();
			} catch (Exception e) {
				return false;
			}

			if (this.data.equals(other.data)) {
				if (this.participant != null && other.participant != null)
					return this.participant.equals(other.participant);
				else
					return true;
			}
		}

		return super.equals(obj);
	}

	@Override
	public String toString() {
		try {
			return "SignedMessage(" + getObject() + ")";
		} catch (Exception e) {
			return "SignedMessage(Empty)";
		}
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sobject = (SignedObject)in.readObject();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		try {
			if (sobject == null)
				generateSignedObject(data);
			out.writeObject(sobject);
		} catch (InvalidKeyException e) {
			throw new IOException(e.toString());
		} catch (SignatureException e) {
			throw new IOException(e.toString());
		}
	}
}

class MsgWrapper implements Serializable {
	private static final long serialVersionUID = 1L;

	private Activity activity;
	private long messageID;
	private byte[] sessionID;
	private Serializable msg;

	public Activity getActivity() {
		return activity;
	}

	public long getMessageID() {
		return messageID;
	}

	public void setMessageID(long messageID) {
		this.messageID = messageID;
	}

	public Serializable getMsg() {
		return msg;
	}

	public byte[] getSessionID() {
		return sessionID;
	}

	public MsgWrapper(Activity activity, long messageID, byte[] sessionID, Serializable msg) {
		super();
		this.activity = activity;
		this.messageID = messageID;
		this.sessionID = sessionID;
		this.msg = msg;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((activity == null) ? 0 : activity.hashCode());
		result = PRIME * result + (int) (messageID ^ (messageID >>> 32));
		result = PRIME * result + ((msg == null) ? 0 : msg.hashCode());
		result = PRIME * result + Arrays.hashCode(sessionID);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final MsgWrapper other = (MsgWrapper) obj;
		if (activity == null) {
			if (other.activity != null)
				return false;
		} else if (!activity.equals(other.activity))
			return false;
		if (messageID != other.messageID)
			return false;
		if (msg == null) {
			if (other.msg != null)
				return false;
		} else if (!msg.equals(other.msg))
			return false;
		if (!Arrays.equals(sessionID, other.sessionID))
			return false;
		return true;
	}
}

