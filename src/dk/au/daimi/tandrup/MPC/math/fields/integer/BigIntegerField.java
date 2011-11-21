/**
 * 
 */
package dk.au.daimi.tandrup.MPC.math.fields.integer;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Random;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.FieldException;
import dk.au.daimi.tandrup.MPC.math.fields.IllegalFieldException;

public class BigIntegerField implements Field, Serializable {
	private static final long serialVersionUID = 1L;
	private BigInteger modulo;

	public BigIntegerField(BigInteger modulo) {
		this.modulo = modulo;
	}

	public FieldElement element(Random random) {
	    return new BigIntegerFieldElement(random, modulo);
	}

	public FieldElement element(long index) {
	    return new BigIntegerFieldElement(BigInteger.valueOf(index), modulo);
	}

	public FieldElement one() {
		return new BigIntegerFieldElement(BigInteger.ONE, modulo);
	}

	public FieldElement zero() {
		return new BigIntegerFieldElement(BigInteger.ZERO, modulo);
	}
	
	public BigInteger getModulo() {
		return modulo;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((modulo == null) ? 0 : modulo.hashCode());
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
		final BigIntegerField other = (BigIntegerField) obj;
		if (modulo == null) {
			if (other.modulo != null)
				return false;
		} else if (!modulo.equals(other.modulo))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "mod " + modulo;
	}

	protected BigIntegerFieldElement validateOtherElement(Object arg0) {
		if (arg0 instanceof BigIntegerFieldElement) {
			return (BigIntegerFieldElement)arg0;
		}
		throw new IllegalFieldException("Incompatible type: " + arg0.getClass());
	}
	
	public FieldElement add(FieldElement x, FieldElement y) {
		BigIntegerFieldElement _x = validateOtherElement(x);
		BigIntegerFieldElement _y = validateOtherElement(y);
		return new BigIntegerFieldElement(_x.getValue().add(_y.getValue()), this);
	}

	public FieldElement subtract(FieldElement x, FieldElement y) {
		BigIntegerFieldElement _x = validateOtherElement(x);
		BigIntegerFieldElement _y = validateOtherElement(y);
		return new BigIntegerFieldElement(_x.getValue().subtract(_y.getValue()), this);
	}

	public FieldElement negative(FieldElement x) {
		BigIntegerFieldElement _x = validateOtherElement(x);
		return new BigIntegerFieldElement(_x.getValue().negate(), this);
	}

	public FieldElement multiply(FieldElement x, FieldElement y) {
		BigIntegerFieldElement _x = validateOtherElement(x);
		BigIntegerFieldElement _y = validateOtherElement(y);
		return new BigIntegerFieldElement(_x.getValue().multiply(_y.getValue()), this);
	}

	public FieldElement divide(FieldElement x, FieldElement y) {
		return multiply(x, inverse(y));
	}

	public FieldElement inverse(FieldElement x) {
		try {
			BigIntegerFieldElement _x = validateOtherElement(x);
			return new BigIntegerFieldElement(_x.getValue().modInverse(modulo), this);
		} catch (ArithmeticException ex) {
			throw new FieldException(ex);
		}
	}

	public FieldElement pow(FieldElement x, BigInteger exp) {
		BigIntegerFieldElement _x = validateOtherElement(x);
		return new BigIntegerFieldElement(_x.getValue().modPow(exp, modulo), this);
	}

	public FieldElement pow(FieldElement x, long exp) {
		BigIntegerFieldElement _x = validateOtherElement(x);
		BigInteger _exp = BigInteger.valueOf(exp);
		return new BigIntegerFieldElement(_x.getValue().modPow(_exp, modulo), this);
	}

	public int compare(FieldElement x, FieldElement y) {
		BigIntegerFieldElement _x = validateOtherElement(x);
		BigIntegerFieldElement _y = validateOtherElement(y);
		return _x.compareTo(_y);
	}

	public long order() {
		return modulo.longValue();
	}
}