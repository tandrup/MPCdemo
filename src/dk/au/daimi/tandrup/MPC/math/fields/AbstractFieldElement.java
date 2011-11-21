package dk.au.daimi.tandrup.MPC.math.fields;

import java.io.Serializable;
import java.math.BigInteger;


public abstract class AbstractFieldElement implements FieldElement, Serializable {
	protected Field field;
	
	protected AbstractFieldElement(Field field) {
		this.field = field;
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

	public String getFieldString() {
		return field.toString();
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

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((field == null) ? 0 : field.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AbstractFieldElement other = (AbstractFieldElement) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		return true;
	}

	public int compareTo(FieldElement o) {
		return field.compare(this, o);
	}
}
