package dk.au.daimi.tandrup.MPC.math;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class ExtendedEuclidean {
	private FieldPolynomial1D d, s, t;
	
	public ExtendedEuclidean(FieldPolynomial1D a, FieldPolynomial1D b) {
		if (!(a.degree() >= b.degree()))
			throw new IllegalArgumentException("Error: deg(a) < deg(b)");
		
		Field field = a.coefficient(0).field();
		FieldPolynomial1D r, r_, s_, t_;
		
		r = a;
		r_ = b;
		s = new FieldPolynomial1D(field.one(), 0);
		s_ = new FieldPolynomial1D(field.zero(), 0);
		t = new FieldPolynomial1D(field.zero(), 0);
		t_ = new FieldPolynomial1D(field.one(), 0);
		
		while (!r_.isZero()) {
			FieldPolynomial1D[] qr__ = r.quotientAndRemainder(r_);
			FieldPolynomial1D q = qr__[0];
			FieldPolynomial1D r__ = qr__[1];
			
			FieldPolynomial1D tmp;

			r = r_;
			r_ = r__;
			
			tmp = s;
			s = s_;
			s_ = tmp.substract(s_);
			
			tmp = t;
			t = t_;
			t_ = tmp.substract(t_.multiply(q));
		}
		
		FieldElement c = r.coefficient(r.degree());
		
		d = r.quotient(c);
		s = s.quotient(c);
		t = t.quotient(c);
	}
	
	public FieldPolynomial1D d() {
		return d;
	}

	public FieldPolynomial1D s() {
		return s;
	}

	public FieldPolynomial1D t() {
		return t;
	}
}
