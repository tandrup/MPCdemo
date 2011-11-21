package dk.au.daimi.tandrup.MPC.math;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.BigIntegerFieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.LongField;

public class LagrangeTest {
	private BigInteger modulo = BigInteger.valueOf(101);

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testInterpolate() {
		FieldElement[] coeffs = new FieldElement[3];
		coeffs[0] = new BigIntegerFieldElement(BigInteger.valueOf(65), modulo);
		coeffs[1] = new BigIntegerFieldElement(BigInteger.valueOf(32), modulo);
		coeffs[2] = new BigIntegerFieldElement(BigInteger.valueOf(12), modulo);
		Field field = coeffs[0].field();
		FieldPolynomial1D poly = new FieldPolynomial1D(coeffs);
		
		FieldElement[] shares = new FieldElement[5];
		FieldElement[] shareIndexes = new FieldElement[5];
		
		for (int i = 0; i < shares.length; i++) {
			shareIndexes[i] = field.element(i*2+2);
			shares[i] = poly.eval(shareIndexes[i]);
		}
		
		FieldPolynomial1D receivedPoly;
		
		receivedPoly = Lagrange.interpolate(shareIndexes, shares);
		assertEquals("Received Secret", poly, receivedPoly);

		shares = new FieldElement[8];
		shareIndexes = new FieldElement[8];
		
		for (int i = 0; i < shares.length; i++) {
			shareIndexes[i] = field.element(i+2);
			shares[i] = poly.eval(shareIndexes[i]);
		}
		
		receivedPoly = Lagrange.interpolate(shareIndexes, shares);
		assertEquals("Received Secret", poly, receivedPoly);
	}

	@Test
	public void testNegation() {
		Field field = new LongField(101);
		FieldElement[] coeffs = new FieldElement[3];
		coeffs[0] = field.element(65);
		coeffs[1] = field.element(32);
		coeffs[2] = field.element(12);
		FieldPolynomial1D poly = new FieldPolynomial1D(coeffs);
		
		FieldElement[] shares = new FieldElement[5];
		FieldElement[] shareIndexes = new FieldElement[5];
		
		for (int i = 0; i < shares.length; i++) {
			shareIndexes[i] = field.element(i*2+2);
			shares[i] = field.zero().subtract(poly.eval(shareIndexes[i]));
		}
		
		FieldPolynomial1D receivedPoly = Lagrange.interpolate(shareIndexes, shares);
		assertEquals("Received Secret", coeffs[0].negative(), receivedPoly.eval(field.zero()));
	}

	@Test
	public void testEval() {
		Field field = new LongField(11);
		FieldElement[] shares = new FieldElement[5];
		FieldElement[] shareIndexes = new FieldElement[5];
		FieldPolynomial1D poly;
		for (int i = 0; i < shares.length; i++) {
			shareIndexes[i] = field.element(i+1);
		}
		
		// mult
		shares[0] = field.element(4);
		shares[1] = field.element(10);
		shares[2] = field.element(5);
		shares[3] = field.element(0);
		shares[4] = field.element(6);
		poly = Lagrange.interpolate(shareIndexes, shares);
		System.out.println("EVAL MULT: " + poly);
	}
	
	@Test
	public void testTriples() {
		Field field = new LongField(11);
		FieldElement[] shares = new FieldElement[5];
		FieldElement[] shareIndexes = new FieldElement[5];
		FieldPolynomial1D poly;
		for (int i = 0; i < shares.length; i++) {
			shareIndexes[i] = field.element(i+1);
		}
		
		// a
		shares[0] = field.element(4);
		shares[1] = field.element(4);
		shares[2] = field.element(4);
		shares[3] = field.element(4);
		shares[4] = field.element(4);
		poly = Lagrange.interpolate(shareIndexes, shares);
		System.out.println("a: " + poly);
		
		// b
		shares[0] = field.element(9);
		shares[1] = field.element(6);
		shares[2] = field.element(3);
		shares[3] = field.element(0);
		shares[4] = field.element(8);
		poly = Lagrange.interpolate(shareIndexes, shares);
		System.out.println("b: " + poly);
		
		// c
		shares[0] = field.element(5);
		shares[1] = field.element(6);
		shares[2] = field.element(7);
		shares[3] = field.element(8);
		shares[4] = field.element(9);
		poly = Lagrange.interpolate(shareIndexes, shares);
		System.out.println("c: " + poly);

	}
}
