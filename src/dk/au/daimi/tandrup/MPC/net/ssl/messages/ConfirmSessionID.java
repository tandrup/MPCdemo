package dk.au.daimi.tandrup.MPC.net.ssl.messages;

import java.util.Arrays;


public class ConfirmSessionID extends ControlMessage {
	private static final long serialVersionUID = 1L;
	
	private byte[] sessionID;

	public ConfirmSessionID(byte[] sessionID) {
		super();
		this.sessionID = sessionID;
	}

	public byte[] getSessionID() {
		return sessionID;
	}

	@Override
	public String toString() {
		return "ConfirmSessionID: " + Arrays.toString(sessionID);
	}
}
