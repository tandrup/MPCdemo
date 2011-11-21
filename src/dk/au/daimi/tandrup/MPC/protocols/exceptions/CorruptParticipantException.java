package dk.au.daimi.tandrup.MPC.protocols.exceptions;

import dk.au.daimi.tandrup.MPC.net.Participant;

public class CorruptParticipantException extends ProtocolException {
	private static final long serialVersionUID = 1L;

	public CorruptParticipantException(Participant p) {
		super(p + " is corrut");
	}

	public CorruptParticipantException(Participant p, Throwable t) {
		super(p + " is corrut", t);
	}
}
