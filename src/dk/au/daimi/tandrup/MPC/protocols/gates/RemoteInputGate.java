package dk.au.daimi.tandrup.MPC.protocols.gates;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Participant;

public class RemoteInputGate extends InputGate {
	private static final long serialVersionUID = 1L;

	public RemoteInputGate(int id, Participant inputProvider, FieldElement rs) {
		super(id, inputProvider, rs);
	}

	@Override
	public boolean isConfigured() {
		return true;
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public String toString() {
		if (isComputed())
			return super.toString() + "(" + inputProvider + ", " + rs + ", " + delta + ") = " + output();
		return super.toString() + "(" + inputProvider + ", " + rs + ")";
	}
}
