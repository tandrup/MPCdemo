package dk.au.daimi.tandrup.MPC.math;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class FieldPolynomial1D implements Serializable {
	private static final long serialVersionUID = 1L;

	private FieldElement[] coeffs;

	/**
	 * Creates a random 1 dimensional polynomial such that f(0) = secret
	 * @param secret
	 * @param degree
	 * @param randomGen
	 */
	public FieldPolynomial1D(FieldElement secret, int degree, Random randomGen) {
		coeffs = new FieldElement[degree+1];

		// Set secret to a point in the polynomium	
		coeffs[0] = secret;

		// Fill rest with random data
		for (int i = 1; i < degree+1; i++)
			coeffs[i] = secret.field().element(randomGen);
	}

	/**
	 * Creates a random 1 dimensional polynomial
	 * @param degree
	 * @param randomGen
	 */
	public FieldPolynomial1D(Field field, int degree, Random randomGen) {
		coeffs = new FieldElement[degree+1];

		// Fill coefficients with random data
		for (int i = 0; i < degree+1; i++)
			coeffs[i] = field.element(randomGen);
	}

	/**
	 * Creates the polynomialal f(x) = coeff * x ^ index
	 * @param coeff
	 * @param index
	 */
	public FieldPolynomial1D(FieldElement coeff, int index) {
		coeffs = new FieldElement[index+1];

		// Fill rest with random data
		for (int i = 0; i < coeffs.length; i++)
			coeffs[i] = coeff.field().zero();

		// Set secret to a point in the polynomium	
		coeffs[index] = coeff;
	}

	/**
	 * Creates a 1 dimensional polynomial using the sypplied coefficients.
	 * @param coeffs
	 */
	public FieldPolynomial1D(FieldElement[] coeffs) {
		super();
		if (coeffs.length == 0)
			throw new IllegalArgumentException("Empty coefficient array");

		int deg = degree(coeffs);
		if (deg + 1 < coeffs.length) {
			this.coeffs = new FieldElement[deg+1];
			for (int i = 0; i <= deg; i++)
				this.coeffs[i] = coeffs[i];
		} else {
			this.coeffs = coeffs;
		}
	}

	/**
	 * Evaluate the polynomial using the value x.
	 * @param x
	 * @return
	 */
	public FieldElement eval(FieldElement x) {
		// Evaluate shares (Horner's method)
		FieldElement result = x.field().zero();
		for (int j = degree(); j >= 0; j--) 
			result = coeffs[j].add(x.multiply(result));
		return result;
	}

	/**
	 * Evaluate the polynomial using the value x in the specified field.
	 * @param x
	 * @param field
	 * @return
	 */
	public FieldElement eval(FieldElement x, Field field) {
		// Evaluate shares (Horner's method)
		FieldElement result = field.zero();
		for (int j = degree(); j >= 0; j--) 
			result = field.add(coeffs[j], x.multiply(result));
		return result;
	}

	/**
	 * Evaluate the polynomial from index 1 up to and including index
	 * @param index
	 * @return
	 */
	public FieldElement[] evalUpTo(int index) {
		FieldElement[] shares = new FieldElement[index];

		// Evaluate shares (Horner's method)
		for (int i = 0; i < index; i++) {
			FieldElement xVal = elemField().element(i+1);
			shares[i] = elemField().zero();
			for (int j = degree(); j >= 0; j--) 
				shares[i] = coeffs[j].add(xVal.multiply(shares[i]));
		}

		return shares;
	}

	public Field elemField() {
		return coeffs[0].field();
	}

	public boolean isZero() {
		if (degree() == 0)
			return coeffs[0].equals(coeffs[0].field().zero());
		else
			return false;
	}

	/**
	 * Return the degree of the polynomial
	 * @return the degree of the polynomial
	 */
	public int degree() {
		return degree(coeffs);
	}

	private static int degree(FieldElement[] coeffs) {
		for (int i = coeffs.length - 1; i >= 0; i--) {
			FieldElement k = coeffs[i];
			if (!k.equals(k.field().zero()))
				return i;
		}
		return 0;
	}

	@Override
	public String toString() {
		String retVal = "f(x) = ";

		for (int i = 0; i <= degree(); i++) {
			if (!coeffs[i].equals(elemField().zero())) {
				if (i == 0) {
					retVal += coeffs[i].getElementString() + " + ";
				} else {
					String elemString = "";
					if (!coeffs[i].equals(elemField().one()))
						elemString = coeffs[i].getElementString() + "*";

					if (i == 1)
						retVal += elemString + "x + ";
					else
						retVal += elemString + "x^" + i + " + ";
				}
			}
		}

		return retVal.substring(0, retVal.length() - 3).trim() + " " + coeffs[0].getFieldString();
	}

	/**
	 * Get the coefficient from the i'th term in the polynomial
	 * @param i
	 * @return
	 */
	public FieldElement coefficient(int i) {
		if (i < coeffs.length)
			return coeffs[i];

		return elemField().zero();
	}

	/**
	 * Get all the coefficients in the polynomial
	 * @return
	 */
	public FieldElement[] coefficients() {
		return coeffs;
	}

	/**
	 * Add this polynomial to another polynomial
	 * @param other
	 * @return this + other
	 */
	public FieldPolynomial1D add(FieldPolynomial1D other) {
		int coeffsLength = this.coeffs.length;
		if (other.coeffs.length > coeffsLength)
			coeffsLength = other.coeffs.length;

		FieldElement[] newCoeffs = new FieldElement[coeffsLength];

		for (int i = 0; i < newCoeffs.length; i++) {
			if (i < this.coeffs.length && i < other.coeffs.length) {
				newCoeffs[i] = this.coeffs[i].add(other.coeffs[i]);
			} else if (i < this.coeffs.length) {
				newCoeffs[i] = this.coeffs[i];
			} else if (i < other.coeffs.length) {
				newCoeffs[i] = other.coeffs[i];
			} else {
				throw new IllegalStateException("Programming error");
			}
		}

		return new FieldPolynomial1D(newCoeffs);
	}

	/**
	 * Subtract another polynomial from this polynomial
	 * @param other
	 * @return this - other
	 */
	public FieldPolynomial1D substract(FieldPolynomial1D other) {
		int coeffsLength = this.coeffs.length;
		if (other.coeffs.length > coeffsLength)
			coeffsLength = other.coeffs.length;

		FieldElement[] newCoeffs = new FieldElement[coeffsLength];

		for (int i = 0; i < newCoeffs.length; i++) {
			if (i < this.coeffs.length && i < other.coeffs.length) {
				newCoeffs[i] = this.coeffs[i].subtract(other.coeffs[i]);
			} else if (i < this.coeffs.length) {
				newCoeffs[i] = this.coeffs[i];
			} else if (i < other.coeffs.length) {
				newCoeffs[i] = other.coeffs[i].negative();
			} else {
				throw new IllegalStateException("Programming error");
			}
		}

		return new FieldPolynomial1D(newCoeffs);
	}

	public FieldPolynomial1D negative() {
		FieldElement[] newCoeffs = new FieldElement[coeffs.length];

		for (int i = 0; i < newCoeffs.length; i++) {
			newCoeffs[i] = this.coeffs[i].negative();
		}

		return new FieldPolynomial1D(newCoeffs);
	}

	/**
	 * Multiply this polynomial with the value x
	 * @param x
	 * @return 
	 */
	public FieldPolynomial1D multiply(FieldElement x) {
		FieldElement[] newCoeffs = new FieldElement[coeffs.length];

		for (int i = 0; i < newCoeffs.length; i++) {
			newCoeffs[i] = this.coeffs[i].multiply(x);
		}

		return new FieldPolynomial1D(newCoeffs);
	}

	/**
	 * Multiply this polynomial with another polynomial
	 * @param other
	 * @return
	 */
	public FieldPolynomial1D multiply(FieldPolynomial1D other) {
		Field field = this.coeffs[0].field();

		FieldElement[] newCoeffs = new FieldElement[this.coeffs.length + other.coeffs.length];

		for (int i = 0; i < newCoeffs.length; i++) {
			newCoeffs[i] = field.zero();
		}

		for (int i = 0; i < this.coeffs.length; i++) {
			for (int j = 0; j < other.coeffs.length; j++) {
				FieldElement prod = this.coeffs[i].multiply(other.coeffs[j]);
				newCoeffs[i+j] = newCoeffs[i+j].add(prod);
			}
		}

		return new FieldPolynomial1D(newCoeffs);
	}

	/**
	 * Divide this polynomial be the value x
	 * @param x
	 * @return this / x
	 */
	public FieldPolynomial1D quotient(FieldElement x) {
		FieldElement[] newCoeffs = new FieldElement[coeffs.length];

		for (int i = 0; i < newCoeffs.length; i++) {
			newCoeffs[i] = this.coeffs[i].divide(x);
		}

		return new FieldPolynomial1D(newCoeffs);
	}

	/**
	 * Return the quotient of this polynomial f divided by d.
	 * @param d
	 * @return q such that f = qd + r
	 */
	public FieldPolynomial1D quotient(FieldPolynomial1D d) {
		return quotientAndRemainder(d)[0];
	}

	/**
	 * Return the quotient and the remainder of this polynomial f divided by d.
	 * @param d
	 * @return q, r such that f = qd + r
	 * @throws ArithmeticException if the divisor is the zero poly
	 */
	public FieldPolynomial1D[] quotientAndRemainder(FieldPolynomial1D d) {
		if (d.isZero())
			throw new ArithmeticException("Division by zero");

		Field field = coeffs[0].field();

		FieldPolynomial1D q = new FieldPolynomial1D(field.zero(), 0);
		FieldPolynomial1D r = new FieldPolynomial1D(field.zero(), 0);
		FieldPolynomial1D s = this;

		while (!s.isZero()) {
			int m = d.degree();
			int n = s.degree();

			FieldElement a = d.coefficient(m);
			FieldElement b = s.coefficient(n);

			/*System.out.println("----------------------");
			System.out.println("q: " + q);
			System.out.println("r: " + r);
			System.out.println("s: " + s);
			System.out.println("m: " + m);
			System.out.println("n: " + n);
			System.out.println("a: " + a);
			System.out.println("b: " + b);*/

			if (n >= m) {
				FieldElement c = b.multiply(a.inverse());
				FieldPolynomial1D cxnm = new FieldPolynomial1D(c, n-m);
				q = q.add(cxnm);
				s = s.substract(d.multiply(cxnm));
			} else {
				FieldPolynomial1D bxn = new FieldPolynomial1D(b, n);
				r = r.add(bxn);
				s = s.substract(bxn);
			}

		}

		/*System.out.println("----------------------");
		System.out.println("q: " + q);
		System.out.println("r: " + r);
		System.out.println("s: " + s);
		System.out.println("----------------------");*/

		return new FieldPolynomial1D[] {q, r};
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + Arrays.hashCode(coeffs);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;

		if (!(obj instanceof FieldPolynomial1D))
			return false;

		FieldPolynomial1D other = (FieldPolynomial1D)obj;
		if (this.degree() != other.degree())
			return false;

		for (int i = 0; i < this.coeffs.length && i < other.coeffs.length; i++) {
			if (!this.coeffs[i].equals(other.coeffs[i]))
				return false;
		}

		return true;
	}
}
