package dk.au.daimi.tandrup.MPC.protocols.gates;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Participant;

public class LocalInputGate extends InputGate {
	private static final long serialVersionUID = 1L;

	private FieldElement r;
	private transient FieldElement input;

	public LocalInputGate(int id, Participant inputProvider, FieldElement rs, FieldElement r) {
		super(id, inputProvider, rs);
		this.r = r;
	}

	public void setInput(FieldElement input) {
		this.input = input;
	}

	public FieldElement calculateDelta() {
		return r.add(input);
	}

	@Override
	public String toString() {
		if (isComputed())
			return super.toString() + "(" + inputProvider + ", " + rs + ", " + delta + ", " + r + ") = " + output();
		return super.toString() + "(" + inputProvider + ", " + rs + ", " + r + ")";
	}
	
	@Override
	public boolean isConfigured() {
		return input != null;
	}

	@Override
	public boolean isReady() {
		return isConfigured();
	}
}
