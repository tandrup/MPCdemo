package dk.au.daimi.tandrup.MPC.protocols.messages;

import java.io.Serializable;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;

public class VssShare implements Serializable {
	private static final long serialVersionUID = 1L;
	public FieldPolynomial1D f, g;

	public VssShare(FieldPolynomial1D f, FieldPolynomial1D g) {
		super();
		this.f = f;
		this.g = g;
	}

	@Override
	public String toString() {
		return "VSS Share " + f + ", " + g + ")";
	}
}
