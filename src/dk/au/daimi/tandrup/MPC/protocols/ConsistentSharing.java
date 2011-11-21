package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.Lagrange;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public class ConsistentSharing extends AbstractRandomProtocol {
	private Participant dealer;
	private int d;
	private Disputes disputes;
	
	private Activity flipActivity;
	private Activity yShareActivity;
	private Activity dealAllSharesActivity;
	
	public ConsistentSharing(Random random, CommunicationChannel channel, int threshold, Activity activity, Participant dealer, int d, Disputes disputes) {
		super(random, channel, threshold, activity.subActivity("ConsistenSharing from " + dealer.getID()));
		this.dealer = dealer;
		this.d = d;
		this.disputes = disputes;
		
		this.flipActivity = activity.subActivity("Flip");
		this.yShareActivity = activity.subActivity("yShare");
		this.dealAllSharesActivity = activity.subActivity("Dealer all shares");
	}

	public FieldElement[] share(FieldElement[] yis) 
	throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException {
		Collection<? extends Participant> participants = channel.listConnectedParticipants();
		
		Field field = yis[0].field();
		FieldElement[] ylShares = new FieldElement[yis.length];
		
		// Generate d-sharings
		FieldPolynomial1D[] polys = new FieldPolynomial1D[yis.length];
		for (int i = 0; i < yis.length; i++) {
			polys[i] = DisputeSharing.generatePoly(yis[i], disputes, new Corruption(), threshold, random);
		}
		FieldElement r = field.element(random);
		FieldPolynomial1D polyRandom = DisputeSharing.generatePoly(r, disputes, new Corruption(), threshold, random);
		
		// Distibute shares
		for (Participant participant : participants) {
			FieldElement x = field.element(participant.getID());

			if (participant.equals(dealer)) {
				for (int i = 0; i < polys.length; i++) {
					ylShares[i] = polys[i].eval(x);
				}
			} else {
				for (int i = 0; i < polys.length; i++) {
					FieldElement y = polys[i].eval(x);
					channel.send(participant, activity.subActivity("y" + i), y);
				}

				FieldElement y = polyRandom.eval(x);
				channel.send(participant, activity.subActivity("r"), y);
			}
		}

		// Step 3
		Flip flip = new Flip(field, random, channel, threshold, flipActivity);
		FieldElement x = flip.run();

		// Calculate all shares
		Map<Participant, FieldElement> yShares = new HashMap<Participant, FieldElement>();
		for (Participant participant : participants) {
			FieldElement id = field.element(participant.getID());
			
			FieldElement rShare = polyRandom.eval(id);
			
			FieldElement yShare = rShare;
			
			for (int i = 0; i < polys.length; i++) {
				int l = i + 1;
				FieldElement ylShare = polys[i].eval(id);
				yShare = yShare.add(x.pow(l).multiply(ylShare));
			}
			
			yShares.put(participant, yShare);
		}
		
		// Step 4
		if (!disputes.contains(channel.localParticipant())) {
			// Broadcast resulting share
			channel.broadcast(yShareActivity, yShares.get(dealer));
		}
		
		// Step 5
		channel.broadcast(dealAllSharesActivity, new DealerShares(yShares, field));
		
		return ylShares;
	}
	
	public FieldElement[] receive(Field field, int count) throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException {
		if (disputes.contains(dealer))
			return trivialShares(count, field);
		
		FieldElement[] ylShares = new FieldElement[count];
		FieldElement rShare;
		
		for (int i = 0; i < count; i++) {
			IChannelData chData = channel.receive(activity.subActivity("y" + i), dealer);
			
			ylShares[i] = (FieldElement)chData.getObject();
		}
		IChannelData chData = channel.receive(activity.subActivity("r"), dealer);
		rShare = (FieldElement)chData.getObject();
		
		// Select random integer
		Flip flip = new Flip(field, random, channel, threshold, flipActivity);
		FieldElement x = flip.run();
		
		// Calculate share of y
		FieldElement yShare = rShare;
		for (int l = 1; l <= count; l++) {
			yShare = yShare.add(x.pow(l).multiply(ylShares[l-1]));
		}
		
		if (!disputes.contains(channel.localParticipant())) {
			// Broadcast resulting share
			channel.broadcast(yShareActivity, yShare);
		}
		
		chData = channel.receive(dealAllSharesActivity, dealer);
		DealerShares dealerShares = (DealerShares)chData.getObject();

		FieldPolynomial1D dealerPoly = dealerShares.interpolate();
		int degree = dealerPoly.degree();
		if (degree > d)
			return trivialShares(count, field);
		
		for (Participant participant : channel.listConnectedParticipants()) {
			if (!channel.localParticipant().equals(participant) && !disputes.contains(participant)) {
				chData = channel.receive(yShareActivity, participant);
				FieldElement share = (FieldElement)chData.getObject(); 
				if (!share.equals(dealerShares.getValue(field.element(participant.getID()))))
					disputes.add(dealer, participant);
			}
		}
		
		return ylShares;
	}
	
	public FieldElement[] trivialShares(int count, Field field) {
		FieldElement[] retVal = new FieldElement[count];
		
		for (int i = 0; i < retVal.length; i++) {
			retVal[i] = field.zero();
		}
		
		return retVal;
	}
}

class DealerShares implements Serializable {
	private static final long serialVersionUID = 1L;

	private Map<FieldElement, FieldElement> shares = new HashMap<FieldElement, FieldElement>();

	public DealerShares(Map<Participant, FieldElement> yShares, Field field) {
		for (Entry<Participant, FieldElement> entry : yShares.entrySet()) {
			FieldElement x = field.element(entry.getKey().getID());
			FieldElement y = entry.getValue();
			shares.put(x, y);
		}
	}
	
	public FieldElement getValue(FieldElement x) {
		return shares.get(x);
	}
	
	public FieldPolynomial1D interpolate() {
		return Lagrange.interpolate(shares.entrySet());
	}

	@Override
	public String toString() {
		String str = null;
		for (Entry<FieldElement, FieldElement> entry : shares.entrySet()) {
			if (str == null)
				str = entry.getKey() + " = " + entry.getValue();
			else
				str += ", " + entry.getKey() + " = " + entry.getValue();
		}
		return "DealerShares(" + str + ")";
	}
}
