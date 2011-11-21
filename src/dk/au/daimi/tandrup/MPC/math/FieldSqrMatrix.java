package dk.au.daimi.tandrup.MPC.math;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class FieldSqrMatrix {
	
	/** 
	 * the dimension of the matrix
	 */
	private int n;
	
	/** 
	 * the matrix's elements
	 */
	private FieldElement[][] values;

	/** 
	 * the field of the matrix's elements
	 */
	private Field elemField;

	/**
	 * Constuctor, creates the matrix instance from a FieldElement 2D - array
	 * All calculations will be performed in the Field of 
	 * @param mat a 2D array containing the values of the matrix
	 * @throws MatrixDimensionsException if mat dimensions are not equal
	 */
	public FieldSqrMatrix(FieldElement[][] mat) {
		// check dimensions
		if (mat.length == 0 || (mat[0].length != mat.length)) {
			throw new MatrixDimensionsException("Matrix is not square");
		}
	
		// init the values of the matrix
		values = new FieldElement[mat.length][mat.length];	
		copyArray(values,mat,mat.length,mat.length,0,0);
		n = mat.length;
		elemField = mat[0][0].field();
	}
	
	/**
	 * Constructor for creating a new empty matrix.
	 * @param dim the dimensions of the matrix
	 * @param elemField the field used for calculations
	 */
	public FieldSqrMatrix(int dim, Field elemField) {
		values = new FieldElement[dim][dim];
		this.elemField = elemField;
		n = dim;
	}

	/**
	 * copy constructor 
	 * @param from the source of the copy
	 */	
	public FieldSqrMatrix(FieldSqrMatrix from) {
		values = new FieldElement[from.n][from.n];
		this.n = from.n;
		copyArray(values,from.values,n,n,0,0);	
		this.elemField = from.elemField;
	}
	
	@Override
	public String toString() {
		String res=new String();

		for (int i=0;i<n;i++)
		{
			for (int j=0;j<n;j++)
			{
				res=res.concat(values[i][j]+" ");
			}
			res=res.concat("\n");
		}
		
		return "The Matrix is:\n"+res+"\n";
	}

	/**
	 * helper function for copying large areas of 2D FieldElement arrays 
	 * @param target the target of the copy
	 * @param source the source of the copy
	 * @param sizeY the Y dimensions of the area to copy 
	 * @param sizeX the X dimensions of the area to copy 
	 * @param y y dimesion skew between source and destination
	 * @param x x dimesion skew between source and destination
	 */
	private static void copyArray(FieldElement[][] target, FieldElement[][] source, int sizeY, int sizeX, int y, int x) {
		for (int i = y; i<sizeY; i++)	{
			for (int j = x; j<sizeX; j++) {
				target[i-y][j-x] = source[i][j];
			}
		}
	}
	
	/**
	 * inverts the matrix using gaussian reduction method
	 * @return the inversion matrix B
	 * @throws MatrixNotInvertiableException if the matrix is not invertiable
	 */
	public FieldSqrMatrix invert() {
		FieldSqrMatrix res = new FieldSqrMatrix(this);
		FieldElement[][] tempArray = new FieldElement[n][2*n];
	
		copyArray(tempArray,this.values,n,n,0,0);
		tempArray = invertArray(tempArray, n, elemField);
		copyArray(res.values,tempArray,n,2*n,0,n);
		return res;
	}
	
	/**
	 * turn the matrix into a upper diagonal matrix using linear actions
	 * on the row vectors
	 * @return 
	 * the upper diagonal matrix created from this
	 */
	public FieldSqrMatrix diag() {
		FieldSqrMatrix res = new FieldSqrMatrix(this);
		diagonalArray(res.values, n, elemField);
		return res;
	}
	
	/**
	 * helper function for inverting a FieldElement 2D matrix.
	 * @return the inverted matrix in the locations [0..n]X[n..2n-1]
	 * @throws MatrixNotInvertiableException if the matrix is not invertiable
	 * @param D the array to be inverted
	 * @param n the diemnsion to be inverted (assuming D is of dimensions nX2n)
	 * @param elemField the field in which the calculation are performed
	 */
	private static FieldElement[][] invertArray(FieldElement[][] D, int n, Field elemField) {
		FieldElement alpha;
		FieldElement beta;
		int i;
		int j;
		int k;

		int n2 = 2 * n;

		// init the reduction matrix  
		for (i = 0; i < n; i++) {
			for (j = 0; j < n; j++) {
				D[i][j + n] = elemField.zero();
			}
			D[i][i + n] = elemField.one();
		}

		// perform the reductions  
		for (i = 0; i < n; i++) {
			alpha = D[i][i];
				
			if (alpha.equals(elemField.zero())) /* error - singular matrix */ {
				throw new MatrixNotInvertiableException("Singular matrix, cannot invert");
			} else {
				// normalize the vector
				for (j = 0; j < n2; j++) {
					D[i][j] = D[i][j].multiply(alpha.inverse());
				}
				// subtract the vector from all other vectors to zero the 
				// relevant matrix elements in the current column 
				for (k = 0; k < n; k++) {
					if ((k - i) != 0) {
						beta = D[k][i];
						for (j = 0; j < n2; j++) {
							D[k][j] = D[k][j].subtract(beta.multiply(D[i][j]));
						}
					}
				}
			}
		}
		return D;
	}

	/**
	 * helper function for turning a 2D array of FieldElements to an upper 
	 * diagonal matrix using linear actions. Assuming array dimensions are 
	 * nXn.
	 * @return FieldElement 2D array containing the upper diagonal matrix
	 * @param 
	 * @param D 2D array to be turned into diagonal form
	 * @param n the dimension of the array
	 * @param elemField the field in which the calculation are performed
	 */
	private static FieldElement[][] diagonalArray(FieldElement[][] D, int n, Field elemField) {
		FieldElement alpha;
		FieldElement beta;
		int i;
		int j;
		int k;
		
		// perform the reduction
		for (i = 0; i < n; i++) {
			alpha = D[i][i];
			
			if (alpha.equals(elemField.zero())) {
				// search for a vector which doesnt have a zero value in the 
				// same position
				for (j=i; j<n; j++) {
					if (!D[j][i].equals(elemField.zero())) {
						break;
					}
				}
				
				// found a vector swap them
				if (j != n) {
					for (int t=i; t<n; t++) {
						FieldElement temp = D[j][t];
						D[j][t] = D[i][t];
						D[i][t]	= temp;
					}	
					alpha = D[i][i];
				} else {
					// didnt find a vector - continue
					continue;	
				}
			}
			
			// normalize the current vector							
			for (j = 0; j < n; j++) {
				D[i][j] = D[i][j].multiply(alpha.inverse());
			}
			
			// reduce the vector from the next vectors thus causing the 
			// current column to be zero
			for (k = 0; k < n; k++) {
				if ((k - i) != 0) {
					beta = D[k][i];
					for (j = 0; j < n; j++) {
						D[k][j] = D[k][j].subtract(beta.multiply(D[i][j]));
					}
				}
			}
		}
		return D;
	}
	
	/**
	 * multiply two matices and return the result.
	 * @param mat the matrix to be multiplied with this
	 * @return the result of the multiplation
	 */
	public FieldSqrMatrix multiply(FieldSqrMatrix mat) {
		FieldSqrMatrix res = new FieldSqrMatrix(this.n,this.elemField);
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				res.values[i][j] = elemField.zero();
				for (int k = 0; k < n; k++)
					res.values[i][j] = res.values[i][j].add(this.values[i][k].multiply(mat.values[k][j]));
			}
		}
		return res;
	}
	
	/**
	 * multiply the matrix with a vector
	 * @param vector the vector to be multiplied
	 * @return the vector which is the result of the multiplation
	 */
	public FieldElement[] multiply(FieldElement[] vector) {
		if (vector.length != this.n) 
			throw new RuntimeException("illegal dimensions for multiply");
			
		FieldElement[] res = new FieldElement[n];
		
		for (int i = 0; i < n; i++) {
			res[i] = elemField.zero();
			for (int j = 0; j < n; j++) {
				res[i] = res[i].add(this.values[i][j].multiply(vector[j]));
			}
		}
		return res;
	}
	
	/**
	 * solve the linear equation system which is represented by this matrix
	 * and returns a private solution.
	 * @return a private solution to the equation system
	 */
	public FieldElement[] solveHLinearEq() {
		FieldSqrMatrix tempMat = this.diag();
		FieldElement[] privateSolution = new FieldElement[n];

		// init the private solution vector and rest of the elements
		// of the matrix which are dependent on others
		for (int i = 0; i < n; i++) {
			if (tempMat.values[i][i].equals(elemField.zero())) {
				tempMat.values[i][i] = elemField.one();
				privateSolution[i] = elemField.one();
			} else {
				privateSolution[i] = elemField.zero();
			}
		}
		
		// invert the equations system and multiply with the private 
		// solution
		tempMat = tempMat.invert();
		privateSolution = tempMat.multiply(privateSolution);
		
		return privateSolution;
	}


}

/**
 * MatrixDimensionsException class used for signaling an error in matrix
 * dimensions
 */
class MatrixDimensionsException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public MatrixDimensionsException(String msg) {
		super(msg);	
	}
}

/**
 * MatrixNotInvertiableException class used for signaling that the 
 * matrix is not invertiable
 */
class MatrixNotInvertiableException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public MatrixNotInvertiableException(String msg) {
		super(msg);	
	}
}

