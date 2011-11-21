package dk.au.daimi.tandrup.MPC.net.messages;

import java.io.ObjectStreamException;
import java.io.Serializable;

import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.ssl.SignedMessage;

public class TransferMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	private Participant from;
	private SignedMessage signedMessage;

	public TransferMessage(SignedMessage signedMessage) {
		super();
		this.from = signedMessage.getSender();
		this.signedMessage = signedMessage;
	}

	public Participant getFrom() {
		return from;
	}

	/**
	 * Set the signed messages sender value, as it is transient
	 * @return
	 * @throws ObjectStreamException
	 */
	protected Object readResolve() throws ObjectStreamException {
		signedMessage.setParticipant(from);
		return this;
	}
	public SignedMessage getSignedMessage() {
		return signedMessage;
	}
	
	@Override
	public String toString() {
		return "TransferMessage: " + signedMessage.toString();
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((from == null) ? 0 : from.hashCode());
		result = PRIME * result + ((signedMessage == null) ? 0 : signedMessage.hashCode());
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
		final TransferMessage other = (TransferMessage) obj;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (signedMessage == null) {
			if (other.signedMessage != null)
				return false;
		} else if (!signedMessage.equals(other.signedMessage))
			return false;
		return true;
	}
	
	
}
