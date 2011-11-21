package dk.au.daimi.tandrup.MPC.math.fields;

import java.io.Serializable;
import java.math.BigInteger;

public interface FieldElement extends Comparable<FieldElement>, Serializable {
	public FieldElement add(FieldElement other);
	public FieldElement subtract(FieldElement other);
	public FieldElement negative();
	
	public FieldElement multiply(FieldElement other);
	public FieldElement divide(FieldElement other);
	public FieldElement inverse();

	public FieldElement pow(BigInteger exp);
	public FieldElement pow(long exp);
	
	public int index();
	
	public Field field();
	
	public String getElementString();
	public String getFieldString();
}
