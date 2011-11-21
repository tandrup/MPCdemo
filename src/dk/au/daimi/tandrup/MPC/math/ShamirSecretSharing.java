package dk.au.daimi.tandrup.MPC.math;

import java.util.Random;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class ShamirSecretSharing {
	public static FieldElement[] split(FieldElement secret, int threshold, int sharesCount, Random randomGen) {
		FieldElement[] a = new FieldElement[threshold];
		FieldElement[] shares = new FieldElement[sharesCount];

		// Set secret to a point in the polynomium	
		a[0] = secret;

		// Fill rest with random data
		for (int i = 1; i < threshold; i++) 
			a[i] = secret.field().element(randomGen);

		// Evaluate shares (Horner's method)
		for (int i = 0; i < sharesCount; i++) {
			//BigInteger x = BigInteger.ONE;
			FieldElement xVal = secret.field().element(i+1);
			shares[i] = secret.field().zero();
			for (int j = threshold-1; j >= 0; j--) 
				shares[i] = a[j].add(xVal.multiply(shares[i]));
		}

		return shares;
	}

	public static FieldElement join(FieldElement[] shares, FieldElement[] shareIndexs, int threshold) {
		if (shares.length < threshold)
			throw new IllegalArgumentException("Not enough shares");
		Field field = shares[0].field();

		// compute the key
		FieldElement sumTermAcc = field.zero();
		for(int i=0; i < shares.length; i++) {
			FieldElement prodTermAcc = field.one();
			for(int j=0; j < shares.length; j++) {
				if (i == j) 
					continue;

				FieldElement xj = shareIndexs[j];

				// xj - xi
				FieldElement xjSubxi = xj.subtract(shareIndexs[i]);
				// (xj - xi)^-1
				FieldElement xjSubxiInv = xjSubxi.inverse();

				// xj * (xj - xi)^-1
				FieldElement prodTerm = xj.multiply(xjSubxiInv);

				// 
				prodTermAcc = prodTermAcc.multiply(prodTerm);
			}
			FieldElement sumTerm = shares[i].multiply(prodTermAcc);
			sumTermAcc = sumTermAcc.add(sumTerm);
		}

		return sumTermAcc;
	}

	public static FieldElement join(FieldElement[] shares, FieldElement[] shareIndexs, FieldElement x) {
		Field field = shares[0].field();

		// compute the key
		FieldElement sumTermAcc = field.zero();
		for(int j=0; j < shares.length; j++) {
			FieldElement prodTermAcc = field.one();
			for(int i=0; i < shares.length; i++) {
				if (i == j) 
					continue;

				FieldElement xi = shareIndexs[i];
				FieldElement xj = shareIndexs[j];

				// xj - xi
				FieldElement xjSubxi = xj.subtract(xi);
				// (xj - xi)^-1
				FieldElement xjSubxiInv = xjSubxi.inverse();
				
				// x - xi
				FieldElement xSubxi = x.subtract(xi);

				// (x - xi) * (xj - xi)^-1
				FieldElement prodTerm = xSubxi.multiply(xjSubxiInv);

				// 
				prodTermAcc = prodTermAcc.multiply(prodTerm);
			}
			FieldElement yj = shares[j];
			FieldElement sumTerm = yj.multiply(prodTermAcc);
			sumTermAcc = sumTermAcc.add(sumTerm);
		}

		return sumTermAcc;
	}
}
