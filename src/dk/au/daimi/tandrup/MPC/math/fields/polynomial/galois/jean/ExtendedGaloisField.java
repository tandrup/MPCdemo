package dk.au.daimi.tandrup.MPC.math.fields.polynomial.galois.jean;

/*
GALOISFIELD

This Package is written by Jan Struyf
URL: http://ace.ulyssis.student.kuleuven.ac.be/~jeans
EMail: jan.struyf@student.kuleuven.ac.be
Post:	Jan Struyf, Hoogstraat 47, 3360 Bierbeek, BELGIUM

You can do anything with it, if you leave this comment field unmodified.
 */

import java.util.Arrays;
import java.util.Vector;

public class ExtendedGaloisField extends GaloisField {
	GaloisPolynomial[] alfaPowers;
	GaloisPolynomial moduloPoly;
	char alfa;
	GaloisField base;
	int pwr;

	private static final int[] ALFACOEFS = {0, 1};

	public ExtendedGaloisField(GaloisField newbase, char newalfa, int newpwr) throws GaloisException {
		super(GaloisField.power(newbase.getCardinality(),newpwr),0);
		alfa = newalfa;
		base = newbase;
		pwr = newpwr;
		boolean alfafound = false;
		int ctr;
		GaloisPolynomial myalfa = new GaloisPolynomial(ALFACOEFS,alfa,base);
		Vector irrpolys = GaloisPolynomial.irrMonomials(pwr,alfa,base);
		ExtendedFieldHelper help = new ExtendedFieldHelper(pwr,alfa,base);
		for (ctr = 0; ctr < irrpolys.size() && alfafound == false; ctr++) {
			GaloisPolynomial irrpoly = (GaloisPolynomial)irrpolys.elementAt(ctr);
			help.setModuloPoly(irrpoly);
			if (help.isAlfa(myalfa)) {		
				alfafound = true;
				moduloPoly = irrpoly; 
			}
		}
		for (ctr = 0; ctr < irrpolys.size() && alfafound == false; ctr++) {
			GaloisPolynomial irrpoly = (GaloisPolynomial)irrpolys.elementAt(ctr);
			help.setModuloPoly(irrpoly);
			try {
				help.setModuloPoly(irrpoly);
				myalfa = help.findAlfa();
				alfafound = true;
				moduloPoly = irrpoly; 
			} catch (GaloisException e) {}
		}
		if (alfafound) alfaPowers = help.createAlfaPowers(myalfa);
		else throw new GaloisException("Can't find primitive polynomial",GaloisException.PRIMITIVE);		
	}

	public ExtendedGaloisField(GaloisPolynomial irrpoly) throws GaloisException  {
		super(GaloisField.power(irrpoly.getField().getCardinality(),irrpoly.degree()),0);
		alfa = irrpoly.getAlfa();
		base = irrpoly.getField();
		pwr = irrpoly.degree();
		moduloPoly = irrpoly; 
		ExtendedFieldHelper help = new ExtendedFieldHelper(pwr,alfa,base);
		help.setModuloPoly(irrpoly);
		GaloisPolynomial myalfa = help.findAlfa();
		alfaPowers = help.createAlfaPowers(myalfa);
	}

	public boolean isBase() {
		return false;
	}

	public GaloisField getBase() {
		return base;
	}

	public int plusmin(int a, int b, boolean add) {
		int c, d, mycard = getCardinality();
		GaloisPolynomial ap, bp, res;
		c = normalise(a);
		d = normalise(b);
		if (d == GaloisField.ZERO) return c;
		if (c == GaloisField.ZERO) {
			if (add) return d;		
			else ap = GaloisPolynomial.createConstant(GaloisField.ZERO,
					base.getAlfa(),base);
		} else ap = alfaPowers[c-1];
		bp = alfaPowers[d-1];
		if (add) res = ap.sum(bp);
		else res = ap.minus(bp);
		for (int ctr = 0; ctr < mycard; ctr++) {
			if (res.equals(alfaPowers[ctr])) return ctr+1;
		}
		return GaloisField.ZERO;
	}

	public int sum(int a, int b) {
		int c = plusmin(a,b,true);
		return c;
	}

	public int minus(int a, int b) {
		return plusmin(a,b,false);
	}

	public int product(int a, int b) {
		int c, d, e;
		c = normalise(a);
		d = normalise(b);
		if (c == GaloisField.ZERO || d == GaloisField.ZERO) return GaloisField.ZERO;		
		e = (c+d-2) % (getCardinality()-1);
		return e+1;
	}


	public int divide(int a, int b) 
	throws GaloisException {
		int c, d, e;
		c = normalise(a);
		d = normalise(b);
		if (c == GaloisField.ZERO) return GaloisField.ZERO;		
		if (d == GaloisField.ZERO) 
			throw new GaloisException("Divide by zero",GaloisException.DIVIDE);
		e = (c-d)%(card-1);
		if (e >= 0) return e+1;
		else return e+card;
	}

	public String toString() {
		return "Extended Galois Field GF("+getCardinality()+").";
	}

	public int toBase(int element) throws GaloisException {
		int c = normalise(element);
		if (c == GaloisField.ZERO || c == GaloisField.ONE) return c;
		GaloisPolynomial myelem = alfaPowers[c-1];
		if (myelem.degree() > 0)
			throw new GaloisException("toBase: Element not in base field",GaloisException.FIELDCONV);
		return myelem.getCoefficient(0);
	}

	public int toSuper(int a) throws GaloisException {
		if (a == GaloisField.ZERO || a == GaloisField.ONE) return a;
		int res = -1;
		int mycard = getCardinality();
		GaloisPolynomial myelem = GaloisPolynomial.createConstant(a,alfa,base);
		for (int ctr = 0; ctr < mycard && res == -1; ctr++) {
			if (myelem.equals(alfaPowers[ctr])) res = ctr+1;
		}
		if (res == -1) throw new GaloisException("toSuper: Element not in extended field",
				GaloisException.FIELDCONV);
		return res;
	}

	public GaloisPolynomial getModuloPoly() {
		return moduloPoly;
	}

	public String valueString(int a) {
		int vl = normalise(a);
		if (vl == GaloisField.ZERO) {
			if (getDisplayMode() == GaloisField.POLYCOEFS)
				return fillString('0',pwr);
			return "0";
		}
		switch (getDisplayMode()) {
		case GaloisField.POLYNOMIAL:
			return alfaPowers[vl-1].toString();
		case GaloisField.POLYCOEFS:
			return alfaPowers[vl-1].toCoefficientString(pwr);
		default:
			return String.valueOf(alfa) + "^" + (vl-1);
		}
	}

	public int parse(String strg, int onempty) throws GaloisException {
		String mystrg = cleanString(strg);
		if (mystrg.length() == 0) return onempty;
		GaloisPolynomial mypoly = 
			new GaloisPolynomial(mystrg,alfa,base);
		if (mypoly.degree() >= pwr) {
			GaloisPolynomial divrem[] = mypoly.divide(moduloPoly);
			mypoly = divrem[1];
		}
		int mycard = getCardinality();
		for (int ctr = 0; ctr < mycard; ctr++) {
			if (mypoly.equals(alfaPowers[ctr])) return ctr+1;
		}
		return GaloisField.ZERO;
	}

	public char getAlfa() {
		return alfa;
	}

	public int getAlfaPower(int pwr) {
		return pwr+1;
	}
}

class ExtendedFieldHelper {
	int pwr, num;
	char alfa;
	GaloisField base;
	GaloisPolynomial[] elements;
	GaloisPolynomial moduloPoly;

	public ExtendedFieldHelper(int newpwr, char newalfa, GaloisField newbase) 
	throws GaloisException {
		pwr = newpwr;
		alfa = newalfa;
		base = newbase;
		if (pwr < 2) throw new GaloisException("Modulo Polynomial Degree < 2",
				GaloisException.MODULOPOLY);
		num = ExtendedGaloisField.power(base.getCardinality(),pwr);
		elements = GaloisPolynomial.allPolynomials(pwr-1,alfa,base);
		moduloPoly = null;
	}

	public void setModuloPoly(GaloisPolynomial irrPoly) {
		moduloPoly = irrPoly;
	}

	private GaloisPolynomial alfaProduct(GaloisPolynomial a, GaloisPolynomial b)
	throws GaloisException {
		if (moduloPoly == null)
			throw new GaloisException("Modulo Polynomial Null",GaloisException.MODULOPOLY);
		GaloisPolynomial divrem[] = a.product(b).divide(moduloPoly);
		return divrem[1];
	}

	public GaloisPolynomial[] createAlfaPowers(GaloisPolynomial myalfa) 
	throws GaloisException {
		GaloisPolynomial betw = 
			GaloisPolynomial.createConstant(GaloisField.ONE,alfa,base);
		GaloisPolynomial[] res = new GaloisPolynomial[num];
		for (int ctr = 0; ctr < num; ctr++) {
			res[ctr] = betw;
			betw = alfaProduct(betw,myalfa);
		}
		return res;
	}

	public boolean isAlfa(GaloisPolynomial myalfa) throws GaloisException {
		int reachnum, count;
		boolean[] reached = new boolean[num-1];
		GaloisPolynomial betw = myalfa;		
		reachnum = 1; count = 0;
		do {
			for (int ctr = 0; ctr < num-1; ctr++)
				if (elements[ctr+1].equals(betw) && reached[ctr] == false) {
					reachnum++;
					reached[ctr] = true;
				}
			betw = alfaProduct(betw,myalfa);
			count++;
		} while (reachnum < num && betw.equals(myalfa) == false && count < num);
		return reachnum == num;
	}

	public GaloisPolynomial findAlfa() throws GaloisException {
		for (int ctr = 0; ctr < num; ctr++) {
			GaloisPolynomial myalfa = elements[ctr];
			if (isAlfa(myalfa)) return myalfa;
		}
		throw new GaloisException("Can't find primitive element",GaloisException.PRIMITIVE);
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((base == null) ? 0 : base.hashCode());
		result = PRIME * result + Arrays.hashCode(elements);
		result = PRIME * result + ((moduloPoly == null) ? 0 : moduloPoly.hashCode());
		result = PRIME * result + num;
		result = PRIME * result + pwr;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ExtendedFieldHelper other = (ExtendedFieldHelper) obj;
		if (base == null) {
			if (other.base != null)
				return false;
		} else if (!base.equals(other.base))
			return false;
		if (!Arrays.equals(elements, other.elements))
			return false;
		if (moduloPoly == null) {
			if (other.moduloPoly != null)
				return false;
		} else if (!moduloPoly.equals(other.moduloPoly))
			return false;
		if (num != other.num)
			return false;
		if (pwr != other.pwr)
			return false;
		return true;
	}
}

