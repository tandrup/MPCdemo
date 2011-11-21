package dk.au.daimi.tandrup.MPC.protocols.messages;

import java.io.Serializable;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class OpenResult implements Serializable {
	private static final long serialVersionUID = 1L;

	private final boolean error;
	private final FieldElement result;

	public OpenResult(FieldElement share) {
		super();
		this.error = false;
		this.result = share;
	}

	public OpenResult() {
		super();
		this.error = true;
		this.result = null;
	}

	public boolean isError() {
		return error;
	}
	
	public FieldElement getResult() {
		return result;
	}

	@Override
	public String toString() {
		if (isError())
			return "OpenResult(ERROR)";
		else
			return "OpenResult(" + result + ")";
	}

}
