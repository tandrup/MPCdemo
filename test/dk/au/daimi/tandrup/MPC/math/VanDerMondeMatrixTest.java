package dk.au.daimi.tandrup.MPC.math;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.BigIntegerField;

public class VanDerMondeMatrixTest {
	Field field;
	
	@Before
	public void setUp() throws Exception {
		field = new BigIntegerField(BigInteger.valueOf(101));		
	}


	@Test
	public void testMultiply() {
		FieldElement[] xs = new FieldElement[] {field.element(42), field.element(73), field.element(29)};
		
		VanDerMondeMatrix m = new VanDerMondeMatrix(field, 8, 3);
		
		FieldElement[] ys = m.multiply(xs);
		
		assertEquals(8, ys.length);

		ys[4] = ys[4].add(field.element(13));
		
		FieldPolynomial1D poly = BerlekampWelch.interpolate(ys, 2);

		System.out.println(poly);
		
		assertEquals(xs, poly.coefficients());
	}
}
