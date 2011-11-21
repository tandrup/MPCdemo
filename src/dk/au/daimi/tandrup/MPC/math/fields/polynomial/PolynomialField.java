package dk.au.daimi.tandrup.MPC.math.fields.polynomial;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Random;

import dk.au.daimi.tandrup.MPC.math.ExtendedEuclidean;
import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.FieldException;
import dk.au.daimi.tandrup.MPC.math.fields.IllegalFieldException;

public class PolynomialField implements Field, Serializable {
	private static final long serialVersionUID = 1L;

	private FieldPolynomial1D base;
	
	private final PolynomialFieldElement ONE, ZERO;

	public PolynomialField(FieldPolynomial1D base) {
		super();
		this.base = base;
		this.ZERO = new PolynomialFieldElement(this, new FieldPolynomial1D(baseField().element(0), 0));
		this.ONE = new PolynomialFieldElement(this, new FieldPolynomial1D(baseField().element(1), 0));
	}

	FieldPolynomial1D getBasePoly() {
		return base;
	}

	protected Field baseField() {
		return base.elemField();
	}
	
	protected PolynomialFieldElement validateOtherElement(FieldElement arg0) {
		if (arg0 instanceof PolynomialFieldElement) {
			PolynomialFieldElement elem = (PolynomialFieldElement)arg0;
			if (!this.equals(elem.field()))
				throw new IllegalFieldException("Incompatible field: " + elem.field());
				
			return elem;
		}
		
		// If the base field is the same, up lift the element to the extension field
		if (arg0.field().equals(baseField())) {
			return element(arg0);
		}
		
		throw new IllegalFieldException("Incompatible field: " + arg0.field());
	}

	public FieldElement add(FieldElement x, FieldElement y) {
		PolynomialFieldElement _x = validateOtherElement(x);
		PolynomialFieldElement _y = validateOtherElement(y);
		return new PolynomialFieldElement(this, _x.getVal().add(_y.getVal()));
	}

	public FieldElement divide(FieldElement x, FieldElement y) {
		return multiply(x, inverse(y));
	}

	public FieldElement element(Random random) {
		FieldPolynomial1D polyR = new FieldPolynomial1D(baseField(), base.degree(), random);
		return new PolynomialFieldElement(this, polyR);
	}

	public FieldElement element(long i) {
		return element(baseField().element(i));
	}

	public PolynomialFieldElement element(FieldElement element) {
		return new PolynomialFieldElement(this, new FieldPolynomial1D(element, 0));
	}

	public long order() {
		long retVal = 1;
		long baseOrder = baseField().order();
		long degree = base.degree();
		
		for (int i = 0; i < degree; i++)
			retVal = retVal * baseOrder;
		
		return retVal;
	}

	public FieldElement inverse(FieldElement x) {
		PolynomialFieldElement _x = validateOtherElement(x);
		ExtendedEuclidean euc = new ExtendedEuclidean(base, _x.getVal());
		return new PolynomialFieldElement(this, euc.t());
	}

	public FieldElement multiply(FieldElement x, FieldElement y) {
		PolynomialFieldElement _x = validateOtherElement(x);
		PolynomialFieldElement _y = validateOtherElement(y);
		return new PolynomialFieldElement(this, _x.getVal().multiply(_y.getVal()));
	}

	public FieldElement negative(FieldElement x) {
		PolynomialFieldElement _x = validateOtherElement(x);
		return new PolynomialFieldElement(this, _x.getVal().negative());
	}

	public FieldElement one() {
		return ONE;
	}

	public FieldElement pow(FieldElement x, BigInteger exp) {
		throw new FieldException("NOT IMPLEMENTED");
	}

	public FieldElement pow(FieldElement x, long exp) {
		PolynomialFieldElement _x = validateOtherElement(x);
		FieldPolynomial1D retVal = new FieldPolynomial1D(baseField().one(), 0);

		for (int i = 0; i < exp; i++)
			retVal = retVal.multiply(_x.getVal());
		
		return new PolynomialFieldElement(this, retVal);
	}

	public FieldElement subtract(FieldElement x, FieldElement y) {
		PolynomialFieldElement _x = validateOtherElement(x);
		PolynomialFieldElement _y = validateOtherElement(y);
		return new PolynomialFieldElement(this, _x.getVal().substract(_y.getVal()));
	}

	public FieldElement zero() {
		return ZERO;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((base == null) ? 0 : base.hashCode());
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
		final PolynomialField other = (PolynomialField) obj;
		if (base == null) {
			if (other.base != null)
				return false;
		} else if (!base.equals(other.base))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return base.toString();
	}

	public int compare(FieldElement o1, FieldElement o2) {
		throw new FieldException("NOT IMPLEMENTED"); //FIXME
	}
}
