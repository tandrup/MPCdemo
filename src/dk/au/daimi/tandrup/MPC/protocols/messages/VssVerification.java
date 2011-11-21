package dk.au.daimi.tandrup.MPC.protocols.messages;

import java.io.Serializable;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public class VssVerification implements Serializable {
	private static final long serialVersionUID = 1L;

	public FieldElement sij;
	public VssVerification(FieldElement sij) {
		this.sij = sij;
	}
	@Override
	public String toString() {
		return "VSS Verify sij = " + sij;
	}
}
