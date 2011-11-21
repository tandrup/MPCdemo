package dk.au.daimi.tandrup.MPC.math.fields.polynomial.galois;

import java.math.BigInteger;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class GFelement implements FieldElement {
	private GF field;
	private int val;

	public GFelement(GF field, int val) {
		super();
		this.field = field;
		this.val = val;
	}

	int getVal() {
		return val;
	}
	
	public FieldElement add(FieldElement other) {
		return field.add(this, other);
	}

	public FieldElement divide(FieldElement other) {
		return field.divide(this, other);
	}

	public Field field() {
		return field;
	}

	public String getElementString() {
		return Integer.toString(val);
	}

	public String getFieldString() {
		return field.toString();
	}

	public int index() {
		return val;
	}

	public FieldElement inverse() {
		return field.inverse(this);
	}

	public FieldElement multiply(FieldElement other) {
		return field.multiply(this, other);
	}

	public FieldElement negative() {
		return field.negative(this);
	}

	public FieldElement pow(BigInteger exp) {
		return field.pow(this, exp);
	}

	public FieldElement pow(long exp) {
		return field.pow(this, exp);
	}

	public FieldElement subtract(FieldElement other) {
		return field.subtract(this, other);
	}

	public int compareTo(FieldElement o) {
		return field.compare(this, (GFelement)o);
	}

	@Override
	public String toString() {
		return field.getInternalField().valueString(val);
	}
}
