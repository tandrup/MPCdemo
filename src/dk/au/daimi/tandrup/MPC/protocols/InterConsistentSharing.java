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
import dk.au.daimi.tandrup.MPC.protocols.messages.DisputeMessage;

public class InterConsistentSharing extends AbstractRandomProtocol {
	private Participant dealer;
	private Disputes disputes;
	private Corruption corruption;
	private DisputeMessage disputeMsg;

	private Field fieldF, fieldG;

	private Activity flipActivity;
	private Activity yShareActivity1t;
	private Activity yShareActivity2t;
	private Activity dealAllSharesActivity1t;
	private Activity dealAllSharesActivity2t;

	public InterConsistentSharing(Random random, CommunicationChannel channel, int threshold, Activity activity, Participant dealer, Disputes disputes, Corruption corruption, DisputeMessage disputeMsg, Field fieldF, Field fieldG) {
		super(random, channel, threshold, activity);
		this.dealer = dealer;
		this.disputes = disputes;
		this.fieldF = fieldF;
		this.fieldG = fieldG;
		this.corruption = corruption;
		this.disputeMsg = disputeMsg;

		this.flipActivity = activity.subActivity("Flip");
		this.yShareActivity1t = activity.subActivity("yShare").subActivity("1t");
		this.yShareActivity2t = activity.subActivity("yShare").subActivity("2t");
		this.dealAllSharesActivity1t = activity.subActivity("Dealer all shares").subActivity("1t");
		this.dealAllSharesActivity2t = activity.subActivity("Dealer all shares").subActivity("2t");
	}

	protected FieldPolynomial1D[] generateDSharings(FieldElement[] yis, int d) {
		FieldPolynomial1D[] polys = new FieldPolynomial1D[yis.length];
		for (int i = 0; i < yis.length; i++) {
			polys[i] = DisputeSharing.generatePoly(yis[i], disputes, corruption, d, random);
		}
		return polys;
	}

	public FieldElement[][] share(FieldElement[] yis) 
	throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException {
		Collection<? extends Participant> participants = channel.listConnectedParticipants();

		FieldElement[] ylShares1t = new FieldElement[yis.length];
		FieldElement[] ylShares2t = new FieldElement[yis.length];

		// Generate t- and 2t-sharings
		FieldPolynomial1D[] polys1t = generateDSharings(yis, threshold);
		FieldPolynomial1D[] polys2t = generateDSharings(yis, 2*threshold);
		FieldElement r = fieldG.element(random);
		FieldPolynomial1D polyRandom1t = DisputeSharing.generatePoly(r, disputes, corruption, threshold, random);
		FieldPolynomial1D polyRandom2t = DisputeSharing.generatePoly(r, disputes, corruption, 2*threshold, random);

		// save local poly
		disputeMsg.setLocalSharings(polys1t);

		// Distibute shares
		for (Participant participant : participants) {
			FieldElement x = fieldF.element(participant.getID());

			if (participant.equals(dealer)) {
				for (int i = 0; i < polys1t.length; i++) {
					ylShares1t[i] = polys1t[i].eval(x);
					ylShares2t[i] = polys2t[i].eval(x);
				}
			} else {
				for (int i = 0; i < polys1t.length; i++) {
					FieldElement y1t = polys1t[i].eval(x);
					FieldElement y2t = polys2t[i].eval(x);
					channel.send(participant, activity.subActivity("y" + i), y1t);
					channel.send(participant, activity.subActivity("Y" + i), y2t);
				}

				x = fieldG.element(participant.getID());
				FieldElement y1t = polyRandom1t.eval(x);
				FieldElement y2t = polyRandom2t.eval(x);
				channel.send(participant, activity.subActivity("r"), y1t);
				channel.send(participant, activity.subActivity("R"), y2t);
			}
		}

		// Step 3
		Flip flip = new Flip(fieldG, random, channel, threshold, flipActivity);
		FieldElement x = flip.run();

		// Calculate all y shares
		Map<Participant, FieldElement> yShares1t = new HashMap<Participant, FieldElement>();
		Map<Participant, FieldElement> yShares2t = new HashMap<Participant, FieldElement>();
		for (Participant participant : participants) {
			FieldElement idF = fieldF.element(participant.getID());
			FieldElement idG = fieldG.element(participant.getID());

			FieldElement rShare1t = polyRandom1t.eval(idG);
			FieldElement rShare2t = polyRandom2t.eval(idG);

			FieldElement yShare1t = rShare1t;
			FieldElement yShare2t = rShare2t;

			for (int i = 0; i < polys1t.length; i++) {
				int l = i + 1;
				FieldElement ylShare1t = polys1t[i].eval(idF);
				FieldElement ylShare2t = polys2t[i].eval(idF);
				yShare1t = fieldG.add(yShare1t, fieldG.multiply(x.pow(l), ylShare1t));
				yShare2t = fieldG.add(yShare2t, fieldG.multiply(x.pow(l), ylShare2t));
			}

			yShares1t.put(participant, yShare1t);
			yShares2t.put(participant, yShare2t);
		}

		// Step 4
		if (!disputes.contains(channel.localParticipant())) {
			// Broadcast resulting share
			channel.broadcast(yShareActivity1t, yShares1t.get(dealer));
			channel.broadcast(yShareActivity2t, yShares2t.get(dealer));
		}

		// Step 5
		DealerInterConsShares dealerShares1t = new DealerInterConsShares(yShares1t, fieldG);
		DealerInterConsShares dealerShares2t = new DealerInterConsShares(yShares2t, fieldG);
		channel.broadcast(dealAllSharesActivity1t, dealerShares1t);
		channel.broadcast(dealAllSharesActivity2t, dealerShares2t);

		// Step 6b
		step6b(dealerShares1t, dealerShares2t);

		return new FieldElement[][] {ylShares1t, ylShares2t};
	}

	protected FieldElement receiveShare(Activity activity, Field field) throws IOException, GeneralSecurityException, ClassNotFoundException {
		IChannelData chData = channel.receive(activity, dealer);
		if (chData == null)
			return field.zero();
		FieldElement share = (FieldElement)chData.getObject();
		return share;
	}

	public FieldElement[][] receive(int count) throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException {
		IChannelData chData;

		if (disputes.contains(dealer))
			return trivialShares(count, fieldF);

		FieldElement[] ylShares1t = new FieldElement[count];
		FieldElement[] ylShares2t = new FieldElement[count];
		FieldElement rShare1t;
		FieldElement rShare2t;

		//Receive shares from dealer
		for (int i = 0; i < count; i++) {
			ylShares1t[i] = receiveShare(activity.subActivity("y" + i), fieldF);
			ylShares2t[i] = receiveShare(activity.subActivity("Y" + i), fieldF);
		}
		rShare1t = receiveShare(activity.subActivity("r"), fieldG);
		rShare2t = receiveShare(activity.subActivity("R"), fieldG);

		// save remote shares
		disputeMsg.addRemoteSharings(dealer, ylShares1t);
		
		// Select random integer
		Flip flip = new Flip(fieldG, random, channel, threshold, flipActivity);
		FieldElement x = flip.run();

		// Calculate share of y
		FieldElement yShare1t = rShare1t;
		FieldElement yShare2t = rShare2t;
		for (int l = 1; l <= count; l++) {
			yShare1t = fieldG.add(yShare1t, fieldG.multiply(x.pow(l), ylShares1t[l-1]));
			yShare2t = fieldG.add(yShare2t, fieldG.multiply(x.pow(l), ylShares2t[l-1]));
		}

		// Step 4. Broadcast resulting share
		if (!disputes.contains(channel.localParticipant())) {
			channel.broadcast(yShareActivity1t, yShare1t);
			channel.broadcast(yShareActivity2t, yShare2t);
		}

		// Step 5
		chData = channel.receive(dealAllSharesActivity1t, dealer);
		DealerInterConsShares dealerShares1t = (DealerInterConsShares)chData.getObject();
		chData = channel.receive(dealAllSharesActivity2t, dealer);
		DealerInterConsShares dealerShares2t = (DealerInterConsShares)chData.getObject();

		// Step 6
		FieldPolynomial1D dealerPoly1t = dealerShares1t.interpolate();
		FieldPolynomial1D dealerPoly2t = dealerShares2t.interpolate();
		int degree1t = dealerPoly1t.degree();
		int degree2t = dealerPoly2t.degree();
		if (degree1t > threshold || degree2t > 2*threshold)
			return trivialShares(count, fieldF);

		step6b(dealerShares1t, dealerShares2t);

		return new FieldElement[][] {ylShares1t, ylShares2t};
	}

	private void step6b(DealerInterConsShares dealerShares1t, DealerInterConsShares dealerShares2t) throws IOException, GeneralSecurityException, ClassNotFoundException {
		// Step 6b
		for (Participant participant : channel.listConnectedParticipants()) {
			if (!channel.localParticipant().equals(participant) && !disputes.contains(participant)) {
				IChannelData chData;
				FieldElement partShare1t = null, partShare2t = null;

				chData = channel.receive(yShareActivity1t, participant);
				if (chData != null)
					partShare1t = (FieldElement)chData.getObject(); 

				chData = channel.receive(yShareActivity2t, participant);
				if (chData != null)
					partShare2t = (FieldElement)chData.getObject(); 

				FieldElement dealerShare1t = dealerShares1t.getValue(fieldG.element(participant.getID()));
				FieldElement dealerShare2t = dealerShares2t.getValue(fieldG.element(participant.getID()));

				if (partShare1t == null || partShare2t == null || !(partShare1t.equals(dealerShare1t) && partShare2t.equals(dealerShare2t))) {
					logger.info("Adding dispute between " + dealer + " <-> " + participant);				
					disputes.add(dealer, participant);
				}
			}
		}
	}

	public FieldElement[][] trivialShares(int count, Field fieldF) {
		FieldElement[][] retVal = new FieldElement[2][count];

		for (int i = 0; i < count; i++) {
			retVal[0][i] = fieldF.zero();
			retVal[1][i] = fieldF.zero();
		}

		return retVal;
	}
}

class DealerInterConsShares implements Serializable {
	private static final long serialVersionUID = 1L;

	private Map<FieldElement, FieldElement> shares = new HashMap<FieldElement, FieldElement>();

	public DealerInterConsShares(Map<Participant, FieldElement> yShares, Field field) {
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
