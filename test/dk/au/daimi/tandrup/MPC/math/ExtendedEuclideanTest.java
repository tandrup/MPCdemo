package dk.au.daimi.tandrup.MPC.math;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.BigIntegerField;

public class ExtendedEuclideanTest {
	private Field field;

	@Before
	public void setUp() throws Exception {
		field = new BigIntegerField(BigInteger.valueOf(3));
	}

	@Test
	public void testExtendedEuclidean() {
		FieldPolynomial1D a = new FieldPolynomial1D(new FieldElement[] {field.element(2), field.element(1), field.element(1)});
		FieldPolynomial1D b = new FieldPolynomial1D(new FieldElement[] {field.element(1), field.element(1)});

		System.out.println("a: " + a);
		System.out.println("b: " + b);

		ExtendedEuclidean euc = new ExtendedEuclidean(a, b);
		
		System.out.println("d: " + euc.d());
		System.out.println("s: " + euc.s());
		System.out.println("t: " + euc.t());


		FieldPolynomial1D as = a.multiply(euc.s());
		FieldPolynomial1D bt = b.multiply(euc.t());

		System.out.println("as: " + as);
		System.out.println("bt: " + bt);
		System.out.println("as+bt: " + as.add(bt));

		assertEquals(euc.d(), as.add(bt));
		
		FieldPolynomial1D expB = new FieldPolynomial1D(field.element(1), 0);
		FieldPolynomial1D newB = bt.quotientAndRemainder(a)[1];
		System.out.println(expB);
		System.out.println(newB);
		assertEquals(expB, newB);
	}

}
