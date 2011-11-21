package dk.au.daimi.tandrup.MPC.math;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class BerlekampWelch {

	public static FieldPolynomial1D interpolate(Map<FieldElement, FieldElement> shares, int degree) {
		return interpolate(shares.entrySet(), degree);
	}	

	public static FieldPolynomial1D interpolate(Set<Entry<FieldElement, FieldElement>> shares, int degree) {
		FieldElement[] arrShares = new FieldElement[shares.size()];
		FieldElement[] arrShareIndexs = new FieldElement[shares.size()];

		int i = 0;
		for (Entry<FieldElement,FieldElement> entry : shares) {
			arrShares[i] = entry.getValue();
			arrShareIndexs[i] = entry.getKey();
			i++;
		}

		return interpolate(arrShareIndexs, arrShares, degree);
	}

	/**
	 * reconstructs the poly from the given parameters
	 * @param ys a 1D array of FieldElements containing the received values
	 * @param degree the degree of the poly we are reconstructing
	 * @throws
	 * 	BerlekampWelchException if the poly cannot be reconstructed
	 */
	public static FieldPolynomial1D interpolate(FieldElement[] ys, int degree) {
		FieldElement[] xs = new FieldElement[ys.length];
		Field field = ys[0].field();

		for (int i = 0; i < xs.length; i++) {
			xs[i] = field.element(i+1);
		}

		return interpolate(xs, ys, degree);
	}

	/**
	 * reconstructs the poly from the given parameters
	 * @param xs a 1D array of FieldElements containing the alpha values
	 * @param ys a 1D array of FieldElements containing the received values
	 * @param degree the degree of the poly we are reconstructing
	 * @throws
	 * 	BerlekampWelchException if the poly cannot be reconstructed
	 */
	public static FieldPolynomial1D interpolate(FieldElement[] xs, FieldElement[] ys, int degree) {
		FieldPolynomial1D N;        // the reconstructed poly
		FieldPolynomial1D E;        // the error detection poly
		FieldPolynomial1D N_div_E;  // the result of the division of N poly with E poly

		Field elemField = ys[0].field();

		// check that the the number of points given is compatiable
		if (xs.length != ys.length) {
			throw new BerlekampWelchException("ys and xs must be of the same dimentions");
		}

		// the number of received values
		int n = xs.length;

		// the maximum number of errors
		// we require the following equation:
		// 2e + k + 1 = n ==> e = (n - k - 1)/ 2 or
		// 2e + k + 2 = n ==> e = (n - k) / 2
		int e = (n - degree - 1) / 2 + (n - degree - 1) % 2;

		int dim = 2*e+degree+2;

		// generate the linear homogenious equations system				
		FieldElement[][] eqSys = new FieldElement[dim][dim];
		for (int i = 0; i < dim; i++) {
			for (int j = 0; j < dim; j++) {
				if (n <= i) {
					// if we need to add an equation for E(e) then add a row of zeros 
					eqSys[i][j] = elemField.zero();
				} else if (j <= e + degree) {
					// (alpha_i)^j
					eqSys[i][j] = xs[i].pow(j);
				} else {
					// -(r_i)*(alpha_i)^j
					eqSys[i][j] = (ys[i].multiply(xs[i].pow(j-e-degree-1))).negative();
				} 
			}
		}

		// generate a matrix from the equation system		
		FieldSqrMatrix mat = new FieldSqrMatrix(eqSys);

		// generate coefficients vectors for the polys N and E
		FieldElement[] solutionNE = mat.solveHLinearEq();
		FieldElement[] arrayE = new FieldElement[e+1];
		FieldElement[] arrayN = new FieldElement[e+degree+1];

		System.arraycopy(solutionNE,0,arrayN,0,degree+e+1);
		System.arraycopy(solutionNE,e+degree+1,arrayE,0,e+1);

		// generate polys from the coefficients
		E = new FieldPolynomial1D(arrayE);
		N = new FieldPolynomial1D(arrayN);

		try {
			N_div_E = N.quotient(E);
			return N_div_E;
		} catch (ArithmeticException aexp) {
			throw new BerlekampWelchException("Cannot reconstruct the polynomial");
		}
	}
}

/**
 * Exception for BerlekampWelch class. this is used for signaling an error
 * which cause the poly to be unreconstructable
 */
class BerlekampWelchException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public BerlekampWelchException(String msg) {
		super(msg);	
	}		
}