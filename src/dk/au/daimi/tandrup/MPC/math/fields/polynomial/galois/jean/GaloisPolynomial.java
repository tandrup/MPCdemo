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

/**
 * A class for working with Polynomials.
 * <BR>The coefficients are elements taken from a galois field
 * @author Jan Struyf
 * @version 1.00
 * @see GaloisField
 * @see ExtendedGaloisField
 */
public class GaloisPolynomial {
	private int[] coefs;
	private char alfa;
	private GaloisField field;

	/**
	 * An array of zero coefficients to construct a zero polynomial.
	 */
	public static final int[] ZEROCOEFS = {0};

	/**
	 * Construct a polynomial.
	 * @param newcoef An array containing the coefficients.
	 * @param newalpha The variable. For ex: 'x'.
	 * @param newfield The coefficient galois field.
	 */
	public GaloisPolynomial(int[] newcoef, char newalfa, GaloisField newfield) {
		alfa = newalfa;
		field = newfield;

		int len = newcoef.length;
		while (len > 0 && field.normalise(newcoef[len-1]) == GaloisField.ZERO) len--;

		if (len == 0) {
			coefs = new int[1];
			coefs[0] = GaloisField.ZERO;
		} else {
			coefs = new int[len];
			for (int ctr = 0; ctr < len; ctr++) 
				coefs[ctr] = field.normalise(newcoef[ctr]);
		}
	}

	/**
	 * Construct a polynomial. Invoke the string parser.
	 * @param poly A string representation for the polynomial.
	 * @param newalpha The variable. For ex: 'x'.
	 * @param newfield The coefficient galois field.
	 */
	public GaloisPolynomial(String poly, char newalfa, GaloisField newfield) 
	throws GaloisException {
		this(parsecoefs(poly, newalfa, newfield),newalfa,newfield);
	}

	/**
	 * Returns a constant 'polynomial'.
	 * @param elem The constants value.
	 * @param newalpha The variable. For ex: 'x'.
	 * @param newfield The coefficient galois field.
	 */
	public static GaloisPolynomial createConstant(int elem, char newalfa, GaloisField newfield) {
		int[] coef = new int[1];
		coef[0] = elem;
		return new GaloisPolynomial(coef,newalfa,newfield);
	}

	/**
	 * Evaluates the polynomial for a given galois element using the Horner scheme.
	 */
	public int calcHorner(int vl) {
		int res = GaloisField.ZERO;
		int deg = degree();
		for (int ctr = 0; ctr <= deg; ctr++) {
			res = field.product(res,vl);
			res = field.sum(res,coefs[deg-ctr]);
		}
		return res;
	}

	/**
	 * Converts the polynomial's coefficients into base-field elements.
	 */
	public GaloisPolynomial toBase() throws GaloisException {
		int len = coefs.length;
		int newcoef[] = new int[len];
		for (int ctr = 0; ctr < len; ctr++) 
			newcoef[ctr] = field.toBase(coefs[ctr]);
		return new GaloisPolynomial(newcoef,alfa,field.getBase());
	}

	/**
	 * Converts the polynomial's coefficients into elements of [fld2].
	 * @exception GaloisException If [fld2] is no superfield of getField().
	 */
	public GaloisPolynomial toExtended(GaloisField fld2) throws GaloisException {
		int len = coefs.length;
		int newcoef[] = new int[len];
		for (int ctr = 0; ctr < len; ctr++) 
			newcoef[ctr] = field.toExtended(coefs[ctr],fld2);
		return new GaloisPolynomial(newcoef,alfa,fld2);
	}

	/**
	 * Returns a string representation for this polynomial.
	 * <BR>For ex: 1 + 3x + 4x^2.
	 */
	public String toString() {
		String res = "";
		boolean add = false;
		int len = coefs.length;
		for (int ctr = 0; ctr < len; ctr++) {
			if (coefs[ctr] != GaloisField.ZERO || len == 1) {
				if (add) res += " + ";
				if (coefs[ctr] != GaloisField.ONE || ctr == 0 || 
						field.getDisplayMode() == GaloisField.POLYCOEFS) {
					String coefStr = field.valueString(coefs[ctr]);
					if (coefStr.indexOf(' ') == -1 && coefStr.indexOf('+') == -1)
						res += coefStr;
					else 
						res += "(" + coefStr + ")";
				}
				if (ctr == 1) res += alfa;
				if (ctr > 1) res += alfa + "^" +ctr;
				add = true;
			}
		}
		return res;
	}

	/**
	 * Returns a string representation for this polynomial.
	 * <BR>For ex: 11010001011    GF(2) (only the coefficients are given).
	 * <BR>For ex: 11 01 00 01 11 GF(4)
	 */
	public String toCoefficientString(int pwr) {
		int len = coefs.length;
		boolean tolong = false, spaces = false;
		String mycoef;
		for (int ctr = 0; ctr < len; ctr++) {
			mycoef = field.valueString(coefs[ctr]);
			if (mycoef.length() > 1) tolong = true;
			if (mycoef.indexOf(' ') != -1) spaces = true;
		}		
		if (spaces) return toString();
		else {
			String res = "";
			for (int ctr = 0; ctr < len; ctr++) {
				mycoef = field.valueString(coefs[ctr]);
				if (tolong) res += " " + mycoef + " ";
				else res += mycoef;
			}
			for (int ctr = len; ctr < pwr; ctr++) {
				mycoef = field.valueString(GaloisField.ZERO);
				if (tolong) res += " " + mycoef + " ";
				else res += mycoef;
			}
			return res;
		}
	}

	/**
	 * Returns the degree.
	 */
	public int degree() {
		return coefs.length-1;
	}

	/**
	 * Check if two polynomials are equal. That is, if the coefficients are equal.
	 */
	public boolean equals(GaloisPolynomial b) {
		if (degree() != b.degree()) return false;
		for (int ctr = 0; ctr < coefs.length; ctr++) 
			if (coefs[ctr] != b.getCoefficient(ctr)) return false;

		return true;		
	}

	/**
	 * Returns the coefficient for position [pos].
	 * <BR>getCoefficient(0) returns the constant coefficient.
	 */
	public int getCoefficient(int pos) {
		if (pos > degree()) return GaloisField.ZERO;
		return coefs[pos];
	}

	/**
	 * Returns a copy of this polynomial.
	 * <BR>You should never use this method because a polynomial can't change.
	 * <BR>It uses value-semantics.
	 */
	public GaloisPolynomial copy() {
		return new GaloisPolynomial(coefs,alfa,field);
	}

	/**
	 * Returns the sum of 'this' and a given polynomial.
	 */
	public GaloisPolynomial sum(GaloisPolynomial b) {
		int degres = Math.max(degree(),b.degree());
		int[] sumcoef = new int[degres+1];
		for (int ctr = 0; ctr <= degres; ctr++) {
			sumcoef[ctr] = getCoefficient(ctr);
			sumcoef[ctr] = field.sum(sumcoef[ctr],b.getCoefficient(ctr)); 
		}		
		return new GaloisPolynomial(sumcoef,alfa,field);
	}

	/**
	 * Returns the difference between 'this' and a given polynomial.
	 */
	public GaloisPolynomial minus(GaloisPolynomial b) {
		int degres = Math.max(degree(),b.degree());
		int[] sumcoef = new int[degres+1];
		for (int ctr = 0; ctr <= degres; ctr++) {
			sumcoef[ctr] = getCoefficient(ctr);
			sumcoef[ctr] = field.minus(sumcoef[ctr],b.getCoefficient(ctr)); 
		}		
		return new GaloisPolynomial(sumcoef,alfa,field);
	}

	/**
	 * Returns the product of 'this' and a given polynomial.
	 */
	public GaloisPolynomial product(GaloisPolynomial b) {
		int degres = degree()+b.degree();
		int[] prodcoef = new int[degres+1];
		for (int ctr = 0; ctr <= degres; ctr++) {
			int coef = GaloisField.ZERO;
			for (int deg = ctr; deg >= 0; deg--) {
				coef = field.sum(coef,field.product(getCoefficient(deg),b.getCoefficient(ctr-deg)));
			}
			prodcoef[ctr] = coef;
		}
		return new GaloisPolynomial(prodcoef,alfa,field);
	}

	/**
	 * Returns the product of 'this' and a 'one term polynomial'.
	 * <BR>For ex: [coef]x^[pwr]
	 * @param coef The coefficient for the one-term.
	 * @param pwr The power for the one-term.
	 */
	public GaloisPolynomial product_one_term(int coef, int pwr) {
		int degres = degree()+pwr;
		int ctr;
		int[] prodcoef = new int[degres+1];

		for (ctr = 0; ctr < pwr; ctr++) prodcoef[ctr] = GaloisField.ZERO;

		for (ctr = 0; ctr <= degree(); ctr++)
			prodcoef[ctr+pwr] = field.product(coefs[ctr],coef);		

		return new GaloisPolynomial(prodcoef,alfa,field);
	}

	/**
	 * Returns the quotient of 'this' and a given polynomial.
	 * @exception GaloisException On division by zero.
	 */
	public GaloisPolynomial[] divide(GaloisPolynomial b) 
	throws GaloisException {
		GaloisPolynomial res[] = new GaloisPolynomial[2];
		GaloisPolynomial remainder;
		int[] divcoef = new int[Math.max(0,degree()-b.degree())+1];
		int minpower, nbcoef, nrcoef, mincoef;
		remainder = this.copy();
		nbcoef = b.getCoefficient(b.degree());
		do {
			minpower = remainder.degree()-b.degree();
			if (minpower >= 0) {
				nrcoef = remainder.getCoefficient(remainder.degree());
				mincoef = field.divide(nrcoef,nbcoef);
				GaloisPolynomial minpoly = b.copy();
				minpoly = minpoly.product_one_term(mincoef,minpower);
				remainder = remainder.minus(minpoly);
				divcoef[minpower] = mincoef;
			}
		} while (minpower > 0);
		res[0] = new GaloisPolynomial(divcoef,alfa,field);
		res[1] = remainder;
		return res;
	}

	private static int[] createCoefficients(int deg) {
		int rescoef[] = new int[deg+1];
		for (int ctr = 0; ctr < deg; ctr++) rescoef[ctr] = GaloisField.ZERO;
		return rescoef;
	}

	private static void incCoefficients(int[] rescoef, int deg, GaloisField field) {
		int idx, pos, prevpos;
		boolean carry;
		int card = field.getCardinality();
		pos = 0; 
		do {
			idx = rescoef[pos];			
			prevpos = pos;
			carry = false;
			idx++;
			if (idx >= card) {
				idx = 0;
				pos++;
				carry = true;
			}
			rescoef[prevpos] = idx;			
		} while (carry && pos <= deg);
	}

	/**
	 * Returns a vector containing all the possible irreducible monomials.
	 * <BR>in the galois field [field].
	 * @param deg The degree of the monomials.
	 * @param alfa The variable. For ex: 'x'.
	 * @param field The galois field.
	 */
	public static Vector irrMonomials(int deg, char alfa, GaloisField field) {
		int ctr, num, crdeg, idx, ldeg;
		boolean irr;
		int card = field.getCardinality();

		if (deg < 2) return new Vector();

		GaloisPolynomial zeropol = new GaloisPolynomial(ZEROCOEFS,alfa,field);

		Vector[] calc = new Vector[deg-1];
		for (ctr = 0; ctr < deg-1; ctr++)
			calc[ctr] = new Vector();

		for (crdeg = 2; crdeg <= deg; crdeg++) {
			num = GaloisField.power(card,crdeg);
			int rescoef[] = createCoefficients(crdeg);
			rescoef[crdeg] = GaloisField.ONE;
			for (ctr = 0; ctr < num; ctr++) {
				incCoefficients(rescoef, crdeg-1, field); 
				irr = true;
				GaloisPolynomial thispoly = new GaloisPolynomial(rescoef,alfa,field);

				idx = 0;
				do {
					do {
						idx++;
					} while (idx < card && idx == GaloisField.ZERO);
					if (thispoly.calcHorner(idx) == GaloisField.ZERO)
						irr = false;
				} while (idx < card && irr);

				for (ldeg = 2; ldeg < crdeg && irr; ldeg++) {
					for (idx = 0; idx < calc[ldeg-2].size() && irr; idx++) {
						try {
							GaloisPolynomial ldegpoly = (GaloisPolynomial)calc[ldeg-2].elementAt(idx);
							GaloisPolynomial[] divres = thispoly.divide(ldegpoly);
							if (divres[1].equals(zeropol)) irr = false;
						} catch (GaloisException e) {}
					}
				} 

				if (irr) calc[crdeg-2].addElement(thispoly);
			}
		} 	
		return calc[deg-2];
	}

	/**
	 * Returns a vector containing all possible polynomials degree 0 till [deg].
	 * <BR>in the galois field [field].
	 * @param deg The maximum degree of the polynomials.
	 * @param alfa The variable. For ex: 'x'.
	 * @param field The galois field.
	 */
	public static GaloisPolynomial[] allPolynomials(int deg, char alfa, GaloisField field) {
		int card = field.getCardinality();
		int num = GaloisField.power(card,deg+1);
		int rescoef[] = createCoefficients(deg);
		GaloisPolynomial[] res = new GaloisPolynomial[num];
		for (int ctr = 0; ctr < num; ctr++) {
			res[ctr] = new GaloisPolynomial(rescoef,alfa,field);
			incCoefficients(rescoef, deg, field);
		} 	
		return res;
	}

	/**
	 * Returns the variable. For ex: 'x'.
	 */
	public char getAlfa() {
		return alfa;
	}

	/**
	 * Returns the coefficient's galois field.
	 */
	public GaloisField getField() {
		return field;
	}

	private static int[] parsexpwrs(int mnpwr, int mxpwr, String poly, char newalfa, 
			GaloisField newfield) throws GaloisException {
		int pos = -1;
		int len = poly.length();
		int ch, stpos, pwr, xpos, level, mpos;
		int[] coefs = new int[mxpwr+1];
		boolean[] pwrused = new boolean[mxpwr];
		for (int ctr = 0; ctr < mxpwr; ctr++) pwrused[ctr] = false;
		//Parse the polynomial
		while (pos < len && (pos = poly.indexOf(newalfa,pos+1)) != -1) {
			//Zoek verder om de macht te bepalen
			pwr = 1; xpos = pos; pos++; 
			while (pos < len && poly.charAt(pos) == ' ') pos++;
			if (pos < len) {
				if (poly.charAt(pos) == '^') {
					pos++; while (pos < len && poly.charAt(pos) == ' ') pos++;
					//Read in power of x
					ch = poly.charAt(pos); stpos = pos;
					while (pos < len-1 && ch >= '0' && ch <= '9') ch = poly.charAt(++pos);	
					if (pos == len-1) pos++;
					if (pos > stpos) 
						pwr = GaloisField.atoi(poly.substring(stpos,pos));
				} 
				if (pos < len) {
					while (pos < len && poly.charAt(pos) == ' ') pos++;					
					if (pos < len && poly.charAt(pos) != '+') 
						throw new GaloisException("Parse: x^"+pwr+" must be followed by + or EOS",
								GaloisException.PARSE);
				}
			}
			//Zoek terug om de coefficient bij deze macht te bepalen
			mpos = xpos; level = 0;
			while (mpos >= 0 && (poly.charAt(mpos) != '+' || level > 0)) {
				ch = poly.charAt(mpos);
				if (ch == ')') level++;
				if (ch == '(') level--; 
				mpos--;
			}
			if (level != 0) 
				throw new GaloisException("Parse: () don't match",GaloisException.PARSE);
			if (mpos != 0) mpos++;
			//Watch out for power !
			if (pwrused[pwr-1])
				throw new GaloisException("Parse: Two times x^"+pwr+" encountered",
						GaloisException.PARSE);
			pwrused[pwr-1] = true;	
			//Nu is alles vanaf mpos tot xpos-1 de bijhorende coefficient.
			coefs[pwr] = newfield.parse(poly.substring(mpos,xpos),GaloisField.ONE);
			if (pwr == mnpwr && mpos != 0) 
				coefs[0] = newfield.parse(poly.substring(0,mpos-1),GaloisField.ZERO);
		}
		return coefs;
	}

	private static int[] parsespaces(String poly, GaloisField newfield)
	throws GaloisException {
		int pos, len, pwr, prevpos, nowpwr;
		pos = -1; len = poly.length(); pwr = 0;
		while (pos < len && (pos = poly.indexOf(' ',pos+1)) != -1) pwr++;
		int[] coefs = new int[pwr+1];
		pos = -1; prevpos = 0; nowpwr = 0;
		while (pos < len && (pos = poly.indexOf(' ',pos+1)) != -1) {
			coefs[nowpwr] = newfield.parse(poly.substring(prevpos,pos),GaloisField.ONE);
			nowpwr++; prevpos = pos+1;
		}
		if (prevpos < len) {
			coefs[nowpwr] = newfield.parse(poly.substring(prevpos,len),GaloisField.ONE);
		}
		return coefs;
	}

	private static int[] parsedigits(String poly, GaloisField newfield)
	throws GaloisException {
		int len = poly.length(); 
		int[] coefs = new int[len];
		for (int ctr = 0; ctr < len; ctr++) {
			String coefstrg = String.valueOf(poly.charAt(ctr));
			coefs[ctr] = newfield.parse(coefstrg,GaloisField.ONE);
		}
		return coefs;
	}

	private static int[] parsecoefs(String poly, char newalfa, GaloisField newfield) 
	throws GaloisException {
		int pos = -1;
		int len = poly.length();
		int ch, stpos, pwr, mxpwr = 0, mnpwr = -1;
		//Find out the highest power of x
		while (pos < len && (pos = poly.indexOf(newalfa,pos+1)) != -1) {
			//Zoek verder om de macht te bepalen
			pwr = 1; pos++;
			while (pos < len && poly.charAt(pos) == ' ') pos++;
			if (pos < len) {
				if (poly.charAt(pos) == '^') {
					pos++; while (pos < len && poly.charAt(pos) == ' ') pos++;
					//Read in power of x
					ch = poly.charAt(pos);
					if (ch < '0' || ch > '9') 
						throw new GaloisException("Parse: ^ must be followed by a number",GaloisException.PARSE);
					stpos = pos;
					while (pos < len-1 && ch >= '0' && ch <= '9') ch = poly.charAt(++pos);	
					if (pos == len-1) pos++;
					pwr = GaloisField.atoi(poly.substring(stpos,pos));
				}
			}
			if (pwr > mxpwr) mxpwr = pwr;
			if (pwr < mnpwr || mnpwr == -1) mnpwr = pwr;
		}
		if (mxpwr > 0) return parsexpwrs(mnpwr,mxpwr,poly,newalfa,newfield);
		String data = GaloisField.cleanString(poly);
		if (data.indexOf(' ') != -1) return parsespaces(data,newfield);
		int rootcard = newfield.getRoot().getCardinality();
		if (data.length() > 1 && rootcard < 10) return parsedigits(data,newfield);
		int[] coefs = new int[1];
		coefs[0] = newfield.parse(data,GaloisField.ZERO); 
		return coefs;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + Arrays.hashCode(coefs);
		result = PRIME * result + ((field == null) ? 0 : field.hashCode());
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
		final GaloisPolynomial other = (GaloisPolynomial) obj;
		if (!Arrays.equals(coefs, other.coefs))
			return false;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		return true;
	}
}

