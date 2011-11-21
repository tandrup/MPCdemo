package dk.au.daimi.tandrup.MPC.math;

import java.math.BigInteger;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.BigIntegerFieldElement;

public class BerlekampMassey {
	public static FieldPolynomial1D interpolate(FieldElement[] s, FieldElement[] si, int k_, int l) {
		int k = s.length;
		if (k < 2*l + k_)
			throw new IllegalArgumentException("Missing elements");
		
		//FieldPolynomial1D y = Lagrange.interpolate(s, si);
		
		
		
		throw new IllegalStateException();
	}
	
	public static FieldPolynomial1D interpolateOLD(FieldElement[] s) {
		Field field = s[0].field();
		
		int N = s.length / 2;
		
		FieldPolynomial1D B = new FieldPolynomial1D(field.one(), 0);
		FieldPolynomial1D C = new FieldPolynomial1D(field.one(), 0);
		int L = 0;
		int k = 1;
		FieldElement b = field.one();
		
		for (int n = 0; n < 2*N-1; n++) {
			FieldElement d = s[n+1];
			for (int i = 1; i<L; i++) {
				d = d.add(C.coefficient(i).multiply(s[n-i+1]));
			}
			if (d.equals(d.field().zero())) k = k + 1;
			if (!d.equals(d.field().zero()) && 2*L > n) {
				FieldPolynomial1D dxkBb = new FieldPolynomial1D(d, k).multiply(B.quotient(b));
				C = C.substract(dxkBb);
				k = k + 1;
			}
			if (!d.equals(d.field().zero()) && 2*L <= n) {
				FieldPolynomial1D T = C;
				FieldPolynomial1D dxkBb = new FieldPolynomial1D(d, k).multiply(B.quotient(b));
				C = C.substract(dxkBb);
				B = T;
				L = n+1-L;
				k = 1;
				b = d;
			}
		}
		
		System.out.println(B);
		System.out.println(C);
		
		return C;
	}
	
	public static void main(String[] args) {
		BigInteger mod = BigInteger.valueOf(101);		

		BigIntegerFieldElement[] coeffs = new BigIntegerFieldElement[3];
		coeffs[0] = new BigIntegerFieldElement(BigInteger.valueOf(65), mod);
		coeffs[1] = new BigIntegerFieldElement(BigInteger.valueOf(32), mod);
		coeffs[2] = new BigIntegerFieldElement(BigInteger.valueOf(12), mod);

		FieldPolynomial1D poly = new FieldPolynomial1D(coeffs);
		
		FieldElement[] s = new FieldElement[8];
		
		for (int i = 0; i < s.length; i++) {
			s[i] = poly.eval(new BigIntegerFieldElement(i, mod));
		}
		
		FieldPolynomial1D C = interpolateOLD(s);
		
		System.out.println(C);

		for (int i = 0; i < s.length; i++) {
			System.out.println(C.eval(new BigIntegerFieldElement(i, mod)));
		}
	}
}
