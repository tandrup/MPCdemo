package dk.au.daimi.tandrup.MPC.protocols.gates;

import java.util.Collection;
import java.util.Collections;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Participant;

public abstract class InputGate extends AbstractGate {
	protected Participant inputProvider;
	protected FieldElement rs;
	protected FieldElement delta;
	
	public InputGate(int id, Participant inputProvider, FieldElement rs) {
		super(id);
		this.inputProvider = inputProvider;
		this.rs = rs;
	}
	
	public Participant getInputProvider() {
		return inputProvider;
	}
	
	public void setDelta(FieldElement delta) {
		this.delta = delta;
	}

	@Override
	public FieldElement output() {
		if (!isComputed()) 
			throw new IllegalStateException("Gate is not computed");
		
		return delta.subtract(rs);
	}

	@Override
	public boolean isComputed() {
		return delta != null;
	}

	@Override
	public Collection<AbstractGate> ingressGates() {
		return Collections.emptyList();
	}
}
