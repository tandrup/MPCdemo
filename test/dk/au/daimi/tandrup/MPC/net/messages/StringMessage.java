package dk.au.daimi.tandrup.MPC.net.messages;

import java.io.Serializable;

public class StringMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	private String message;

	public StringMessage(String message) {
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
		if (obj instanceof StringMessage) {
			StringMessage other = (StringMessage) obj;
			return this.message == null ? other.message == null : this.message.equals(other.message);
		}
		return false;
	}
}
