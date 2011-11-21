package dk.au.daimi.tandrup.MPC.math.fields;


public class IllegalFieldException extends FieldException {
	private static final long serialVersionUID = 1L;

	public IllegalFieldException() {
		super();
	}

	public IllegalFieldException(String arg0) {
		super(arg0);
	}

	public IllegalFieldException(Throwable arg0) {
		super(arg0);
	}

	public IllegalFieldException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
}
