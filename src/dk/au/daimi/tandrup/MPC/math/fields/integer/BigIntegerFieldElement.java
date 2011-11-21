package dk.au.daimi.tandrup.MPC.math.fields.integer;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Random;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.IllegalFieldException;

public class BigIntegerFieldElement implements Serializable, FieldElement {
	private static final long serialVersionUID = 1L;
	
	private BigInteger value;
	private BigIntegerField field;
	
	public BigIntegerFieldElement(int value, BigInteger modulo) {
		this(value, new BigIntegerField(modulo));
	}
	
	public BigIntegerFieldElement(BigInteger value, BigInteger modulo) {
		this(value, new BigIntegerField(modulo));
	}

	public BigIntegerFieldElement(Random randomGen, BigInteger modulo) {
		this(randomGen, new BigIntegerField(modulo));
	}

	public BigIntegerFieldElement(int value, BigIntegerField field) {
		this(BigInteger.valueOf(value), field);
	}
	
	public BigIntegerFieldElement(BigInteger value, BigIntegerField field) {
		super();
		this.field = field;
		this.value = value.mod(field.getModulo());
	}

	public BigIntegerFieldElement(Random randomGen, BigIntegerField field) {
		this(new BigInteger(field.getModulo().bitLength(), randomGen), field);
	}

	BigInteger getValue() {
		return value;
	}

	BigIntegerField getField() {
		return field;
	}

	
	
	private BigIntegerFieldElement validateOtherElement(Object arg0) {
		if (arg0 instanceof BigIntegerFieldElement) {
			BigIntegerFieldElement other = (BigIntegerFieldElement)arg0;
			if (!this.field.equals(other.field))
				throw new IllegalFieldException("Incompatible field: " + this.field + " != " + other.field);
			return other;
		}
		throw new IllegalFieldException("Incompatible type: " + arg0.getClass());
	}
	
	public FieldElement add(FieldElement arg) {
		BigIntegerFieldElement other = validateOtherElement(arg);
		return field.add(this, other);
	}

	public FieldElement add(FieldElement other, Field field) {
		return field.add(this, other);
	}

	public FieldElement negative() {
		return field.negative(this);
	}

	public FieldElement subtract(FieldElement arg) {
		BigIntegerFieldElement other = validateOtherElement(arg);
		return field.subtract(this, other);
	}

	public FieldElement inverse() {
		return field.inverse(this);
	}

	public FieldElement multiply(FieldElement arg) {
		BigIntegerFieldElement other = validateOtherElement(arg);
		return field.multiply(this, other);
	}

	public FieldElement multiply(FieldElement other, Field field) {
		return field.multiply(this, other);
	}

	public FieldElement divide(FieldElement arg) {
		BigIntegerFieldElement other = validateOtherElement(arg);
		return field.divide(this, other);
	}

	public FieldElement pow(FieldElement arg) {
		if (arg instanceof BigIntegerFieldElement) {
			BigIntegerFieldElement exp = (BigIntegerFieldElement)arg;
			return new BigIntegerFieldElement(this.value.modPow(exp.value, field.getModulo()), field);
		}
		throw new IllegalFieldException("Invalid type");
	}

	public FieldElement pow(BigInteger exp) {
		return field.pow(this, exp);
	}

	public FieldElement pow(long exp) {
		return field.pow(this, exp);
	}

	public int index() {
		return value.intValue();
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((field == null) ? 0 : field.hashCode());
		result = PRIME * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (obj instanceof BigIntegerFieldElement) {
			BigIntegerFieldElement other = (BigIntegerFieldElement)obj;
			return this.value.equals(other.value) && this.field.equals(other.field);
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return value.toString() + " mod " + field.getModulo().toString();
	}

	public String getElementString() {
		return value.toString();
	}

	public String getFieldString() {
		return field.toString();
	}

	public int compareTo(FieldElement arg) {
		BigIntegerFieldElement other = validateOtherElement(arg);

		return this.value.compareTo(other.value);
	}

	public Field field() {
		return field;
	}
}
