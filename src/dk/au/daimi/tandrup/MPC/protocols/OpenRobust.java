package dk.au.daimi.tandrup.MPC.protocols;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import dk.au.daimi.tandrup.MPC.math.BerlekampWelch;
import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.VanDerMondeMatrix;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.threading.BoundedSharedThreadPool;

public class OpenRobust extends AbstractProtocol {
	private final int d;
	private final FieldElement[] shares;
	
	// Use 2 threads 
	private final ExecutorService pool = new BoundedSharedThreadPool("OpenRobust ", 3);

	public OpenRobust(CommunicationChannel channel, int threshold, Activity activity, int d, FieldElement[] shares) {
		super(channel, threshold, activity);
		this.d = d;
		this.shares = shares;
	}
	
	private class OpenThread implements Runnable {
		private Open open;
		private FieldElement result;
		
		public Exception lastException = null;

		public OpenThread(String msg, Open open) {
			//super(msg);
			this.open = open;
		}
		
		public FieldElement getResult() {
			return result;
		}
		
		public void run() {
			try {
				result = open.run();
			} catch (Exception e) {
				lastException = e;
			}
		}
	}

	public FieldElement[] run() throws InterruptedException, ExecutionException {
		SortedSet<Participant> participants = new TreeSet<Participant>(channel.listConnectedParticipants());
		Field field = shares[0].field();

		// Expand shares into more shares
		VanDerMondeMatrix m = new VanDerMondeMatrix(field, participants.size(), shares.length);		
		FieldElement[] ys = m.multiply(shares);
				
		OpenThread[] openThreads = new OpenThread[participants.size()];
		Future<?>[] openThreadFutures = new Future<?>[participants.size()];
		for (Participant participant : participants) {
			Activity openActivity = activity.subActivity("Open " + participant.getID());
			Open open = new Open(channel, threshold, openActivity, participant, d, ys[participant.getID()-1]);
			
			OpenThread openThread = new OpenThread("Open " + participant, open);
			openThreads[participant.getID()-1] = openThread;
			openThreadFutures[participant.getID()-1] = pool.submit(openThread);
		}
		checkLastException(openThreads);

		// Wait for openings to complete
		for (int i = 0; i < openThreadFutures.length; i++) {
			openThreadFutures[i].get();
		}
		checkLastException(openThreads);
		
		for (int i = 0; i < ys.length; i++) {
			ys[i] = openThreads[i].getResult();
		}
		
		FieldPolynomial1D poly = BerlekampWelch.interpolate(ys, shares.length);
		checkLastException(openThreads);

		pool.shutdown();
		
		if (poly.coefficients().length == shares.length) {
			return poly.coefficients();
		} else {
			FieldElement[] xs = new FieldElement[shares.length];
			for (int i = 0; i < xs.length; i++) {
				xs[i] = poly.coefficient(i);
			}
			return xs;
		}
	}
	
	private void checkLastException(OpenThread[] openThreads) {
		for (OpenThread thread : openThreads) {
			if (thread != null && thread.lastException != null) {
				throw new RuntimeException("Error occured during protocol execution", thread.lastException);
			}
		}
	}
}
