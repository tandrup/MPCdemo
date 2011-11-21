package dk.au.daimi.tandrup.MPC.math;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class Lagrange {
	private transient static final Logger logger = Logger.getLogger(Lagrange.class.getName());
	
	public static FieldPolynomial1D interpolate(Map<FieldElement, FieldElement> shares) {
		return interpolate(shares.entrySet());
	}	

	public static FieldPolynomial1D interpolate(Set<Entry<FieldElement, FieldElement>> shares) {
		FieldElement[] arrShares = new FieldElement[shares.size()];
		FieldElement[] arrShareIndexs = new FieldElement[shares.size()];
		
		int i = 0;
		for (Entry<FieldElement,FieldElement> entry : shares) {
			arrShares[i] = entry.getValue();
			arrShareIndexs[i] = entry.getKey();
			i++;
		}
		
		return interpolate(arrShareIndexs, arrShares);
	}
	
	private static FieldPolynomial1D computeSumTerm(Field field, FieldElement[] shares, FieldElement[] shareIndexs, int j) {
		FieldPolynomial1D prodTermAcc = new FieldPolynomial1D(field.one(), 0);
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
			FieldPolynomial1D xSubxi = new FieldPolynomial1D(new FieldElement[] {xi.negative(), field.one()});

			// (x - xi) * (xj - xi)^-1
			FieldPolynomial1D prodTerm = xSubxi.multiply(xjSubxiInv);

			// 
			prodTermAcc = prodTermAcc.multiply(prodTerm);
		}
		FieldElement yj = shares[j];
		FieldPolynomial1D sumTerm = prodTermAcc.multiply(yj);
		return sumTerm;
	}
	
	public static FieldPolynomial1D interpolate(FieldElement[] shareIndexs, FieldElement[] shares) {
		try {
			Field field = shares[0].field();

			// compute the key
			FieldPolynomial1D sumTermAcc = new FieldPolynomial1D(field.zero(), 0);
			for(int j=0; j < shares.length; j++) {
				FieldPolynomial1D sumTerm = computeSumTerm(field, shares, shareIndexs, j);
				sumTermAcc = sumTermAcc.add(sumTerm);
			}

			return sumTermAcc;
		} catch (ArithmeticException ex) {
			String strInp = null;
			String strInp2 = null;
			for (int i = 0; i < shareIndexs.length; i++) {
				if (strInp == null)
					strInp = shareIndexs[i].toString();
				else
					strInp += "\t" + shareIndexs[i];
				if (strInp2 == null)
					strInp2 = shares[i].toString();
				else
					strInp2 += "\t" + shares[i];
			}
			logger.throwing(Lagrange.class.getName(), "interpolate", ex);
			throw ex;
		}
	}
}
