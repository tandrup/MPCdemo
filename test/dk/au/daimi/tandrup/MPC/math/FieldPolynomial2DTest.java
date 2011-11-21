package dk.au.daimi.tandrup.MPC.math;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.fields.integer.BigIntegerFieldElement;

public class FieldPolynomial2DTest {
	private BigInteger mod = BigInteger.valueOf(101);
	private BigIntegerFieldElement[][] coeffs;
	private BigIntegerFieldElement zero;
	private BigIntegerFieldElement two;
	private BigIntegerFieldElement five;

	@Before
	public void setUp() throws Exception {
		zero = new BigIntegerFieldElement(BigInteger.valueOf(0), mod);
		two = new BigIntegerFieldElement(BigInteger.valueOf(2), mod);
		five = new BigIntegerFieldElement(BigInteger.valueOf(5), mod);

		coeffs = new BigIntegerFieldElement[3][3];
		coeffs[0][0] = new BigIntegerFieldElement(BigInteger.valueOf(65), mod);
		coeffs[1][0] = new BigIntegerFieldElement(BigInteger.valueOf(32), mod);
		coeffs[2][0] = new BigIntegerFieldElement(BigInteger.valueOf(12), mod);

		coeffs[0][1] = new BigIntegerFieldElement(BigInteger.valueOf(76), mod);
		coeffs[1][1] = new BigIntegerFieldElement(BigInteger.valueOf(21), mod);
		coeffs[2][1] = new BigIntegerFieldElement(BigInteger.valueOf(17), mod);

		coeffs[0][2] = new BigIntegerFieldElement(BigInteger.valueOf(6), mod);
		coeffs[1][2] = new BigIntegerFieldElement(BigInteger.valueOf(45), mod);
		coeffs[2][2] = new BigIntegerFieldElement(BigInteger.valueOf(73), mod);
	}

	@Test
	public void testFieldElementPolynomial2DFieldElementIntRandom() {
		FieldPolynomial2D poly = new FieldPolynomial2D(five, 4, new Random());
		assertEquals(five, poly.eval(zero, zero));
	}

	@Test
	public void testFieldElementPolynomial2DFieldElementArrayArray() {
		new FieldPolynomial2D(coeffs);
	}

	@Test
	public void testEval() {
		FieldPolynomial2D poly = new FieldPolynomial2D(coeffs);

		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(65), mod), poly.eval(zero, zero));

		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(63), mod), poly.eval(five, two));

		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(81), mod), poly.eval(two, two));

	}

	@Test
	public void testEvalPartial1() {
		FieldPolynomial2D poly2D = new FieldPolynomial2D(coeffs);

		FieldPolynomial1D poly1D = poly2D.evalPartial1(two);
		
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(76), mod), poly1D.eval(zero));
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(81), mod), poly1D.eval(two));
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(0), mod), poly1D.eval(five));
	}

	@Test
	public void testEvalPartial2() {
		FieldPolynomial2D poly2D = new FieldPolynomial2D(coeffs);

		FieldPolynomial1D poly1D = poly2D.evalPartial2(two);
		
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(39), mod), poly1D.eval(zero));
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(81), mod), poly1D.eval(two));
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(63), mod), poly1D.eval(five));		
	}

	@Test
	public void testDegree() {
		FieldPolynomial2D poly;
		
		poly = new FieldPolynomial2D(five, 4, new Random());
		assertEquals(4, poly.degree());

		poly = new FieldPolynomial2D(coeffs);	
		assertEquals(2, poly.degree());
	}
}
