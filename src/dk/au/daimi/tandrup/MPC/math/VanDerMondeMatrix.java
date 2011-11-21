package dk.au.daimi.tandrup.MPC.math;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class VanDerMondeMatrix {
	private FieldElement[] alphas;
	private Field field;
	private int columns;
	private boolean transposed;
	
	public VanDerMondeMatrix(Field field, int rows, int columns) {
		this.field = field;
		this.columns = columns;
		this.transposed = false;

		this.alphas = new FieldElement[rows];
		for (int r = 0; r < rows; r++) {
			this.alphas[r] = field.element(r+1);
		}
	}
	
	public VanDerMondeMatrix(FieldElement[] alphas, int columns) {
		this.alphas = alphas;
		this.field = alphas[0].field();
		this.columns = columns;
		this.transposed = false;
	}
	
	private VanDerMondeMatrix(FieldElement[] alphas, int columns, boolean transposed) {
		this.alphas = alphas;
		this.field = alphas[0].field();
		this.columns = columns;
		this.transposed = transposed;
	}

	public VanDerMondeMatrix transpose() {
		return new VanDerMondeMatrix(alphas, columns, !transposed);
	}
	
	public FieldElement[] multiply(FieldElement[] xs) {
		if (transposed) {
			
			if (xs.length != alphas.length)
				throw new IllegalArgumentException("x vector has wrong size");
			
			FieldElement[] ys = new FieldElement[columns];
			
			for (int c = 0; c < columns; c++) {
				ys[c] = field.zero();
				for (int r = 0; r < alphas.length; r++) {
					ys[c] = ys[c].add(alphas[r].pow(c).multiply(xs[r]));
				}
			}		

			return ys;

		} else {
		
			if (xs.length != columns)
				throw new IllegalArgumentException("x vector has wrong size");
			FieldElement[] ys = new FieldElement[alphas.length];

			for (int r = 0; r < alphas.length; r++) {
				FieldElement alpha_j = field.one();

				ys[r] = field.zero();
				for (int c = 0; c < columns; c++) {
					ys[r] = ys[r].add(alpha_j.multiply(xs[c]));
					alpha_j = alpha_j.multiply(alphas[r]);
				}
			}		

			return ys;
		}
	}

	@Override
	public String toString() {
		String retVal = "Van = ";
		
		if (transposed) {
			for (int c = 0; c < columns; c++) {
				retVal += "[";
				for (int r = 0; r < alphas.length; r++) {
					if (r > 0)
						retVal += ", ";
					retVal += alphas[r].pow(c);
				}
				retVal += "]\n      ";
			}

		} else {
			for (int r = 0; r < alphas.length; r++) {
				retVal += "[";
				FieldElement alpha_j = field.one();

				for (int c = 0; c < columns; c++) {
					if (c > 0)
						retVal += ", ";
					retVal += alpha_j;
					alpha_j = alpha_j.multiply(alphas[r]);
				}
				retVal += "]\n      ";
			}
		}
		
		return retVal.trim();
	}
}
