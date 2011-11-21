/**
 * 
 */
package dk.au.daimi.tandrup.MPC.protocols;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class RandomPair {
	private FieldElement r1, r2;

	public RandomPair(FieldElement r1, FieldElement r2) {
		super();
		this.r1 = r1;
		this.r2 = r2;
	}

	public FieldElement getR1() {
		return r1;
	}

	public FieldElement getR2() {
		return r2;
	}

	@Override
	public String toString() {
		return "(" + r1 + ", " + r2 + ")";
	}
}