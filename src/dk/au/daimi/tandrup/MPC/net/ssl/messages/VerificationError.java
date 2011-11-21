package dk.au.daimi.tandrup.MPC.net.ssl.messages;


public class VerificationError extends ControlMessage {
	private static final long serialVersionUID = 1L;

	private String message;

	public VerificationError(String message) {
		super();
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "VerificationError: " + message;
	}
}
