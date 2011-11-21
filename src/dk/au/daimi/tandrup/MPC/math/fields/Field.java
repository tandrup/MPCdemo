package dk.au.daimi.tandrup.MPC.math.fields;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Random;

public interface Field extends Comparator<FieldElement> {
	/**
	 * Get the additive identity of the field
	 * @return an element e in the field such that x + e = x
	 */
	public FieldElement zero();	
	
	/**
	 * Get the multiplicative identity of the field
	 * @return an element e in the field such that x * e = x
	 */
	public FieldElement one();
	
	/**
	 * Select a uniformly random element from the field using the supplied random generator
	 * @param random The randomness generator to use
	 * @return a uniformly random element in the field
	 */
	public FieldElement element(Random random);

	/**
	 * Select the i'th element in the field.
	 * @param i
	 * @return
	 */
	public FieldElement element(long i);

	public long order();
	
	public FieldElement add(FieldElement x, FieldElement y);
	public FieldElement subtract(FieldElement x, FieldElement y);
	public FieldElement negative(FieldElement x);
	
	public FieldElement multiply(FieldElement x, FieldElement y);
	public FieldElement divide(FieldElement x, FieldElement y);
	public FieldElement inverse(FieldElement x);

	public FieldElement pow(FieldElement x, BigInteger exp);
	public FieldElement pow(FieldElement x, long exp);
	
	public int hashCode();
	public boolean equals(Object obj);
}
