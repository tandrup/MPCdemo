package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Random;

import dk.au.daimi.tandrup.MPC.math.VanDerMondeMatrix;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public class DoubleRandom extends AbstractRandomProtocol {
	private int l;
	private Field F, G;
	private Disputes disputes;
	Collection<? extends Participant> participants;
		
	private boolean running = false;

	private FieldElement[] xs1;
	private FieldElement[] ys1;
	private FieldElement[] xs2;
	private FieldElement[] ys2;
	
	public DoubleRandom(Random random, CommunicationChannel channel, int threshold, Activity activity, int l, Field F, Field G, Disputes disputes) {
		super(random, channel, threshold, activity);
		this.l = l;
		this.F = F;
		this.G = G;
		this.disputes = disputes;

		xs1 = new FieldElement[l + threshold];
		ys1 = new FieldElement[l + threshold];
		xs2 = new FieldElement[l + threshold];
		ys2 = new FieldElement[l + threshold];
	}
	
	public RandomPair[] run() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException {
		synchronized (this) {
			if (running)
				throw new IllegalStateException("Protocol is alreay running");
			running = true;
		}

		participants = channel.listConnectedParticipants();
		FieldElement si = F.element(random);
		
		Participant me = channel.localParticipant();
					
		Activity dealerActivity;
		DisputeSharing meSharing;

		dealerActivity = activity.subActivity("Sharing t " + me.getID());
		meSharing = new DisputeSharing(random, channel, threshold, dealerActivity, me);
		meSharing.share(si, disputes, new Corruption(), participants);

		dealerActivity = activity.subActivity("Sharing 2t " + me.getID());
		meSharing = new DisputeSharing(random, channel, 2*threshold, dealerActivity, me);
		meSharing.share(si, disputes, new Corruption(), participants);

		for (Participant participant : participants) {
			dealerActivity = activity.subActivity("Sharing t " + participant.getID());
			meSharing = new DisputeSharing(random, channel, threshold, dealerActivity, participant);
			si = meSharing.receive();
			processReceived1(participant.getID(), si);

			dealerActivity = activity.subActivity("Sharing 2t " + participant.getID());
			meSharing = new DisputeSharing(random, channel, 2*threshold, dealerActivity, participant);
			si = meSharing.receive();
			processReceived2(participant.getID(), si);
		}
		
		VanDerMondeMatrix mF = new VanDerMondeMatrix(F, participants.size(), participants.size() - threshold).transpose();
		VanDerMondeMatrix mG = new VanDerMondeMatrix(G, participants.size(), participants.size() - threshold).transpose();

		FieldElement[] ris1 = mF.multiply(ys1);
		FieldElement[] ris2 = mG.multiply(ys2);
		
		RandomPair[] result = new RandomPair[l];
		
		for (int i = 0; i < result.length; i++) {
			result[i] = new RandomPair(ris1[i], ris2[i]);
		}
		
		synchronized (this) {
			running = false;
			return result;
		}
	}
	
	private synchronized void processReceived1(int id, FieldElement si) {
		xs1[id-1] = F.element(id);
		ys1[id-1] = si;
	}

	private synchronized void processReceived2(int id, FieldElement si) {
		xs2[id-1] = G.element(id);
		ys2[id-1] = si;
	}
}
