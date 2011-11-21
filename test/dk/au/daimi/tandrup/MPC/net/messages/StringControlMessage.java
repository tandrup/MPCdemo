package dk.au.daimi.tandrup.MPC.net.messages;

import dk.au.daimi.tandrup.MPC.net.ssl.messages.ControlMessage;

public class StringControlMessage extends ControlMessage {
	private static final long serialVersionUID = 1L;

	private String message;

	public StringControlMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "StringMessage(" + message + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			if (obj instanceof StringControlMessage) {
				StringControlMessage other = (StringControlMessage) obj;
				return this.message == null ? other.message == null : this.message.equals(other.message);
			}
		}
		return false;
	}
}
