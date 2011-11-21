package dk.au.daimi.tandrup.MPC.protocols.gates;

import java.util.Arrays;
import java.util.Collection;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class OutputGate extends AbstractGate {
	private static final long serialVersionUID = 1L;

	private transient AbstractGate inputGate = null;
	private transient FieldElement result = null;
	
	public OutputGate(int id) {
		super(id);
	}

	public void setInput(AbstractGate inputGate) {
		this.inputGate = inputGate;
	}

	public void setResult(FieldElement result) {
		this.result = result;
	}

	@Override
	public boolean isConfigured() {
		return inputGate != null;
	}

	@Override
	public boolean isReady() {
		return inputGate.isComputed();
	}

	@Override
	public boolean isComputed() {
		return result != null;
	}

	@Override
	public FieldElement output() {
		return result;
	}

	public FieldElement getInputShare() {
		return inputGate.output();
	}
	
	@Override
	public Collection<AbstractGate> ingressGates() {
		return Arrays.asList(new AbstractGate[] { inputGate });
	}

	@Override
	public String toString() {
		if (isComputed())
			return super.toString() + " = " + result;
		
		return super.toString();
	}
}
