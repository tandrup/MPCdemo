package dk.au.daimi.tandrup.MPC.math;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.LongField;

public class BerlekampWelchTest {
	private FieldElement two;
	private FieldElement five;
	private Field field;

	@Before
	public void setUp() throws Exception {
		field = new LongField(101);
		//field = new BigIntegerField(BigInteger.valueOf(101));
		
		two = field.element(2);
		five = field.element(5);
	}

	@Test(timeout=5000)
	public void testInterpolate() {
		FieldElement[] coeffs = new FieldElement[3];
		coeffs[0] = field.element(65);
		coeffs[1] = field.element(32);
		coeffs[2] = field.element(12);
		
		FieldPolynomial1D poly = new FieldPolynomial1D(coeffs);

		FieldElement[] shares = new FieldElement[11];
		FieldElement[] indexes = new FieldElement[11];
		
		for (int i = 0; i < shares.length; i++) {
			indexes[i] = field.element(i+1);
			shares[i] = poly.eval(indexes[i]);
		}

		// Invalidate shares
		shares[2] = shares[2].negative();
		shares[4] = shares[4].add(two);
		shares[7] = shares[5].add(five);
		shares[5] = shares[5].add(five);
		//shares[1] = shares[5].add(five);
		
		FieldPolynomial1D poly2 = BerlekampWelch.interpolate(indexes, shares, 2);
		
		assertEquals(poly, poly2);
	}

	@Test(timeout=30000)
	public void testPerformance() {
		Random random = new Random();
		int threshold = 25;
		
		FieldPolynomial1D poly = new FieldPolynomial1D(two, threshold, random);
		
		System.out.println(poly);
		
		FieldElement[] shares = new FieldElement[threshold * 3 + 1];
		FieldElement[] indexes = new FieldElement[threshold * 3 + 1];
		
		for (int i = 0; i < shares.length; i++) {
			indexes[i] = field.element(i+1);
			if (i % 4 == 0)
				shares[i] = field.element(random);
			else
				shares[i] = poly.eval(indexes[i]);
		}

		long startTime, stopTime;
		
		System.out.println("Lagrange:");
		startTime = System.currentTimeMillis();
		FieldPolynomial1D poly2 = Lagrange.interpolate(indexes, shares);
		stopTime = System.currentTimeMillis();
		System.out.println(poly2);
		System.out.println("Degree: " + poly2.degree());
		System.out.println("Time: " + (stopTime - startTime));

		System.out.println("\nBerlekamp-Welch:");
		startTime = System.currentTimeMillis();
		FieldPolynomial1D poly3 = BerlekampWelch.interpolate(indexes, shares, threshold);
		stopTime = System.currentTimeMillis();

		System.out.println(poly3);
		System.out.println("Degree: " + poly3.degree());
		System.out.println("Time: " + (stopTime - startTime));
		
		assertEquals(poly, poly3);
		
		System.out.println("\nPlayer elimination:");
		startTime = System.currentTimeMillis();
		for (int i = 0; i < shares.length; i++) {
			if (!shares[i].equals(poly.eval(indexes[i]))) {
				assertTrue("Player " + i + " shouldn't be corrupt", i % 4 == 0);
				System.out.println("Player " + i + " is corrupt!");
			} else 
				assertTrue("Player " + i + " should be corrupt", i % 4 != 0);
		}
		stopTime = System.currentTimeMillis();
		System.out.println("Time: " + (stopTime - startTime));
	}
	
	public static void main(String[] args) throws IOException {
		Field field = new LongField(101);
		//Field field = new BigIntegerField(BigInteger.valueOf(101));
		
		FieldElement two = field.element(2);

		Random random = new Random();
		int threshold = 25;
		
		FieldPolynomial1D poly = new FieldPolynomial1D(two, threshold, random);
		
		System.out.println(poly);
		
		FieldElement[] shares = new FieldElement[threshold * 3 + 1];
		FieldElement[] indexes = new FieldElement[threshold * 3 + 1];
		
		for (int i = 0; i < shares.length; i++) {
			indexes[i] = field.element(i+1);
			if (i % 4 == 0)
				shares[i] = field.element(random);
			else
				shares[i] = poly.eval(indexes[i]);
		}

		long startTime, stopTime;
		
		System.in.read();
		
		System.out.println("\nBerlekamp-Welch:");
		startTime = System.currentTimeMillis();
		FieldPolynomial1D poly3 = BerlekampWelch.interpolate(indexes, shares, threshold);
		stopTime = System.currentTimeMillis();

		System.out.println(poly3);
		System.out.println("Degree: " + poly3.degree());
		System.out.println("Time: " + (stopTime - startTime));
	}
}
