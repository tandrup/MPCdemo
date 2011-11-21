package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.Lagrange;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.threading.BoundedSharedThreadPool;

public class Flip extends AbstractRandomProtocol {
	private Field field;
	
	private SortedSet<VssThread> runners;
	private SortedSet<ReconstructorThread> reconstructors;
	
	// Use 2 threads for executing VSS, use a linked blocking queue, to ensure consistent scheduling accross the network
	private final ExecutorService pool = new BoundedSharedThreadPool("Flip", 4);
	
	public Flip(Field field, Random random, CommunicationChannel channel, int threshold, Activity activity) {
		super(random, channel, threshold, activity);
		this.field = field;
	}
	
	public synchronized FieldElement run() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException {
		// Select random contribution
		FieldElement xi = field.element(random);

		// Create all needed Vss instances
		Collection<Vss> vssMachines = new ArrayList<Vss>();
		
		Collection<? extends Participant> parts = channel.listConnectedParticipants();
		for (Participant dealer : parts) {
			Activity vssActivity = activity.subActivity("VSS: " + dealer.getID());
			Vss vss = new Vss(vssActivity, random, channel, threshold, field, dealer, parts);
			vssMachines.add(vss);
		}

		// Create collections for the other threads
		runners = new TreeSet<VssThread>();
		reconstructors = new TreeSet<ReconstructorThread>();
		
		// Create runner threads
		for (Vss vss : vssMachines) {
			if (vss.getDealer().equals(channel.localParticipant()))
				runners.add(new DealerThread(vss, xi));
			else
				runners.add(new ReceiverThread(vss));
		}

		// Start runner threads
		for (VssThread runner : runners) {
			runner.future = pool.submit(runner);
			checkLastException();
		}

		// Wait for receivers to complete
		for (VssThread receiver : runners) {
			receiver.future.get();
		}
		checkLastException();

		// All receivers has completed, 
		
		// Create and start reconstruction threads
		for (VssThread receiver : runners) {
			ReconstructorThread reconstructor = new ReconstructorThread(receiver.vss.getDealer(), parts, receiver.si);
			reconstructor.future = pool.submit(reconstructor);
			reconstructors.add(reconstructor);
		}
		checkLastException();
		
		// Wait for reconstruction to complete and collect result
		FieldElement resultShare = field.zero();
		for (ReconstructorThread reconstructor : reconstructors) {
			reconstructor.future.get();
			checkLastException();
			
			// If we are the dealer we know the result to expect
			if (reconstructor.dealer.equals(channel.localParticipant())) {
				if (!xi.equals(reconstructor.result))
					throw new IllegalStateException("The reconstructed value did not match");
			}
			
			resultShare.add(reconstructor.result);
		}
		checkLastException();

		pool.shutdown();
		
		// Return result
		return resultShare;
	}
	
	private void checkLastException() {
		for (VssThread thread : runners) {
			if (thread.lastException != null) {
				throw new RuntimeException("Error occured during protocol execution", thread.lastException);
			}
		}

		for (ReconstructorThread thread : reconstructors) {
			if (thread.lastException != null) {
				throw new RuntimeException("Error occured during protocol execution", thread.lastException);
			}
		}
	}

	private abstract class VssThread implements Runnable, Comparable<VssThread> {
		protected Vss vss;

		public VssThread(Vss vss) {
			this.vss = vss;
		}

		FieldElement si;
		Exception lastException = null;
		Future<?> future;

		public int compareTo(VssThread other) {
			return this.vss.getDealer().compareTo(other.vss.getDealer());
		}
	}
	
	private class DealerThread extends VssThread {
		private FieldElement xi;

		public DealerThread(Vss vss, FieldElement xi) {
			super(vss);
			//super(channel.localParticipant().getID() + ": Flip Dealer " + vss.getDealer());
			this.xi = xi;
		}

		public void run() {
			try {
				si = vss.share(xi);
			} catch (Exception e) {
				lastException = e;
				e.printStackTrace();
			}
		}
	}

	private class ReceiverThread extends VssThread {
		public ReceiverThread(Vss vss) {
			//super(channel.localParticipant().getID() + ": Flip Receiver VSS from dealer " + vss.getDealer().getID());
			super(vss);
		}

		public void run() {
			try {
				// Receive share from dealer
				si = vss.receive();
			} catch (Exception e) {
				lastException = e;
				e.printStackTrace();
			}
		}
	}

	private class ReconstructorThread implements Runnable, Comparable<ReconstructorThread> {
		private Participant dealer;
		private Collection<? extends Participant> parts;
		private FieldElement si;
		
		FieldElement result;
		Exception lastException = null;
		Future<?> future;

		public ReconstructorThread(Participant dealer, Collection<? extends Participant> parts, FieldElement si) {
//			super(channel.localParticipant().getID() + ": Flip Reconstruction from dealer " + dealer.getID());
			this.dealer = dealer;
			this.parts = parts;
			this.si = si;
		}

		public void run() {
			try {
				// Reconstruct shares
				Activity reconstructActivity = activity.subActivity("Reconstruct" + dealer.getID());
				channel.broadcast(reconstructActivity, si);
				
				FieldElement[] x = new FieldElement[parts.size()];
				FieldElement[] y = new FieldElement[parts.size()];
				
				// For each participant receive share and add to array
				int i = 0;
				for (Participant participant : parts) {
					if (channel.localParticipant().equals(participant)) {
						x[i] = field.element(participant.getID());
						y[i] = si;
					} else {
						IChannelData chData = channel.receive(reconstructActivity, participant);
						y[i] = (FieldElement)chData.getObject();
						
						x[i] = field.element(participant.getID());
					}
					i++;
				}
				
				// Do lagrange interpolation
				FieldPolynomial1D poly = Lagrange.interpolate(x, y);
				
				result = poly.eval(field.zero());

			} catch (Exception e) {
				lastException = e;
				e.printStackTrace();
			}
		}

		public int compareTo(ReconstructorThread other) {
			return this.dealer.compareTo(other.dealer);
		}
	}
}
