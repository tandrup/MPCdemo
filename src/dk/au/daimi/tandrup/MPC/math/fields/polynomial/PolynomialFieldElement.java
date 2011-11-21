package dk.au.daimi.tandrup.MPC.math.fields.polynomial;

import java.io.Serializable;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.fields.AbstractFieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.FieldException;

public class PolynomialFieldElement extends AbstractFieldElement implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private FieldPolynomial1D poly;
	
	public PolynomialFieldElement(PolynomialField field, FieldPolynomial1D poly) {
		super(field);
		this.poly = poly.quotientAndRemainder(field.getBasePoly())[1];
	}

	FieldPolynomial1D getVal() {
		return poly;
	}
	
	public String getElementString() {
		return poly.toString();
	}

	public int index() {
		throw new FieldException("NOT IMPLEMENTED"); //FIXME
	}

	@Override
	public String toString() {
		return poly + "/" + field;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = super.hashCode();
		result = PRIME * result + ((poly == null) ? 0 : poly.hashCode());
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
		final PolynomialFieldElement other = (PolynomialFieldElement) obj;
		if (poly == null) {
			if (other.poly != null)
				return false;
		} else if (!poly.equals(other.poly))
			return false;
		return true;
	}
}
