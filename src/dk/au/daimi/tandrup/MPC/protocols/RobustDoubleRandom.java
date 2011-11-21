package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import dk.au.daimi.tandrup.MPC.math.VanDerMondeMatrix;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.messages.DisputeMessage;
import dk.au.daimi.tandrup.MPC.threading.BoundedSharedThreadPool;

public class RobustDoubleRandom extends AbstractRandomProtocol {
	private int l;
	private Field fieldF, fieldG;
	private Disputes disputes;
	private Corruption corruption;
	private DisputeMessage disputeMsg;

	Collection<? extends Participant> participants;

	// Thread pool for executing IC sharings
	private final ExecutorService pool = new BoundedSharedThreadPool("RobustDoubleRandom", 2);

	public RobustDoubleRandom(Random random, CommunicationChannel channel, int threshold, Activity activity, int l, Field F, Field G, Disputes disputes, Corruption corruption, DisputeMessage disputeMsg) {
		super(random, channel, threshold, activity);
		this.l = l;
		this.fieldF = F;
		this.fieldG = G;
		this.disputes = disputes;
		this.corruption = corruption;
		this.disputeMsg = disputeMsg;
	}

	public RandomPair[] run() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException {
		participants = channel.listConnectedParticipants();

		Participant me = channel.localParticipant();

		int n = participants.size();
		int shareCount = (int)Math.ceil((double)l / (n - threshold));

		class ICSThread implements Callable<FieldElement[][]>, Comparable<ICSThread> {
			private InterConsistentSharing sharing;
			Participant dealer;
			int shareCount;
			private FieldElement[] secrets;

			Future<FieldElement[][]> future;

			public ICSThread(InterConsistentSharing sharing, Participant dealer, int shareCount, FieldElement[] secrets) {
				//super(channel.localParticipant().getID() + ": RobustDoubleRandom ICSharing Dealer " + dealer.getID());
				this.sharing = sharing;
				this.dealer = dealer;
				this.shareCount = shareCount;
				this.secrets = secrets;
			}

			public FieldElement[][] call() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException {
				FieldElement[][] result;
				if (secrets != null)
					result = sharing.share(secrets);
				else
					result = sharing.receive(shareCount);
				sharing = null;
				secrets = null;
				return result;
			}

			public int compareTo(ICSThread other) {
				return this.dealer.compareTo(other.dealer);
			}
		}

		SortedSet<ICSThread> threads = new TreeSet<ICSThread>();

		FieldElement[] secrets = new FieldElement[shareCount];
		for (int i = 0; i < secrets.length; i++) {
			secrets[i] = fieldF.element(random);
		}

		Activity myActivity = activity.subActivity("ICSharing " + me.getID());
		InterConsistentSharing mySharing = new InterConsistentSharing(random, channel, threshold, myActivity, me, disputes, corruption, disputeMsg, fieldF, fieldG);
		threads.add(new ICSThread(mySharing, me, shareCount, secrets));

		for (Participant participant : participants) {
			if (me.equals(participant))
				continue;

			Activity partActivity = activity.subActivity("ICSharing " + participant.getID());
			InterConsistentSharing partSharing = new InterConsistentSharing(random, channel, threshold, partActivity, participant, disputes, corruption, disputeMsg, fieldF, fieldG);
			ICSThread thread = new ICSThread(partSharing, participant, shareCount, null);
			threads.add(thread);
		}

		// Submit all IC sharings to the pool for execution
		for (ICSThread thread : threads) {
			thread.future = pool.submit(thread);
		}

		VanDerMondeMatrix m = new VanDerMondeMatrix(fieldF, n, n - threshold).transpose();

		FieldElement[][] ris1 = new FieldElement[shareCount][];
		FieldElement[][] ris2 = new FieldElement[shareCount][];

		ArrayList<RandomPair> result = new ArrayList<RandomPair>();

		// calculate
		for (int shareIndex = 0; shareIndex < shareCount; shareIndex++) {
			FieldElement[] ys1 = new FieldElement[n];
			FieldElement[] ys2 = new FieldElement[n];
			for (ICSThread thread : threads) {
				int id = thread.dealer.getID();
				try {
					FieldElement[][] threadResult = thread.future.get();
					ys1[id-1] = threadResult[0][shareIndex];
					ys2[id-1] = threadResult[1][shareIndex];
				} catch (ExecutionException e) {
					throw new RuntimeException("Error occured during ICS protocol execution for dealer " + id, e);
				}
			}

			ris1[shareIndex] = m.multiply(ys1);
			ris2[shareIndex] = m.multiply(ys2);

			for (int i = 0; i < n - threshold; i++) {
				result.add(new RandomPair(ris1[shareIndex][i], ris2[shareIndex][i]));
			}
		}
		
		pool.shutdown();

		return result.toArray(new RandomPair[0]);
	}
}
