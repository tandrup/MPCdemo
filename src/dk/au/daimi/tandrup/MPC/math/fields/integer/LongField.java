package dk.au.daimi.tandrup.MPC.math.fields.integer;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Random;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.FieldException;
import dk.au.daimi.tandrup.MPC.math.fields.IllegalFieldException;

public class LongField implements Field, Serializable {
	private static final long serialVersionUID = 1L;

	private long modulo;
	
	// Some values are preallocated for better performance
	private static final int PREALLOCATECOUNT = 2;
	private transient LongFieldElement ONE, ZERO;
	private transient LongFieldElement[] vals;
	
	public LongField(long modulo) {
		this.modulo = modulo;
		precalculate();
	}

	private void precalculate() {
		if (modulo < PREALLOCATECOUNT)
			vals = new LongFieldElement[(int)modulo];
		else
			vals = new LongFieldElement[PREALLOCATECOUNT];
		
		for (int i = 0; i < vals.length; i++)
			vals[i] = new LongFieldElement(i, this);
		
		this.ZERO = vals[0];
		this.ONE = vals[1];
	}
	
	private Object readResolve() throws ObjectStreamException {
		precalculate();
		return this;
	}
	
	long getModulo() {
		return modulo;
	}
	
	protected LongFieldElement validateOtherElement(FieldElement arg0) {
		if (arg0 instanceof LongFieldElement) {
			LongFieldElement elem = (LongFieldElement)arg0;
			if (!this.equals(elem.field()))
				throw new IllegalFieldException("Incompatible field: " + elem.field());
				
			return elem;
		}
		throw new IllegalFieldException("Incompatible type: " + arg0.field());
	}
	
	public FieldElement add(FieldElement x, FieldElement y) {
		LongFieldElement _x = validateOtherElement(x);
		LongFieldElement _y = validateOtherElement(y);
		return new LongFieldElement(_x.getValue() + _y.getValue(), this);
	}

	public FieldElement subtract(FieldElement x, FieldElement y) {
//		This is too inefective: return add(x, negative(y));
		LongFieldElement _x = validateOtherElement(x);
		LongFieldElement _y = validateOtherElement(y);
		return new LongFieldElement(_x.getValue() - _y.getValue(), this);
	}

	public FieldElement negative(FieldElement x) {
		LongFieldElement _x = validateOtherElement(x);
		return new LongFieldElement(- _x.getValue(), this);
	}

	public FieldElement multiply(FieldElement x, FieldElement y) {
		LongFieldElement _x = validateOtherElement(x);
		LongFieldElement _y = validateOtherElement(y);
		return new LongFieldElement(_x.getValue() * _y.getValue(), this);
	}

	public FieldElement divide(FieldElement x, FieldElement y) {
		return multiply(x, inverse(y));
	}

	public FieldElement inverse(FieldElement x) {
		LongFieldElement _x = validateOtherElement(x);
		long[] u = gcd(modulo, _x.getValue());
		if (_x.getValue() == 0)
			throw new FieldException("Zero is not invertible");
		return new LongFieldElement(u[1], this);
	}

	public FieldElement element(Random random) {
		long r = random.nextLong();
		if (r < 0)
			r = - r;
		return new LongFieldElement(r, this);
	}

	public FieldElement element(long i) {
		if (i < vals.length)
			return vals[(int)i];
		return new LongFieldElement((int)i, this);
	}

	private static long[] gcd(long x, long y) {
		long[] u = {1, 0, x}, v = {0, 1, y}, t = new long[3];
		while (v[2] != 0) {
			long q = u[2]/v[2];
			for (int i = 0; i < 3; i++) {
				t[i] = u[i] - v[i]*q; u[i] = v[i]; v[i] = t[i];
			}
		}
		return u;
	}
	
	public FieldElement pow(FieldElement x, BigInteger exp) {
		throw new FieldException("NOT IMPLEMENTED"); //FIXME
	}

	public FieldElement pow(FieldElement x, long exp) {
		LongFieldElement _x = validateOtherElement(x);
		return new LongFieldElement(exp(_x.getValue(), exp), this);
	}

	private long exp(long X, long Y) {
		long x = X, y = Y, z = 1;
		while (y > 0) {
			while (y%2 == 0) {
				x = x*x % modulo;
				y = y/2;
			}
			z = z*x % modulo;
			y = y - 1;
		}
		return z;
	}

	public FieldElement zero() {
		return ZERO;
	}

	public FieldElement one() {
		return ONE;
	}

	public int compare(FieldElement x, FieldElement y) {
		LongFieldElement _x = validateOtherElement(x);
		LongFieldElement _y = validateOtherElement(y);
		if (_x.getValue() < _y.getValue())
			return -1;
		else if (_x.getValue() > _y.getValue())
			return 1;
		else 
			return 0;
	}

	@Override
	public String toString() {
		return "mod " + modulo;
	}

	public long order() {
		return modulo;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + (int) (modulo ^ (modulo >>> 32));
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
		final LongField other = (LongField) obj;
		if (modulo != other.modulo)
			return false;
		return true;
	}
}
