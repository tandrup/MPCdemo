package dk.au.daimi.tandrup.MPC.math.fields.integer;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldException;
import dk.au.daimi.tandrup.MPC.math.fields.IllegalFieldException;
import dk.au.daimi.tandrup.MPC.math.fields.integer.BigIntegerFieldElement;

public class BigIntegerFieldTest {
	private BigInteger modulo = BigInteger.valueOf(11);
	private BigIntegerFieldElement two;
	private BigIntegerFieldElement five;
	
	@Before
	public void setUp() throws Exception {
		two = new BigIntegerFieldElement(BigInteger.valueOf(2), modulo);
		five = new BigIntegerFieldElement(BigInteger.valueOf(5), modulo);
	}

	@Test(expected=IllegalFieldException.class)
	public void testModuloComparison() {
		BigIntegerFieldElement otherModulo = new BigIntegerFieldElement(BigInteger.valueOf(2), BigInteger.valueOf(5));
		two.add(otherModulo);
	}
	
	@Test
	public void testToString() {
		assertEquals("2 mod 11", two.toString());
		assertEquals("5 mod 11", five.toString());
	}

	@Test
	public void testAdd() {
		BigIntegerFieldElement tmp;
		tmp = (BigIntegerFieldElement)two.add(five);
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(7), modulo), tmp);

		tmp = (BigIntegerFieldElement)five.add(two);
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(7), modulo), tmp);		
	}

	@Test
	public void testNegative() {
		BigIntegerFieldElement tmp;
		tmp = (BigIntegerFieldElement)two.negative();

		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(9), modulo), tmp);		
		assertEquals(two.field().zero(), two.add(tmp));
	}

	@Test
	public void testSubtract() {
		BigIntegerFieldElement tmp;
		tmp = (BigIntegerFieldElement)five.subtract(two);
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(3), modulo), tmp);

		tmp = (BigIntegerFieldElement)two.subtract(five);
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(8), modulo), tmp);
	}

	@Test
	public void testInverse() {
		BigIntegerFieldElement tmp;
		tmp = (BigIntegerFieldElement)two.inverse();

		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(6), modulo), tmp);		
		assertEquals(two.field().one(), two.multiply(tmp));
	}

	@Test(expected=FieldException.class)
	public void testInverseZero() {
		two.field().zero().inverse();
	}

	@Test
	public void testMultiply() {
		BigIntegerFieldElement tmp;
		tmp = (BigIntegerFieldElement)five.multiply(two);
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(10), modulo), tmp);		

		tmp = (BigIntegerFieldElement)tmp.multiply(two);
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(9), modulo), tmp);		

		tmp = (BigIntegerFieldElement)two.multiply(five);
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(10), modulo), tmp);				
	}

	@Test
	public void testIdentityAdd() {
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(0), modulo), two.field().zero());
	}

	@Test
	public void testIdentityMultiply() {
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(1), modulo), two.field().one());
	}

	@Test
	public void testPowElement() {
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(10), modulo), two.pow(five));

		Field field = two.field();
		
		assertEquals(field.element(1), two.pow(0));
		assertEquals(two, two.pow(1));
		assertEquals(field.element(4), two.pow(2));
		assertEquals(field.element(8), two.pow(3));
		assertEquals(field.element(5), two.pow(4));
		assertEquals(field.element(10), two.pow(5));
		assertEquals(field.element(9), two.pow(6));
		assertEquals(field.element(7), two.pow(7));
		assertEquals(field.element(3), two.pow(8));
		assertEquals(field.element(6), two.pow(9));
		assertEquals(field.element(1), two.pow(10));
		assertEquals(field.element(2), two.pow(11));
		assertEquals(field.element(4), two.pow(12));
		assertEquals(field.element(8), two.pow(13));
		assertEquals(field.element(5), two.pow(14));
		assertEquals(field.element(10), two.pow(15));
		assertEquals(field.element(9), two.pow(16));
	}
	
	@Test
	public void testPowBigInteger() {
		BigIntegerFieldElement tmp;
		tmp = (BigIntegerFieldElement)five.pow(BigInteger.valueOf(7));
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(3), modulo), tmp);		

		tmp = (BigIntegerFieldElement)two.pow(BigInteger.valueOf(0));
		assertEquals(new BigIntegerFieldElement(BigInteger.valueOf(1), modulo), tmp);		
	}

}
