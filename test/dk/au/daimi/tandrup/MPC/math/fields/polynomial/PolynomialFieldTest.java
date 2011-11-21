package dk.au.daimi.tandrup.MPC.math.fields.polynomial;


import static org.junit.Assert.assertEquals;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.BigIntegerField;
import dk.au.daimi.tandrup.MPC.math.fields.polynomial.PolynomialField;
import dk.au.daimi.tandrup.MPC.math.fields.polynomial.PolynomialFieldElement;

public class PolynomialFieldTest {
	private Field baseField;
	private FieldPolynomial1D basePoly;
	private PolynomialField field;
	private FieldElement two;
	private FieldElement five;
	private FieldElement twofive;

	@Before
	public void setUp() throws Exception {
		baseField = new BigIntegerField(BigInteger.valueOf(11));
		basePoly = new FieldPolynomial1D(new FieldElement[] {baseField.element(7), baseField.element(1), baseField.element(1)});
		field = new PolynomialField(basePoly);
		two = field.element(2);
		five = field.element(5);
		twofive = new PolynomialFieldElement(field, new FieldPolynomial1D(new FieldElement[] {baseField.element(2), baseField.element(5)}));
	}

	@Test
	public void testToString() {
		assertEquals("f(x) = 2 mod 11/f(x) = 7 + 1*x + 1*x^2 mod 11", two.toString());
		assertEquals("f(x) = 5 mod 11/f(x) = 7 + 1*x + 1*x^2 mod 11", five.toString());
		assertEquals("f(x) = 2 + 5*x mod 11/f(x) = 7 + 1*x + 1*x^2 mod 11", twofive.toString());
	}

	@Test
	public void testAdd() {
		FieldElement tmp;
		tmp = two.add(five);
		assertEquals(field.element(7), tmp);

		tmp = five.add(two);
		assertEquals(field.element(7), tmp);		
	}

	@Test
	public void testNegative() {
		FieldElement tmp;
		tmp = two.negative();

		assertEquals(field.element(9), tmp);
		assertEquals(two.field().zero(), two.add(tmp));

		assertEquals("f(x) = 0 mod 11/f(x) = 7 + 1*x + 1*x^2 mod 11", two.add(tmp).toString());
	}

	@Test
	public void testSubtract() {
		FieldElement tmp;
		tmp = five.subtract(two);
		assertEquals(field.element(3), tmp);

		tmp = two.subtract(five);
		assertEquals(field.element(8), tmp);
	}

	@Test
	public void testInverse() {
		FieldElement tmp;
		tmp = two.inverse();

		assertEquals(field.element(6), tmp);	
		assertEquals(two.field().one(), two.multiply(tmp));
	}

	@Test
	public void testMultiply() {
		FieldElement tmp;
		tmp = five.multiply(two);
		assertEquals(field.element(10), tmp);		

		tmp = tmp.multiply(two);
		assertEquals(field.element(9), tmp);		

		tmp = two.multiply(five);
		assertEquals(field.element(10), tmp);				
	}

	@Test
	public void testIdentityAdd() {
		assertEquals(field.element(0), two.field().zero());
	}

	@Test
	public void testIdentityMultiply() {
		assertEquals(field.element(1), two.field().one());
	}

	@Test
	public void testPowElement() {
		FieldElement tmp;
		
		assertEquals(field.element(1), two.pow(0));
		assertEquals(two, two.pow(1));
		assertEquals(field.element(4), two.pow(2));
		assertEquals(field.element(8), two.pow(3));
		assertEquals(field.element(10), two.pow(5));

		assertEquals(twofive, twofive.pow(1));
		
		tmp = new PolynomialFieldElement(field, new FieldPolynomial1D(new FieldElement[] {baseField.element(5), baseField.element(6)}));
		assertEquals(tmp, twofive.pow(2));
		tmp = new PolynomialFieldElement(field, new FieldPolynomial1D(new FieldElement[] {baseField.element(9), baseField.element(7)}));
		assertEquals(tmp, twofive.pow(3));

	}
	
	@Test
	public void testPowBigInteger() {
		FieldElement tmp;
		tmp = five.pow(BigInteger.valueOf(7));
		assertEquals(field.element(3), tmp);		

		tmp = two.pow(BigInteger.valueOf(0));
		assertEquals(field.element(1), tmp);		
	}
}
