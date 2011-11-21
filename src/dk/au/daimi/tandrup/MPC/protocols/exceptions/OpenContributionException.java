package dk.au.daimi.tandrup.MPC.protocols.exceptions;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Participant;

public class OpenContributionException extends ProtocolException {
	private static final long serialVersionUID = 1L;

	private final Participant pKing;
	private final FieldElement shareIDs[];
	private final FieldElement shares[];

	public OpenContributionException(Participant pKing) {
		super("Contribution Error from " + pKing);
		this.pKing = pKing;
		this.shareIDs = null;
		this.shares = null;
	}
	
	public OpenContributionException(Participant pKing, FieldElement[] shareIDs, FieldElement[] shares) {
		super("Contribution Error from " + pKing);
		this.pKing = pKing;
		this.shareIDs = shareIDs;
		this.shares = shares;
	}

	public Participant getPKing() {
		return pKing;
	}

	public FieldElement[] getShareIDs() {
		return shareIDs;
	}

	public FieldElement[] getShares() {
		return shares;
	}


}
