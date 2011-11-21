package dk.au.daimi.tandrup.MPC.protocols.gates;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class MultGate extends AbstractGate {
	private static final long serialVersionUID = 1L;

	private FieldElement aShare, bShare, cShare;
	private transient FieldElement alpha, beta;
	private transient AbstractGate inputGate1, inputGate2;
	
	public MultGate(int id, FieldElement aShare, FieldElement bShare, FieldElement cShare) {
		super(id);
		this.aShare = aShare;
		this.bShare = bShare;
		this.cShare = cShare;
	}

	public FieldElement getAShare() {
		return aShare;
	}

	public FieldElement getBShare() {
		return bShare;
	}

	public FieldElement getCShare() {
		return cShare;
	}
	
	public void setInputGates(AbstractGate inputGate1, AbstractGate inputGate2) {
		this.inputGate1 = inputGate1;
		this.inputGate2 = inputGate2;
	}

	public FieldElement getAlphaShare() {
		return inputGate1.output().add(this.aShare);
	}

	public FieldElement getBetaShare() {
		return inputGate2.output().add(this.bShare);
	}

	public FieldElement getAlpha() {
		return alpha;
	}

	public void setAlpha(FieldElement alpha) {
		this.alpha = alpha;
	}

	public FieldElement getBeta() {
		return beta;
	}

	public void setBeta(FieldElement beta) {
		this.beta = beta;
	}

	@Override
	public String toString() {
		if (isComputed())
			return super.toString() + "(" + aShare + ", " + bShare + ", " + cShare + ", " + alpha + ", " + beta + ") = " + output();

		return super.toString() + "(" + aShare + ", " + bShare + ", " + cShare + ")";
	}

	@Override
	public FieldElement output() {
		if (!isComputed()) 
			throw new IllegalStateException("Gate is not computed");
		
		FieldElement t1 = alpha.multiply(beta);
		FieldElement t2 = alpha.multiply(bShare);
		FieldElement t3 = beta.multiply(aShare);
		FieldElement t4 = cShare;
		
		return t1.subtract(t2).subtract(t3).add(t4);
	}

	@Override
	public boolean isConfigured() {
		return inputGate1 != null && inputGate2 != null;
	}

	@Override
	public boolean isReady() {
		return isConfigured() && inputGate1.isComputed() && inputGate2.isComputed();
	}

	@Override
	public boolean isComputed() {
		return alpha != null && beta != null;
	}
	
	@Override
	public Collection<AbstractGate> ingressGates() {
		return Arrays.asList(new AbstractGate[] { inputGate1, inputGate2 });
	}

	public void setIngressGates(List<AbstractGate> gates) {
		if (gates.size() != 2)
			throw new IllegalArgumentException("Mult gate takes 2 ingress gates");

	}
}
