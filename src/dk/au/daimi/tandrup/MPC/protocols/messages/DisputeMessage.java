package dk.au.daimi.tandrup.MPC.protocols.messages;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.protocols.exceptions.ProtocolException;

public class DisputeMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	private Participant part;
	private FieldPolynomial1D[] localSharings;	
	private Map<Participant, FieldElement[]> remoteSharings = new TreeMap<Participant, FieldElement[]>();

	public DisputeMessage(Participant part) {
		super();
		this.part = part;
	}

	public void setLocalSharings(FieldPolynomial1D[] localSharings) {
		this.localSharings = localSharings;
	}

	public void addRemoteSharings(Participant dealer, FieldElement[] shares) {
		remoteSharings.put(dealer, shares);
	}

	public FieldElement getShare(int id, Participant from, Participant to) {
		Field field = localSharings[id].elemField();
		if (part.equals(from)) {
			return localSharings[id].eval(field.element(to.getID()));
		} else if (part.equals(to)) {
			return remoteSharings.get(from)[id];
		}
		throw new ProtocolException("Unknown share");
	}

	public FieldPolynomial1D getMyShares(int id) {
		return localSharings[id];
	}

	@Override
	public String toString() {
		String retVal = "DisputeMessage from " + part + "\nlocalSharings: " + Arrays.toString(localSharings) + "\nremoteSharings: " + remoteSharings;
		return retVal;
	}
}
