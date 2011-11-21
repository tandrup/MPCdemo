package dk.au.daimi.tandrup.MPC.protocols.gates;

import java.util.Collection;
import java.util.Collections;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class RandomGate extends AbstractGate {
	private static final long serialVersionUID = 1L;

	private FieldElement rs;

	public RandomGate(int id, FieldElement rs) {
		super(id);
		this.rs = rs;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + rs + ")";
	}

	@Override
	public FieldElement output() {
		return rs;
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
	public boolean isComputed() {
		return true;
	}
	
	@Override
	public Collection<AbstractGate> ingressGates() {
		return Collections.emptyList();
	}
}
