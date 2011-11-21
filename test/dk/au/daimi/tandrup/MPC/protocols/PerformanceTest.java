package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.LongField;
import dk.au.daimi.tandrup.MPC.math.fields.polynomial.PolynomialField;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.ChannelProvider;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.gates.AbstractGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.InputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.LocalInputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.MultGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.OutputGate;
import dk.au.daimi.tandrup.MPC.threading.BoundedSharedThreadPool;

public class PerformanceTest {
	private ChannelProvider channelProvider;
	private Field fieldF, fieldG;
	FieldElement two, three, four;
	Participant[] parts;

	private boolean requireUserInput = false;
	private boolean testDoubleRandom = true;
	private boolean testInput = true;
	private boolean testMult = true;
	private boolean testOutput = true;
	public void requireUserInput(boolean requireUserInput) {
		this.requireUserInput = requireUserInput;
	}
	public void testDoubleRandom(boolean testDoubleRandom) {
		this.testDoubleRandom = testDoubleRandom;
	}
	public void testInput(boolean testInput) {
		this.testInput = testInput;
	}
	public void testMultiplication(boolean testMult) {
		this.testMult = testMult;
	}
	public void testOutput(boolean testOutput) {
		this.testOutput = testOutput;
	}

	private static final ExecutorService execpool = Executors.newCachedThreadPool();

	class PreProcessThread implements Runnable {
		public boolean failed = false;
		public PreProcess preProcess;

		public PreProcessThread(int id, PreProcess preProcess) {
			//super("PreProcessThread " + (id+1));
			this.preProcess = preProcess;
		}

		public void run() {
			try {
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}

				preProcess.run();
			} catch (Exception e) {
				//System.err.print(this.getName() + ": ");
				e.printStackTrace();
				this.failed = true; 
			}
		}
	}

	class EvaluateThread implements Runnable {
		private String mode;
		public boolean failed = false;
		public int id;
		public PreProcessResult preProcess;
		public Evaluate evaluate;

		public EvaluateThread(int id, PreProcessResult preProcess, Evaluate evaluate, String mode) {
			//super("EvaluateThread " + mode + " " + (id+1));
			this.id = id;
			this.preProcess = preProcess;
			this.evaluate = evaluate;
			this.mode = mode;
		}

		public void run() {
			try {
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}

				if (mode.equals("input")) {

					// Provide inputs

					InputGate[] gates = new InputGate[preProcess.getInputGateCount()];
					for (int i = 0; i < gates.length; i++) {
						gates[i] = preProcess.getInputGate(i);

						if (gates[i] instanceof LocalInputGate)
							((LocalInputGate)gates[i]).setInput(fieldF.element(new Random()));
					}
					evaluate.evaluate(gates);

				} else if (mode.equals("mult")) {

					// Do multiplications

					MultGate[] gates = new MultGate[preProcess.getMultGateCount()];
					for (int i = 0; i < preProcess.getMultGateCount(); i++) {
						gates[i] = preProcess.getMultGate(i);
						AbstractGate g1 = preProcess.getRandomGate(0);
						AbstractGate g2;
						if (i < preProcess.getRandomGateCount())
							g2 = preProcess.getRandomGate(i);
						else
							g2 = preProcess.getRandomGate(preProcess.getRandomGateCount()-1);

						gates[i].setInputGates(g1, g2);
					}
					evaluate.evaluate(gates);

				} else if (mode.equals("output")) {

					// Do openings

					OutputGate[] gates = new OutputGate[preProcess.getOutputGateCount()];
					for (int i = 0; i < preProcess.getOutputGateCount(); i++) {
						gates[i] = preProcess.getOutputGate(i);
						gates[i].setInput(preProcess.getRandomGate(i));
					}
					evaluate.evaluate(gates);

				} else {
					throw new RuntimeException("UNKNOWN MODE: " + mode);
				}

			} catch (Exception e) {
				//System.err.print(this.getName() + ": ");
				e.printStackTrace();
				this.failed = true; 
			}
		}
	}


	public void setUp() throws Exception {
		fieldF = new LongField(67);
		FieldPolynomial1D basePoly = new FieldPolynomial1D(new FieldElement[] {fieldF.element(6), fieldF.element(1), fieldF.element(1)});
		//fieldF = new LongField(11);
		//FieldPolynomial1D basePoly = new FieldPolynomial1D(new FieldElement[] {fieldF.element(7), fieldF.element(1), fieldF.element(1)});
		fieldG = new PolynomialField(basePoly);

		two = fieldF.element(2);
		three = fieldF.element(3);
		four = fieldF.element(4);

		System.gc();
	}

	public void runStuff(int n, int t, int iCount, int rCount, int mCount, int oCount) throws InterruptedException, IOException, GeneralSecurityException, ClassNotFoundException, ExecutionException {
		Activity testActivity = new Activity("Test");

		//int l = n - (2*t + 1);

		channelProvider = ChannelProvider.getDefaultInstance(n);
		parts = channelProvider.getParticipants();
		CommunicationChannel[] channels = channelProvider.getChannels();

		int aCount = 3;

		Participant[] inputProviders = new Participant[iCount];
		if (iCount > 0)
			inputProviders[0] = parts[1];
		if (iCount > 1)
			inputProviders[1] = parts[2];
		if (iCount > 2)
			inputProviders[2] = parts[1];

		// Generate random pairs
		int pCount = -1;
		FieldPolynomial1D[] polys1t = null, polys2t = null;
		if (!testDoubleRandom) {
			Random random = new Random();
			pCount = rCount + 3*mCount + iCount;
			polys1t = new FieldPolynomial1D[pCount];
			polys2t = new FieldPolynomial1D[pCount];
			for (int i = 0; i < pCount; i++) {
				FieldElement secret = fieldF.element(random);
				polys1t[i] = new FieldPolynomial1D(secret, t, random);
				polys2t[i] = new FieldPolynomial1D(secret, 2*t, random);
			}
		}

		// Create preprocess threads
		PreProcessThread[] preProcessors = new PreProcessThread[n];
		for (int i = 0; i < parts.length; i++) {
			Disputes disputes = new Disputes();
			PreProcess preProcess;

			if (!testDoubleRandom) {
				// Generate random pairs
				RandomPair[] randomPairs = new RandomPair[pCount];
				for (int j = 0; j < randomPairs.length; j++) {
					FieldElement x = fieldF.element(i+1);
					FieldElement r1 = polys1t[j].eval(x);
					FieldElement r2 = polys2t[j].eval(x);
					randomPairs[j] = new RandomPair(r1, r2);
				}

				preProcess = new PreProcess(new Random(), channels[i], t, testActivity.subActivity("PreProc"), rCount, inputProviders, mCount, aCount, oCount, fieldF, fieldG, disputes, randomPairs);
			} else {
				preProcess = new PreProcess(new Random(), channels[i], t, testActivity.subActivity("PreProc"), rCount, inputProviders, mCount, aCount, oCount, fieldF, fieldG, disputes);
			}

			preProcessors[i] = new PreProcessThread(i, preProcess);
		}

		if (requireUserInput) {
			System.out.println("Pres enter to start preprocessing");
			System.in.read();
		}

		// Test preprocessing 
		benchmarkPhase("PreProc", preProcessors, n, t, iCount, rCount, mCount, oCount);

		if (requireUserInput) {
			System.out.println("Preprocessing done");
			System.in.read();
		}

		// Create eval threads
		EvaluateThread[] inputEvals = new EvaluateThread[n];
		EvaluateThread[] multEvals = new EvaluateThread[n];
		EvaluateThread[] affineEvals = new EvaluateThread[n];
		EvaluateThread[] outputEvals = new EvaluateThread[n];
		for (int i = 0; i < parts.length; i++) {
			Evaluate evaluate = new Evaluate(channels[i], t, testActivity.subActivity("Eval"));
			inputEvals[i] = new EvaluateThread(i, preProcessors[i].preProcess.getResult(), evaluate, "input");
			multEvals[i] = new EvaluateThread(i, preProcessors[i].preProcess.getResult(), evaluate, "mult");
			affineEvals[i] = new EvaluateThread(i, preProcessors[i].preProcess.getResult(), evaluate, "affine");
			outputEvals[i] = new EvaluateThread(i, preProcessors[i].preProcess.getResult(), evaluate, "output");
		}

		// Test input
		if (testInput)
			benchmarkPhase("Input", inputEvals, n, t, iCount, rCount, mCount, oCount);

		if (requireUserInput) {
			System.out.println("Pres enter to start EvalMult");
			System.in.read();
		}

		// Test multiplication
		if (testMult)
			benchmarkPhase("Mult", multEvals, n, t, iCount, rCount, mCount, oCount);

		if (requireUserInput) {
			System.out.println("EvalMult done");
			System.in.read();
		}

		// Test affine
		//benchmarkPhase("Affine", affineEvals, n, t, iCount, rCount, mCount, oCount);

		// Test output
		if (testOutput)
			benchmarkPhase("Output", outputEvals, n, t, iCount, rCount, mCount, oCount);

		channelProvider.close();

		Thread.sleep(1000);
	}

	private void benchmarkPhase(String name, Runnable[] threads, int n, int t, int iCount, int rCount, int mCount, int oCount) throws InterruptedException, ExecutionException {
		long startTime, stopTime;

		Future<?>[] results = new Future<?>[threads.length];

		startTime = System.currentTimeMillis();
		// Start threads
		for (int i = 0; i < threads.length; i++) {
			results[i] = execpool.submit(threads[i]);
		}

		// Wait for threads to complete
		for (int i = 0; i < results.length; i++) {
			results[i].get();
		}
		stopTime = System.currentTimeMillis();

		// Print and Reset PreProcess Stat:
		System.out.println(name + "\t"  + n + "\t"  + t + "\t"  + iCount + "\t"  + rCount + "\t"  + mCount + "\t"  + oCount + "\t"  + channelProvider.getStatistics() + "\t" + (stopTime - startTime));
		channelProvider.resetStatistics();
	}

	public static void main(String[] args) throws Exception {
		// Setup logging
		Handler[] handlers = Logger.getLogger("").getHandlers();
		for ( int index = 0; index < handlers.length; index++ ) {
			handlers[index].setLevel( Level.WARNING );
		}
		//Logger.getLogger("dk.au.daimi.tandrup.MPC.net.ssl.SSLCommunicationChannel").setLevel(Level.FINEST);
		//Logger.getLogger("dk.au.daimi.tandrup.MPC.net.ssl").setLevel(Level.FINER);


//		LogManager.getLogManager().getLogger("").setLevel(Level.FINEST);

		System.out.println("Name\tn\tt\tiCount\trCount\tmCount\toCount\tMsg's\tBytes\ttime");

		String testMode = "";
		if (args.length > 0)
			testMode = args[0];

		if (testMode.equals("massiveMult")) {
			int iCount = 0;
			int oCount = 0;

			for (int n = 4; n <= 5; n++) {
				int t = n / 3;
				if (n / 3.0 == n / 3)
					t = n / 3 - 1;

				if (t < 1)
					throw new IllegalStateException("Bad threshold");

				int[] mCounts = new int[] {500, 400, 200, 100, 50};
				
				for (int i = 0; i < mCounts.length; i++) {
					int mCount = mCounts[i];
					PerformanceTest test = new PerformanceTest();
					test.testDoubleRandom(false);
					test.testInput(false);
					test.testMultiplication(true);
					test.testOutput(false);
					test.setUp();
					int rCount = mCount;
					test.runStuff(n, t, iCount, rCount, mCount, oCount);
				}
			}

		} else if (testMode.equals("networkEval")) {

			int iCount = 3;
			int rCount = 3;
			int mCount = 3;
			int oCount = 3;

			for (int n = 4; n <= 50; n++) {
				int t = n / 3;
				if (n / 3.0 == n / 3)
					t = n / 3 - 1;

				PerformanceTest test = new PerformanceTest();
				test.testDoubleRandom(false);
				test.setUp();
				test.runStuff(n, t, iCount, rCount, mCount, oCount);
			}

		} else if (testMode.equals("networkBoth")) {

			int iCount = 3;
			int rCount = 3;
			int mCount = 3;
			int oCount = 3;

			for (int n = 4; n <= 12; n++) {
				int t = n / 3;
				if (n / 3.0 == n / 3)
					t = n / 3 - 1;

				PerformanceTest test = new PerformanceTest();
				test.testInput(false);
				test.testMultiplication(false);
				test.testOutput(false);
				test.setUp();
				test.runStuff(n, t, iCount, rCount, mCount, oCount);
			}

		} else if (testMode.equals("shark")) {

			int n = 4;
			int t = n / 3;

			if (n / 3.0 == n / 3)
				t = n / 3 - 1;

			if (t < 1)
				throw new IllegalStateException("Bad threshold");

			int iCount = 0;
			int rCount = 1;
			int mCount = 5;
			int oCount = 0;

			PerformanceTest test = new PerformanceTest();
			test.requireUserInput(true);
			test.setUp();
			test.runStuff(n, t, iCount, rCount, mCount, oCount);
		}

		System.out.println("\nDone");

		execpool.shutdown();
		BoundedSharedThreadPool.shutdownSharedPool();
	}
}
