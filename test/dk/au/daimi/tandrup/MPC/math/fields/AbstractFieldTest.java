package dk.au.daimi.tandrup.MPC.math.fields;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.Random;

import org.junit.Test;

public abstract class AbstractFieldTest {
	protected Field field;
	protected FieldElement two;
	protected FieldElement five;

	@Test
	public void testZero() {
		FieldElement zero = field.zero();
		assertEquals(field.zero(), zero);

		zero = zero.add(zero);
		assertEquals(field.zero(), zero);

		zero = zero.add(zero);
		assertEquals(field.zero(), zero);

		assertEquals(two, zero.add(two));
		assertEquals(two, two.add(zero));
	}

	@Test
	public void testOne() {
		FieldElement one = field.one();
		assertEquals(field.one(), one);

		one = one.multiply(one);
		assertEquals(field.one(), one);

		one = one.multiply(one);
		assertEquals(field.one(), one);

		assertEquals(two, one.multiply(two));
		assertEquals(two, two.multiply(one));
		assertEquals(five, five.multiply(one));
	}

	@Test
	public void testElementRandom() {
		Random random = new Random();
		
		FieldElement r;
		
		r = field.element(random);
		
		System.out.println(r);
		assertEquals(field.one(), r.multiply(r.inverse()));
	}
	
	@Test
	public void testElement() {
		assertEquals(field.one(), field.element(1));
		assertEquals(field.one(), field.element(12));
		assertEquals(field.one().add(field.one()), field.element(2));
		assertEquals(field.one().add(field.one()), field.element(13));
		assertEquals(two, field.element(2));
	}
	
	@Test
	public void testAdd() {
		FieldElement tmp;
		tmp = two.add(five);
		assertEquals(field.element(7), tmp);

		tmp = two.add(two);
		assertEquals(field.element(4), tmp);

		tmp = five.add(two);
		assertEquals(field.element(7), tmp);		

		tmp = five.add(five);
		assertEquals(field.element(10), tmp);

		tmp = tmp.add(five);
		assertEquals(field.element(4), tmp);
	}

	@Test
	public void testSubtract() {
		FieldElement tmp;
		tmp = five.subtract(two);
		assertEquals(field.element(3), tmp);

		tmp = two.subtract(five);
		assertEquals(field.element(8), tmp);

		tmp = five.subtract(two.pow(8));
		assertEquals(field.element(2), tmp);

	
	}

	@Test
	public void testNegative() {
		FieldElement tmp;
		
		tmp = two.negative();
		assertEquals(field.element(9), tmp);
		assertEquals(field.zero(), two.add(tmp));

		tmp = five.negative();
		assertEquals(field.element(6), tmp);
		assertEquals(field.zero(), five.add(tmp));
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
	public void testDivide() {
		FieldElement tmp;
		tmp = five.divide(two);
		assertEquals(field.element(8), tmp);
		assertEquals(five, tmp.multiply(two));

		tmp = tmp.divide(two);
		assertEquals(field.element(4), tmp);
		assertEquals(five, tmp.multiply(two).multiply(two));

		tmp = two.divide(five);
		assertEquals(field.element(7), tmp);				
		assertEquals(two, tmp.multiply(five));
	}

	@Test
	public void testInverse() {
		FieldElement tmp;
		
		tmp = two.inverse();
		assertEquals(field.element(6), tmp);	
		assertEquals(field.one(), two.multiply(tmp));

		tmp = five.inverse();
		assertEquals(field.element(9), tmp);	
		assertEquals(field.one(), five.multiply(tmp));
	}

	@Test(expected=FieldException.class)
	public void testInverseZero() {
		field.zero().inverse();
	}

	@Test
	public void testPowElement() {
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
		FieldElement tmp;
		tmp = five.pow(BigInteger.valueOf(7));
		assertEquals(field.element(3), tmp);		

		tmp = two.pow(BigInteger.valueOf(0));
		assertEquals(field.element(1), tmp);		
	}
}
