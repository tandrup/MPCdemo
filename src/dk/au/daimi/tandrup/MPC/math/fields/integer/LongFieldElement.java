package dk.au.daimi.tandrup.MPC.math.fields.integer;

import java.io.Serializable;

import dk.au.daimi.tandrup.MPC.math.fields.AbstractFieldElement;

public class LongFieldElement extends AbstractFieldElement implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private long value;
	
	public LongFieldElement(long value, LongField field) {
		super(field);
		this.value = normalise(value, field.getModulo());
	}

	public LongFieldElement(long value, long modulo) {
		super(new LongField(modulo));
		this.value = normalise(value, modulo);
	}

	private static final long normalise(long element, long modulo) {
		long c = element % modulo;
		while (c < 0)
			c += modulo;
		return c;
	}

	long getValue() {
		return value;
	}
	
	public String getElementString() {
		return Long.toString(value);
	}

	public int index() {
		return (int)value;
	}

	@Override
	public String toString() {
		return Long.toString(value) + " " + field.toString();
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = super.hashCode();
		result = PRIME * result + (int) (value ^ (value >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final LongFieldElement other = (LongFieldElement) obj;
		if (value != other.value)
			return false;
		return true;
	}
}
