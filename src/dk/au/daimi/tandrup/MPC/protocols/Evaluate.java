package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.gates.AbstractGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.InputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.LocalInputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.MultGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.OutputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.RemoteInputGate;
import dk.au.daimi.tandrup.MPC.threading.BoundedSharedThreadPool;

public class Evaluate extends AbstractProtocol {
	private Activity evalActivity;
	private int evalNo = 1;

	// Use 2 threads 
	private final ExecutorService pool = new BoundedSharedThreadPool("Evaluate", 2);

	public Evaluate(CommunicationChannel channel, int threshold, Activity activity) {
		super(channel, threshold, activity);
	}

	private boolean evaluate(LocalInputGate gate) throws IOException, GeneralSecurityException, ClassNotFoundException {
		Activity inputActivity = evalActivity.subActivity("Input" + gate.getID());
		FieldElement delta = gate.calculateDelta();
		channel.broadcast(inputActivity, delta);

		checkInputDelta(gate, delta);
		gate.setDelta(delta);
		return true;
	}

	private boolean evaluate(RemoteInputGate gate) throws IOException, GeneralSecurityException, ClassNotFoundException {
		Activity inputActivity = evalActivity.subActivity("Input" + gate.getID());

		IChannelData chData = channel.receive(inputActivity, gate.getInputProvider());
		FieldElement delta = (FieldElement)chData.getObject();

		checkInputDelta(gate, delta);
		gate.setDelta(delta);
		return true;
	}

	private void checkInputDelta(InputGate g, FieldElement delta) throws IOException, GeneralSecurityException, ClassNotFoundException {
		Activity inputCheckActivity = evalActivity.subActivity("InputCheck" + g.getID());

		channel.broadcast(inputCheckActivity, delta);

		int faultyDeltas = 0;
		for (Participant participant : channel.listConnectedParticipants()) {
			if (participant.equals(g.getInputProvider()))
				continue;
			if (participant.equals(channel.localParticipant()))
				continue;

			IChannelData chData = channel.receive(inputCheckActivity, participant);
			FieldElement otherDelta = (FieldElement)chData.getObject();

			if (!delta.equals(otherDelta))
				faultyDeltas++;
		}

		if (faultyDeltas > threshold)
			throw new IllegalStateException("Input Provider: " + g.getInputProvider() + " is invalid");		
	}

	private boolean evaluate(MultGate gate, Map<AbstractGate, FieldElement[]> openings) {
		if (openings.containsKey(gate))
			return false;

		FieldElement alphaSh = gate.getAlphaShare();
		FieldElement betaSh = gate.getBetaShare();

		openings.put(gate, new FieldElement[] { alphaSh, betaSh });	
		return true;
	}

	private boolean evaluate(OutputGate gate, Map<AbstractGate, FieldElement[]> openings) {
		if (openings.containsKey(gate))
			return false;

		openings.put(gate, new FieldElement[] { gate.getInputShare() });
		return true;
	}

	private boolean evaluate(InputGate gate) throws IOException, GeneralSecurityException, ClassNotFoundException {
		if (gate instanceof LocalInputGate) {
			return evaluate((LocalInputGate)gate);			
		} else if (gate instanceof RemoteInputGate) {
			return evaluate((RemoteInputGate)gate);	
		}
		return false;
	}

	private boolean evaluate(AbstractGate gate, Map<AbstractGate, FieldElement[]> openings) throws IOException, GeneralSecurityException, ClassNotFoundException {
		if (gate instanceof InputGate) {
			return evaluate((InputGate)gate);
		} else if (gate instanceof MultGate) {
			return evaluate((MultGate)gate, openings);		
		} else if (gate instanceof OutputGate) {
			return evaluate((OutputGate)gate, openings);
		}
		return false;
	}

	private SortedSet<AbstractGate> buildWorkSet(Collection<? extends AbstractGate> gates) {
		SortedSet<AbstractGate> workSet = new TreeSet<AbstractGate>();
		buildWorkSet(workSet, gates);
		return workSet;
	}

	private void buildWorkSet(SortedSet<AbstractGate> workSet, Collection<? extends AbstractGate> gates) {
		Set<AbstractGate> ingressGates = new TreeSet<AbstractGate>();

		for (AbstractGate gate : gates) {
			// Check that the gate is configured
			if (!gate.isConfigured())
				throw new IllegalStateException("Gate unconfigured");

			// Don't look at computed gates
			if (gate.isComputed())
				continue;

			// Add the gate to the work set
			workSet.add(gate);

			// If the gate is not ready include all ingress gates
			if (!gate.isReady())
				ingressGates.addAll(gate.ingressGates());
		}

		// Build work set for all ingress gates
		if (!ingressGates.isEmpty())
			buildWorkSet(workSet, ingressGates);
	}

	private void evalutateInputGates(Collection<AbstractGate> workList) throws IOException, GeneralSecurityException, ClassNotFoundException {
		// Create set of input gates ordered by gate id
		SortedSet<InputGate> inputGates = new TreeSet<InputGate>(
				new Comparator<InputGate>() {
					public int compare(InputGate o1, InputGate o2) {
						if (o1.getID() == o2.getID())
							return 0;

						if (o1.getID() < o2.getID())
							return -1;

						return 1;
					}
				}
		);

		// Fill set
		for (AbstractGate gate : workList) {
			if (gate instanceof InputGate)
				inputGates.add((InputGate)gate);
		}

		// Evaluate gates
		for (InputGate gate : inputGates) {
			evaluate(gate);
			if (!gate.isComputed())
				throw new IllegalStateException("Input gate could not be computed");
			workList.remove(gate);
		}
	}

	public synchronized void evaluate(AbstractGate[] gates) throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException  {
		evaluate(Arrays.asList(gates));
	}

	public synchronized void evaluate(Collection<? extends AbstractGate> gates) throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException {
		this.evalActivity = this.activity.subActivity("No" + evalNo++);

		Set<AbstractGate> workSet = buildWorkSet(gates);

		// Evaluate input gates first to ensure that no output is provided before all inputs are given
		evalutateInputGates(workSet);

		int round = 1;
		// Evaluate gates
		while (!workSet.isEmpty()) {
			SortedMap<AbstractGate, FieldElement[]> openings = new TreeMap<AbstractGate, FieldElement[]>();

			// Find new openings
			for (AbstractGate gate : workSet) {
				if (gate.isReady() && !gate.isComputed()) {
					evaluate(gate, openings);
				}
			}

			// Perform openings
			performOpenings(openings, round);

			// Remove gates that are computed
			for (AbstractGate gate : new ArrayList<AbstractGate>(workSet)) {
				if (gate.isComputed())
					workSet.remove(gate);
			}

			round++;
		}
	}

	private void performOpenings(SortedMap<AbstractGate, FieldElement[]> openings, int round) throws InterruptedException, ExecutionException {
		if (openings.isEmpty())
			return;

		int participantCount = channel.listConnectedParticipants().size();
		int blockSize = participantCount - (2*threshold + 1);

		// Perform openings
		SortedMap<AbstractGate, List<Integer>> resultIndexs = new TreeMap<AbstractGate, List<Integer>>();
		List<FieldElement> openingShares = new ArrayList<FieldElement>();
		int index = 0;
		for (Entry<AbstractGate, FieldElement[]> opening : openings.entrySet()) {
			List<Integer> indexes = new ArrayList<Integer>();
			for (FieldElement val : opening.getValue()) {
				openingShares.add(index, val);
				indexes.add(index);
				index++;
			}
			resultIndexs.put(opening.getKey(), indexes);
		}

		List<FieldElement> openingResults = new ArrayList<FieldElement>();

		// Open each block
		Collection<OpenThread> openThreads = new ArrayList<OpenThread>();
		for (int block = 0; block <= openingShares.size() / blockSize; block++) {
			int fromIndex = block * blockSize;
			int toIndex = (block+1) * blockSize;
			if (toIndex > openingShares.size())
				toIndex = openingShares.size();

			if (fromIndex == toIndex)
				continue;

			OpenThread openThread = new OpenThread(round, block, fromIndex, toIndex, openingShares);
			openThread.future = pool.submit(openThread);
			openThreads.add(openThread);
		}

		// Wait for open protocols to complete and collect the results
		for (OpenThread openThread : openThreads) {
			openThread.future.get();

			if (openThread.lastException1 != null)
				throw openThread.lastException1;
			if (openThread.lastException2 != null)
				throw openThread.lastException2;
			if (openThread.lastException3 != null)
				throw openThread.lastException3;

			for (int i = 0; i < openThread.result.length; i++) {
				openingResults.add(i + openThread.fromIndex, openThread.result[i]);
			}
		}

		// Transfer result back to gate
		for (Entry<AbstractGate, List<Integer>> gateIndexes : resultIndexs.entrySet()) {
			List<Integer> indexes = gateIndexes.getValue();
			if (gateIndexes.getKey() instanceof MultGate) {
				MultGate gate = (MultGate)gateIndexes.getKey();
				gate.setAlpha(openingResults.get(indexes.get(0)));
				gate.setBeta(openingResults.get(indexes.get(1)));
			} else if (gateIndexes.getKey() instanceof OutputGate) {
				OutputGate gate = (OutputGate)gateIndexes.getKey();
				gate.setResult(openingResults.get(indexes.get(0)));
			} else {
				throw new IllegalStateException("Should not have been reached");
			}
		}
	}

	// Thread for opening each protocol
	private class OpenThread implements Runnable {
		int round;
		int block;
		int fromIndex, toIndex;
		List<FieldElement> openingShares;
		FieldElement[] result;
		ExecutionException lastException1;
		InterruptedException lastException2;
		RuntimeException lastException3;
		Future<?> future;

		public OpenThread(int round, int block, int fromIndex, int toIndex, List<FieldElement> openingShares) {
			//super("Evaluate.OpenThread " + round + "," + block);
			this.round = round;
			this.block = block;
			this.fromIndex = fromIndex;
			this.toIndex = toIndex;
			this.openingShares = openingShares;
		}

		public void run() {
			try {
				FieldElement[] shareBlock = openingShares.subList(fromIndex, toIndex).toArray(new FieldElement[0]);
				OpenRobust openProt = new OpenRobust(channel, threshold, evalActivity.subActivity("Open" + round + "," + block), threshold, shareBlock);
				result = openProt.run();
			} catch (ExecutionException e) {
				this.lastException1 = e;
			} catch (InterruptedException e) {
				this.lastException2 = e;
			} catch (RuntimeException e) {
				this.lastException3 = e;
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			pool.shutdown();
		} finally {
			super.finalize();
		}
	}
}
