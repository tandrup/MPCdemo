package dk.au.daimi.tandrup.MPC.math;

import java.io.Serializable;
import java.util.Random;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class FieldPolynomial2D implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private FieldElement[][] coeffs;

	/**
	 * Creates a 2 dimensional polynomial such that f(0,0) = secret
	 * @param secret
	 * @param degree
	 * @param randomGen
	 */
	public FieldPolynomial2D(FieldElement secret, int degree, Random randomGen) {
		coeffs = new FieldElement[degree+1][degree+1];
		
		// Fill coefficients with random data
		for (int i = 0; i <= degree; i++)
			for (int j = 0; j <= degree; j++)
				coeffs[i][j] = secret.field().element(randomGen);

		// Set secret to a point in the polynomium	
		coeffs[0][0] = secret;
	}

	/**
	 * Creates a 2 dimensional polynomial using the sypplied coefficients.
	 * @param coeffs
	 */
	public FieldPolynomial2D(FieldElement[][] coeffs) {
		super();
		this.coeffs = coeffs;
	}
	
	public FieldElement eval(FieldElement x, FieldElement y) {
		FieldElement result = x.field().zero();

		for (int i = 0; i <= degree(); i++) {
			for (int j = 0; j <= degree(); j++) {
				FieldElement xy = x.pow(i).multiply(y.pow(j));
				FieldElement led = coeffs[i][j].multiply(xy);
				result = result.add(led);
			}
		}
		
		return result;
	}
	
	public FieldPolynomial1D evalPartial1(FieldElement x) {
		FieldElement[] coeffs1D = new FieldElement[degree()+1];
		
		for (int j = 0; j <= degree(); j++) {
			coeffs1D[j] = x.field().zero();
			for (int i = 0; i <= degree(); i++) {
				coeffs1D[j] = coeffs1D[j].add(coeffs[i][j].multiply(x.pow(i)));
			}
		}
		
		return new FieldPolynomial1D(coeffs1D);
	}
	
	public FieldPolynomial1D evalPartial2(FieldElement y) {
		FieldElement[] coeffs1D = new FieldElement[degree()+1];
		
		for (int i = 0; i <= degree(); i++) {
			coeffs1D[i] = y.field().zero();
			for (int j = 0; j <= degree(); j++) {
				coeffs1D[i] = coeffs1D[i].add(coeffs[i][j].multiply(y.pow(j)));
			}
		}
		
		return new FieldPolynomial1D(coeffs1D);
	}
	
	public int degree() {
		return coeffs.length - 1;
	}
	
	@Override
	public String toString() {
		String retVal = "f(x) = ";
		
		for (int i = 0; i <= degree(); i++) {
			for (int j = 0; j <= degree(); j++) {
				retVal += "(" + coeffs[i][j] + ")*x^" + i + "*y^" + j + " + ";
			}
		}
		
		return retVal.substring(0, retVal.length() - 3).trim();
	}
}
