package dk.au.daimi.tandrup.MPC.math.fields;

public class FieldException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public FieldException() {
		super();
	}

	public FieldException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public FieldException(String arg0) {
		super(arg0);
	}

	public FieldException(Throwable arg0) {
		super(arg0);
	}
}
