package dk.au.daimi.tandrup.MPC.net.ssl.messages;

import java.util.Arrays;


public class SessionRequest extends ControlMessage {
	private static final long serialVersionUID = 1L;

	private byte[] sessionIDPart;
	
	public SessionRequest(byte[] sessionIDPart) {
		this.sessionIDPart = sessionIDPart;
	}
	
	public byte[] getSessionIDPart() {
		return sessionIDPart;
	}
	
	@Override
	public String toString() {
		return "SessionRequest Part: " + Arrays.toString(sessionIDPart);
	}
}
