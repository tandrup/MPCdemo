package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Random;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.Lagrange;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public class DisputeSharing extends AbstractRandomProtocol {
	private Participant dealer;
	public DisputeSharing(Random random, CommunicationChannel channel, int threshold, Activity activity, Participant dealer) {
		super(random, channel, threshold, activity);
		this.dealer = dealer;
	}
	
	public static FieldPolynomial1D generatePoly(FieldElement secret, Disputes dispute, Corruption corruption, int threshold, Random random) {
		if (dispute.size() > threshold)
			throw new IllegalStateException("Too many disputes: " + dispute.size());
		
		Field field = secret.field();
		
		FieldElement[] shares = new FieldElement[threshold+1];
		FieldElement[] indexs = new FieldElement[threshold+1];
		
		// Save secret
		shares[0] = secret;
		indexs[0] = field.zero();
		
		int i = 1, maxID = 0;
		for (Participant part : dispute.getParticipants()) {
			shares[i] = field.zero();
			indexs[i] = field.element(part.getID());
			i++;
			
			if (maxID < part.getID())
				maxID = part.getID();
		}
		
		while (i < shares.length) {
			shares[i] = field.element(random);
			indexs[i] = field.element(maxID + i);
			i++;
		}
		
		return Lagrange.interpolate(indexs, shares);
	}
	
	public void share(FieldElement secret, Disputes dispute, Corruption corruption, Collection<? extends Participant> participants) throws IOException, GeneralSecurityException, ClassNotFoundException {
		Field field = secret.field();
		
		FieldPolynomial1D poly = generatePoly(secret, dispute, corruption, threshold, random);
		
		for (Participant participant : participants) {
			FieldElement x = field.element(participant.getID());
			FieldElement y = poly.eval(x);
			channel.send(participant, activity, y);
		}
	}
	
	public FieldElement receive() throws IOException, GeneralSecurityException, ClassNotFoundException {
		IChannelData data = channel.receive(activity, dealer);
		
		FieldElement share = (FieldElement)data.getObject();
		
		return share;
	}
}
