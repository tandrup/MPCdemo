package dk.au.daimi.tandrup.MPC.net.ssl.messages;

import dk.au.daimi.tandrup.MPC.net.ssl.SignedMessage;

public class AbortSessionRequest extends ControlMessage  {
	private static final long serialVersionUID = 1L;
	
	private byte[] mySessionID;
	private SignedMessage offendingMessage;
	public AbortSessionRequest(byte[] mySessionID, SignedMessage offendingMessage) {
		super();
		this.mySessionID = mySessionID;
		this.offendingMessage = offendingMessage;
	}

	public byte[] getCalculatedSessionID() {
		return mySessionID;
	}
	
	public SignedMessage getOffendingMessage() {
		return offendingMessage;
	}
}
