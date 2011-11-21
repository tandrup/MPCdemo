package dk.au.daimi.tandrup.MPC.math.fields.polynomial.galois;

import java.math.BigInteger;
import java.util.Random;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.FieldException;
import dk.au.daimi.tandrup.MPC.math.fields.IllegalFieldException;
import dk.au.daimi.tandrup.MPC.math.fields.polynomial.galois.jean.ExtendedGaloisField;
import dk.au.daimi.tandrup.MPC.math.fields.polynomial.galois.jean.GaloisField;

public class GF implements Field {
	private GaloisField field;
	
	protected GF(GaloisField field) {
		this.field = field;
	}
	
	public GF(int p) {
		field = new GaloisField(p);
	}
	
	protected GFelement validateOtherElement(Object arg0) {
		if (arg0 instanceof GFelement) {
			GFelement elem = (GFelement)arg0;
			if (!this.equals(elem.field()))
				throw new IllegalFieldException("Incompatible field: " + elem.field());
				
			return elem;
		}
		throw new IllegalFieldException("Incompatible type: " + arg0.getClass());
	}
	
	GaloisField getInternalField() {
		return field;
	}
	
	public FieldElement add(FieldElement x, FieldElement y) {
		GFelement _x = validateOtherElement(x);
		GFelement _y = validateOtherElement(y);
		return new GFelement(this, field.sum(_x.getVal(), _y.getVal()));
	}

	public FieldElement divide(FieldElement x, FieldElement y) {
		GFelement _x = validateOtherElement(x);
		GFelement _y = validateOtherElement(y);
		return new GFelement(this, field.divide(_x.getVal(), _y.getVal()));
	}

	public FieldElement element(Random random) {
		return new GFelement(this, random.nextInt(field.getCardinality()));
	}

	public FieldElement element(long i) {
		if (i < field.getCardinality())
			return new GFelement(this, (int)i);
		else
			throw new FieldException("Element is too big");
	}

	public FieldElement inverse(FieldElement x) {
		GFelement _x = validateOtherElement(x);
		return new GFelement(this, field.inverse(_x.getVal()));
	}

	public FieldElement multiply(FieldElement x, FieldElement y) {
		GFelement _x = validateOtherElement(x);
		GFelement _y = validateOtherElement(y);
		return new GFelement(this, field.product(_x.getVal(), _y.getVal()));
	}

	public FieldElement negative(FieldElement x) {
		GFelement _x = validateOtherElement(x);
		return new GFelement(this, field.negative(_x.getVal()));
	}

	public FieldElement one() {
		return new GFelement(this, ExtendedGaloisField.ONE);
	}

	public FieldElement pow(FieldElement x, BigInteger exp) {
		throw new FieldException("NOT IMPLEMENTED"); //FIXME
	}

	public FieldElement pow(FieldElement x, long exp) {
		GFelement _x = validateOtherElement(x);
		int retVal = field.ONE;

		for (int i = 0; i < exp; i++)
			retVal = field.product(retVal, _x.getVal());
		
		return new GFelement(this, retVal);
	}

	public FieldElement subtract(FieldElement x, FieldElement y) {
		GFelement _x = validateOtherElement(x);
		GFelement _y = validateOtherElement(y);
		return new GFelement(this, field.minus(_x.getVal(), _y.getVal()));
	}

	public FieldElement zero() {
		return new GFelement(this, ExtendedGaloisField.ZERO);
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
		final GF other = (GF) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		return true;
	}

	public int compare(FieldElement o1, FieldElement o2) {
		throw new FieldException("NOT IMPLEMENTED"); //FIXME
	}

	public long order() {
		throw new FieldException("NOT IMPLEMENTED"); //FIXME
	}
}
