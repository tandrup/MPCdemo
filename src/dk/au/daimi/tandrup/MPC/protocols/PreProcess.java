package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import dk.au.daimi.tandrup.MPC.math.BerlekampWelch;
import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.Lagrange;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.Triples.Triple;
import dk.au.daimi.tandrup.MPC.protocols.exceptions.OpenContributionException;
import dk.au.daimi.tandrup.MPC.protocols.gates.*;
import dk.au.daimi.tandrup.MPC.protocols.messages.DisputeMessage;

public class PreProcess extends AbstractRandomProtocol {
	private int rCount, iCount, mCount;
	private Participant[] inputProviders;
	private Field fieldF, fieldG;
	private Disputes disputes;
	private Corruption corruption;

	private RandomPair[] randomPairs;

	private RandomPair[] randomInputPairs;
	private RandomPair[] randomRandomPairs;
	private RandomPair[] randomMultPairs;

	private PreProcessResult result;

	private DisputeMessage disputeMsg;
	
	public PreProcess(Random random, CommunicationChannel channel, int threshold, Activity activity, int rCount, Participant[] inputProviders, int mCount, int aCount, int oCount, Field fieldF, Field fieldG, Disputes disputes) {
		super(random, channel, threshold, activity);
		this.rCount = rCount;
		this.inputProviders = inputProviders;
		this.iCount = inputProviders.length;
		this.mCount = mCount;
		this.fieldF = fieldF;
		this.fieldG = fieldG;
		this.disputes = disputes;

		result = new PreProcessResult(rCount, iCount, mCount, aCount, oCount);
		disputeMsg = new DisputeMessage(channel.localParticipant());
	}

	PreProcess(Random random, CommunicationChannel channel, int threshold, Activity activity, int rCount, Participant[] inputProviders, int mCount, int aCount, int oCount, Field fieldF, Field fieldG, Disputes disputes, RandomPair[] randomPairs) {
		this(random, channel, threshold, activity, rCount, inputProviders, mCount, aCount, oCount, fieldF, fieldG, disputes);
		this.randomPairs = randomPairs;
	}

	public void run() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException {
		while (true) {
			try {
				logger.config("Constructing random pairs");
				constructRandomPairs();

				logger.config("Constructing random gates");
				constructRandomGates();

				logger.config("Constructing input gates");
				constructInputGates();

				logger.config("Constructing mult gates");
				constructMultGates();

				logger.config("Constructing affine gates");
				constructAffineGates();

				logger.config("Constructing output gates");
				constructOutputGates();

				logger.config("PreProc Done");
				return;
			} catch (OpenContributionException ex) {
				logger.config("Open contribution exception from pKing " + ex.getPKing());
				runDetectDispute();
			}
		}
	}

	private void runDetectDispute() throws IOException, GeneralSecurityException, ClassNotFoundException {
		DetectDispute detect = new DetectDispute(channel, threshold, activity, rCount + iCount + 3*mCount, disputes, corruption, disputeMsg);
		detect.run();
		
		// Clear old share log
		disputeMsg = new DisputeMessage(channel.localParticipant());
	}

	private void constructRandomPairs() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException {
		if (randomPairs == null) {
			RobustDoubleRandom protRandom = new RobustDoubleRandom(random, channel, threshold, activity.subActivity("RandomPairs"), rCount + iCount + 3*mCount, fieldF, fieldG, disputes, corruption, disputeMsg);
			randomPairs = protRandom.run();
		}

		randomInputPairs = new RandomPair[iCount];
		randomRandomPairs = new RandomPair[rCount];
		randomMultPairs = new RandomPair[3*mCount];

		for (int i = 0; i < randomPairs.length; i++) {
			if (randomPairs[i] == null)
				throw new IllegalStateException("Index " + i + " is null.");
		}

		int offset = 0;
		System.arraycopy(randomPairs, offset, randomInputPairs, 0, iCount);
		offset += iCount;
		System.arraycopy(randomPairs, offset, randomRandomPairs, 0, rCount);
		offset += rCount;
		System.arraycopy(randomPairs, offset, randomMultPairs, 0, 3*mCount);
		offset += 3*mCount;		
	}

	private void constructRandomGates() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException {
		// Construct random gates
		for (int i = 0; i < rCount; i++) {
			result.setRandomGate(new RandomGate(i, randomRandomPairs[i].getR1()));
		}
	}

	private void constructInputGates() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException {
		// Construct input gates
		Activity inputActivity = activity.subActivity("Input");

		for (int i = 0; i < inputProviders.length; i++) {
			Activity shareInputActivity = inputActivity.subActivity(inputProviders[i].getID() + "," + i);

			if (channel.localParticipant().equals(inputProviders[i])) {
				Collection<IChannelData> chDatas = channel.receiveFromEachParticipant(shareInputActivity);

				Map<FieldElement, FieldElement> shares = new HashMap<FieldElement, FieldElement>();				
				for (IChannelData chData : chDatas) {
					FieldElement r1 = (FieldElement)chData.getObject();
					shares.put(fieldF.element(chData.getSender().getID()), r1);
				}

				FieldPolynomial1D poly;

				poly = Lagrange.interpolate(shares);

				// Check if poly is consistent, if not run berlekamp welch
				if (poly.degree() > threshold)
					poly = BerlekampWelch.interpolate(shares, threshold);

				result.setInputGate(new LocalInputGate(i, inputProviders[i], randomInputPairs[i].getR1(), poly.coefficient(0)));
			} else {
				channel.send(inputProviders[i], shareInputActivity, randomInputPairs[i].getR1());
				result.setInputGate(new RemoteInputGate(i, inputProviders[i], randomInputPairs[i].getR1()));
			}
		}
	}

	private void constructMultGates() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException {
		Triples protTriples = new Triples(channel, threshold, activity.subActivity("MultGates"), mCount, randomMultPairs, fieldF);

		Triple[] trips = protTriples.run();

		for (int i = 0; i < trips.length; i++) {
			result.setMultGate(new MultGate(i, trips[i].getA(), trips[i].getB(), trips[i].getC()));
		}
	}

	private void constructAffineGates() {
		for (int i = 0; i < result.getAffineGateCount(); i++) {
			result.setAffineGate(new AffineGate(i));
		}
	}

	private void constructOutputGates() {
		for (int i = 0; i < result.getOutputGateCount(); i++) {
			result.setOutputGate(new OutputGate(i));
		}
	}

	public PreProcessResult getResult() {
		return result;
	}
}
