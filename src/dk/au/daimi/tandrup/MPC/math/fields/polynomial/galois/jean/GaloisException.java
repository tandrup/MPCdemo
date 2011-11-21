package dk.au.daimi.tandrup.MPC.math.fields.polynomial.galois.jean;

import dk.au.daimi.tandrup.MPC.math.fields.FieldException;

public class GaloisException extends FieldException {
	private static final long serialVersionUID = 1L;
	private String outstr;
	private int code;

	public static final int POLYNOMIAL = 0;
	public static final int DIVIDE = 1;
	public static final int MODULOPOLY = 2;
	public static final int PRIMITIVE = 3;
	public static final int PARSE = 4;
	public static final int FIELDCONV = 5;
	public static final int GENERAL = 6;
	public static final int CODE = 7;

	public GaloisException(String newstr, int newcode) {
		outstr = newstr;
		code = newcode;
	}

	public String toString() {
		return "GaloisException: " + outstr;
	}
}