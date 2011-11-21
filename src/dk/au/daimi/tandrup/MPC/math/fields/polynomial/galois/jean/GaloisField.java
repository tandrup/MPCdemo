package dk.au.daimi.tandrup.MPC.math.fields.polynomial.galois.jean;

/*
GALOISFIELD

This Package is written by Jan Struyf
URL: http://ace.ulyssis.student.kuleuven.ac.be/~jeans
EMail: jan.struyf@student.kuleuven.ac.be
Post:	Jan Struyf, Hoogstraat 47, 3360 Bierbeek, BELGIUM

You can do anything with it, if you leave this comment field unmodified.

http://java.sun.com/products/jdk/1.1/docs/api/
 */

import java.util.Vector;

public class GaloisField {
	int card;
	int dmode;
	int alfa;

	/**
	 * A constant for the galois element zero '0' 
	 */
	public static final int ZERO = 0;
	/**
	 * A constant for the galois element one '1'
	 */
	public static final int ONE = 1;
	/**
	 * A constant for the display mode ALFAPOWER
	 * <BR>For ex: valueString(a) -> a^k
	 */
	public static final int ALFAPOWER = 0;
	/**
	 * A constant for the display mode POLYNOMIAL
	 * <BR>For ex: valueString(a) -> 1+a
	 */
	public static final int POLYNOMIAL = 1;
	/**
	 * A constant for the display mode POLYCOEFS
	 * <BR>For ex: valueString(a) -> 1 1
	 */
	public static final int POLYCOEFS = 2;

	/**
	 * Creates a new base galois field.
	 * @param p The cardinality for the new field. 
	 * @exception GaloisException If p is not a prime number.
	 */
	public GaloisField(int p) throws GaloisException {
		this(prime(p),findAlfa(p));
	}	

	/**
	 * This constructor is for internal use only. Do not call it directly!
	 * @param newcard The cardinality for the new field.
	 * @param newalpha The index of the primitive element. 
	 */
	protected GaloisField(int newcard, int newalfa) {
		card = newcard;
		dmode = POLYNOMIAL;
		alfa = newalfa;
	}	

	/**
	 * Set a new display mode.
	 * <BR> See: 'ALFAPOWER'
	 * <BR> See: 'POLYNOMIAL'
	 * <BR> See: 'POLYCOEFS'
	 * @param newmode One of the supported display modes.
	 */
	public void setDisplayMode(int newmode) {
		dmode = newmode;
	}

	/**
	 * Returns the display mode.
	 * <BR> See: setDisplayMode
	 */
	public int getDisplayMode() {
		return dmode;
	}

	/**
	 * Returns the cardinality.
	 */
	public int getCardinality() {
		return card;
	}

	/**
	 * Normalises a galois element.
	 * <BR>Returns the modulo value of the element in [0,getCardinality()-1].
	 */
	public int normalise(int element) {
		int c = element % card;
		if (c < 0) c += card;
		return c;		
	}

	/**
	 * Returns a String representation of this field.
	 * <BR>Something like "Base Galois Field GF(5)".
	 */
	public String toString() {
		return "Base Galois Field GF("+card+").";
	}

	/**
	 * Returns the sum of two galois elements.
	 */
	public int sum(int a, int b) {
		return (a + b) % card;
	}

	/**
	 * Returns a - b.
	 */
	public int minus(int a, int b) {	
		return normalise(a - b);
	}

	/**
	 * Returns - b.
	 */
	public int negative(int b) {	
		return normalise(ZERO - b);
	}


	/**
	 * Returns the product of two galois elements.
	 */
	public int product(int a, int b) {
		return (a * b) % card;
	}

	public int inverse(int b) {
		if ((b % card) == ZERO)
			throw new GaloisException("Divide by zero",GaloisException.DIVIDE);
		return glPower(b,card-2);
	}
	
	/**
	 * Returns a / b.
	 * @exception GaloisException If b = 0 (Division by zero).
	 */
	public int divide(int a, int b) 
	throws GaloisException {
		return (a * inverse(b)) % card;
	}

	/**
	 * Returns the cyclotomic of [elem] modulo [modulo].
	 * <BR>This is an array of primitive element powers.
	 * @param modulo The modulo parameter for the cyclotomic. 
	 * @param elem The starting power. 
	 */
	public int[] getCyclotomic(int modulo, int elem) {
		int betw, ctr;
		betw = elem; ctr = 0;
		do {
			betw *= card;
			betw %= modulo;
			ctr++;
		} while (betw != elem && ctr <= modulo);
		int[] res = new int[ctr];
		betw = elem; ctr = 0; 
		do {
			res[ctr] = betw;
			betw *= card;
			betw %= modulo;
			ctr++;
		} while (betw != elem && ctr <= modulo);
		return res;
	}

	/**
	 * Returns all the cyclotomics modulo [modulo].
	 * <BR>This is a Vector of cyclotomics.
	 * <BR> See: getCyclotomic
	 * @param modulo The modulo parameter for the cyclotomic. 
	 */
	public Vector getAllCyclotomics(int modulo) {
		Vector res = new Vector();
		boolean ntfound;
		for (int ctr = 0; ctr < modulo; ctr++) {
			ntfound = true;
			for (int tel = 0; tel < res.size() && ntfound; tel++) {
				int[] myclass = (int[])res.elementAt(tel);
				for (int cou = 0; cou < myclass.length && ntfound; cou++) 
					if (ctr == myclass[cou]) ntfound = false;
			}
			if (ntfound) {
				int[] newcyclo = getCyclotomic(modulo,ctr);
				res.addElement(newcyclo);
			}
		}
		return res;
	}

	/**
	 * Method for construction of primitive roots for 'one'.
	 * <BR>Returns an array of two integers.
	 * <BR>The first is the k-parameter for the extended field containing the root.
	 * <BR>The second is the power p of the primitive element in that field.
	 * <BR>beta^n = 1, beta = alpha^p in GF(base^k).
	 * @param n Take the n-th root of 'one'.
	 */
	public int[] getKPrimitiveNRoot(int n) {
		int pwr = card;
		int k = 1;
		while (pwr % n != 1 && k < 1000) {
			k++;
			pwr *= card;
		}
		int[] res = new int[2];
		res[0] = k;
		res[1] = (pwr-1)/n;
		return res;
	}

	/**
	 * Returns a minimal polynomial.
	 * @param alphapwr To identify the polynomial: p(alpha^alphapwr) = 0.
	 * @param talpha The polynomial variable. For ex: 'x'.
	 * @exception GaloisException If an error occurs.
	 */
	public GaloisPolynomial getMinimalPolynomial(int alphapwr, char talfa)
	throws GaloisException {
		int[] cyclo = getBase().getCyclotomic(getCardinality()-1,alphapwr);
		GaloisPolynomial betw = 
			GaloisPolynomial.createConstant(GaloisField.ONE,talfa,this);
		int[] coefs = new int[2];
		coefs[1] = GaloisField.ONE;
		for (int ctr = 0; ctr < cyclo.length; ctr++) {
			coefs[0] = minus(GaloisField.ZERO,getAlfaPower(cyclo[ctr]));
			GaloisPolynomial factor = 
				new GaloisPolynomial(coefs,talfa,this);
			betw = betw.product(factor);
		}
		return betw.toBase();
	}

	/**
	 * Returns a power of the primitive element.
	 */
	public int getAlfaPower(int pwr) {
		return glPower(alfa,pwr);
	}

	/**
	 * Returns a String representation for the given element.
	 */
	public String valueString(int a) {
		return String.valueOf(a);
	}

	/**
	 * Is this field a base field?.
	 */
	public boolean isBase() {
		return true;
	}

	/**
	 * Returns the base field.
	 */
	public GaloisField getBase() {
		return this;
	}

	/**
	 * Returns the root field.
	 * <BR>If 'this' = GF((p^k)...^q)) this method returns GF(p).  
	 */
	public GaloisField getRoot() {
		GaloisField nowfld = this;
		while (!nowfld.isBase()) nowfld = nowfld.getBase();
		return nowfld;
	}

	/**
	 * Converts a galois element from 'this' field to the base field.
	 * @exception GaloisException If the element can't be found in the base field.
	 */
	public int toBase(int a) throws GaloisException {
		return a;
	}

	/**
	 * Converts a galois element from the base field to 'this' field.
	 * @exception GaloisException If the element can't be converted.
	 */
	public int toSuper(int a) throws GaloisException {
		return a;
	}

	/**
	 * Converts a galois element from 'this' field to a given ExtendedGaloisField.
	 * @exception GaloisException If 'this' is no sub-field of the given field.
	 */
	public int toExtended(int a, GaloisField till) throws GaloisException {
		GaloisField nowin, other;
		int elem = a;
		nowin = this;
		while (nowin != till) {
			other = till;
			while (other.getBase() != nowin) {
				if (other.isBase()) 
					throw new GaloisException(
							"toExtended: element not in a base field",
							GaloisException.FIELDCONV);
				other = other.getBase();
			}
			elem = other.toSuper(elem);
			nowin = other;
		}
		return elem;
	}


	/**
	 * Returns the character used for alpha in a polynomial representation.
	 */
	public char getAlfa() {
		return '*';
	}

	/**
	 * Cleans a string. Removes all surrounding spaces and parantheses.
	 * <BR>For ex: " ( (lk()dj )) " -> "lk()dj"
	 */
	public static String cleanString(String strg) {
		int len = strg.length();
		int psa = 0, psb = len-1;
		boolean again;
		do {
			while (psa < len && strg.charAt(psa) == ' ') psa++;
			while (psb > 0 && (strg.charAt(psb) == ' ' || strg.charAt(psb) == '*')) psb--;
			if (psb < psa) return "";
			again = false;
			if (psa < len && psb > 0 && strg.charAt(psa) == '(' && strg.charAt(psb) == ')') {
				psa++; psb--; again = true;
			}
		} while (again);
		return strg.substring(psa,psb+1);
	}

	/**
	 * Converts a string to a galois element.
	 * @exception GaloisException If the string is no valid representation.
	 */
	public int parse(String strg, int onempty) throws GaloisException {
		String mystrg = cleanString(strg);
		if (mystrg.length() == 0) return onempty;
		int c = atoi(mystrg);
		if (c >= card) throw new GaloisException("parse: Digit not in root field",
				GaloisException.PARSE);
		return c;
	}

	private int glPower(int what, int vl) {
		vl %= card-1;
		int res = 1;
		for (int ctr = 1; ctr <= vl; ctr++)
			res *= what;
		return res % card;		
	} 

	private static int prime(int p) throws GaloisException {
		int till = (int)Math.round(Math.sqrt(p));
		for (int ctr = 2; ctr < till; ctr++)
			if (p % ctr == 0) 
				throw new GaloisException("Base field cardinality not prime",
						GaloisException.PRIMITIVE);	
		return p;
	}

	private static boolean isAlfa(int myalfa, int num) throws GaloisException {
		int reachnum, count;
		boolean[] reached = new boolean[num-1];
		int betw = myalfa;		
		reachnum = 1; count = 0;
		do {
			for (int ctr = 0; ctr < num-1; ctr++)
				if (ctr+1 == betw && reached[ctr] == false) {
					reachnum++;
					reached[ctr] = true;
				}
			betw = (betw*myalfa) % num;
			count++;
		} while (reachnum < num && betw != myalfa && count < num);
		return reachnum == num;
	}

	private static int findAlfa(int num) throws GaloisException {
		for (int ctr = GaloisField.ONE; ctr < num; ctr++) {
			if (isAlfa(ctr,num)) return ctr;
		}
		throw new GaloisException("Can't find primitive element",GaloisException.PRIMITIVE);
	}

	/**
	 * Converts a string to an integer.
	 * @exception GaloisException If there are illegal characters in the string.
	 */
	public static int atoi(String intvl) throws GaloisException {
		int betw, len, ctr;
		char ch;
		betw = 0;
		len = intvl.length();
		for (ctr = 0; ctr < len; ctr++) {
			betw *= 10;
			ch = intvl.charAt(ctr);
			if (ch < '0' || ch > '9') 
				throw new GaloisException("Atoi: Character not a digit [0..9]",GaloisException.PARSE);
			betw += (int)(ch-'0');
		}
		return betw;
	}

	/**
	 * Calculates the power of a given integer.
	 * <BR>Warning: this is not the power function for galois elements.
	 */
	public static int power(int what, int vl) {
		int res = 1;
		for (int ctr = 1; ctr <= vl; ctr++)
			res *= what;
		return res;		
	} 

	/**
	 * Returns a string length [len] containing nothing but characters [ch].
	 */
	public static String fillString(char ch, int len) {
		char[] str = new char[len];
		for (int ctr = 0; ctr < len; ctr++) str[ctr] = ch;
		return new String(str);
	}

	/**
	 * Returns (a mod b).
	 */
	public static int modulo(int a, int b) {
		int c = a % b;
		if (c < 0) c += b;
		return c;		
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + alfa;
		result = PRIME * result + card;
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
		final GaloisField other = (GaloisField) obj;
		if (alfa != other.alfa)
			return false;
		if (card != other.card)
			return false;
		return true;
	}
}

