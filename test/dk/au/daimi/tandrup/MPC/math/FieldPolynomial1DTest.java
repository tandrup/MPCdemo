package dk.au.daimi.tandrup.MPC.math;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.BigIntegerField;

public class FieldPolynomial1DTest {
	private Field field;
	private FieldElement[] coeffs;
	private FieldElement two;
	private FieldElement five;

	@Before
	public void setUp() throws Exception {
		field = new BigIntegerField(BigInteger.valueOf(101));
		two = field.element(2);
		five = field.element(5);

		coeffs = new FieldElement[3];
		coeffs[0] = field.element(65);
		coeffs[1] = field.element(32);
		coeffs[2] = field.element(12);
	}

	@Test
	public void testFieldElementPolynomial1DFieldElementIntRandom() {
		FieldPolynomial1D poly = new FieldPolynomial1D(five, 4, new Random());
		assertEquals(five, poly.eval(two.field().zero()));
	}

	@Test
	public void testFieldElementPolynomial1DFieldElementArray() {
		new FieldPolynomial1D(coeffs);
	}

	@Test
	public void testEval() {
		FieldPolynomial1D poly = new FieldPolynomial1D(coeffs);
		
		assertEquals(field.element(20), poly.eval(five));

		assertEquals(field.element(76), poly.eval(two));
	}

	@Test
	public void testEvalUpTo() {
		fail("Not yet implemented");
	}

	@Test
	public void testDegree() {
		FieldPolynomial1D poly;
		
		poly = new FieldPolynomial1D(five, 4, new Random());
		assertEquals(4, poly.degree());

		poly = new FieldPolynomial1D(coeffs);
		assertEquals(2, poly.degree());
		
		coeffs[2] = field.zero();
		poly = new FieldPolynomial1D(coeffs);
		assertEquals(1, poly.degree());
	}

	@Test
	public void testMultiplyFieldPolynomial1D() {
		FieldElement[] coeffs1 = new FieldElement[3];
		coeffs1[0] = field.element(2);
		coeffs1[1] = field.element(3);
		coeffs1[2] = field.element(2);
		
		FieldElement[] coeffs2 = new FieldElement[3];
		coeffs2[0] = field.element(1);
		coeffs2[1] = field.element(2);
		coeffs2[2] = field.element(5);

		FieldPolynomial1D poly1 = new FieldPolynomial1D(coeffs1);
		FieldPolynomial1D poly2 = new FieldPolynomial1D(coeffs2);
		
		FieldPolynomial1D poly3 = poly1.multiply(poly2);

		FieldElement[] coeffs3 = new FieldElement[5];
		coeffs3[0] = field.element(2);
		coeffs3[1] = field.element(7);
		coeffs3[2] = field.element(18);
		coeffs3[3] = field.element(19);
		coeffs3[4] = field.element(10);

		assertEquals(new FieldPolynomial1D(coeffs3), poly3);
	}

	@Test
	public void testQuotient() {
		FieldElement[] coeffs1 = new FieldElement[5];
		coeffs1[0] = field.element(-1);
		coeffs1[1] = field.element(1);
		coeffs1[2] = field.element(0);
		coeffs1[3] = field.element(0);
		coeffs1[4] = field.element(1);
		
		FieldElement[] coeffs2 = new FieldElement[2];
		coeffs2[0] = field.element(-1);
		coeffs2[1] = field.element(1);
		
		FieldPolynomial1D poly1 = new FieldPolynomial1D(coeffs1);
		FieldPolynomial1D poly2 = new FieldPolynomial1D(coeffs2);
		
		FieldPolynomial1D poly3 = poly1.quotient(poly2);
		
		FieldElement[] coeffs3 = new FieldElement[4];
		coeffs3[0] = field.element(2);
		coeffs3[1] = field.element(1);
		coeffs3[2] = field.element(1);
		coeffs3[3] = field.element(1);
		
		assertEquals(new FieldPolynomial1D(coeffs3), poly3);
	}
}
