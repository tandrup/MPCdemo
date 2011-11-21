package dk.au.daimi.tandrup.MPC.protocols.gates;

import java.util.Arrays;
import java.util.Collection;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class AffineGate extends AbstractGate {
	private static final long serialVersionUID = 1L;

	private FieldElement xs;
	
	private transient FieldElement a0; 
	private transient AbstractGate[] gid;
	private transient FieldElement[] a;
	
	public AffineGate(int id) {
		super(id);
	}

	public void configure(FieldElement a0, AbstractGate[] gid, FieldElement[] a) {
		if (gid.length != a.length)
			throw new IllegalArgumentException("Gates array is different lengt than a array");
		
		this.a0 = a0;
		this.gid = gid;
		this.a = a;
	}

	private void compute() {
		xs = a0;
		
		for (int l = 0; l < gid.length; l++) {
			xs = xs.add(a[l].multiply(gid[l].output()));
		}
	}

	@Override
	public FieldElement output() {
		if (!isConfigured()) 
			throw new IllegalStateException("Gate is not configured");

		if (xs == null)
			compute();
		
		return xs;
	}

	@Override
	public boolean isConfigured() {
		return a0 != null && gid != null && a != null;
	}

	@Override
	public boolean isReady() {
		boolean retVal = isConfigured();

		for (int l = 0; l < gid.length; l++) {
			retVal = retVal && gid[l].isComputed(); 
		}

		return retVal;
	}

	@Override
	public boolean isComputed() {
		return isReady();
	}

	@Override
	public Collection<AbstractGate> ingressGates() {
		return Arrays.asList(gid);
	}

	@Override
	public String toString() {
		String retVal = super.toString() + " = " + a0;
		for (int i = 0; i < gid.length; i++) {
			retVal += " + " + a[i] + " * " + gid[i].getClass().getSimpleName() + gid[i].getID();
		}
		return retVal;
	}
}
