package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Random;

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
import dk.au.daimi.tandrup.MPC.protocols.gates.AffineGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.InputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.LocalInputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.MultGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.OutputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.RandomGate;

public class NetworkSizePerformanceTest {
	private ChannelProvider channelProvider;
	private Field fieldF, fieldG;
	FieldElement two, three, four;
	Participant[] parts;

	class PreProcessThread extends Thread {
		public boolean failed = false;
		public PreProcess preProcess;

		public PreProcessThread(int id, PreProcess preProcess) {
			super("PreProcessThread " + (id+1));
			this.preProcess = preProcess;
		}
		
		public void run() {
			try {
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}
				
				preProcess.run();
			} catch (Exception e) {
				System.err.print(this.getName() + ": ");
				e.printStackTrace();
				this.failed = true; 
			}
		}
	}
	
	class EvaluateThread extends Thread {
		private String mode;
		public boolean failed = false;
		public int id;
		public PreProcessResult preProcess;
		public Evaluate evaluate;

		public EvaluateThread(int id, PreProcessResult preProcess, Evaluate evaluate, String mode) {
			super("EvaluateThread " + mode + " " + (id+1));
			this.id = id;
			this.preProcess = preProcess;
			this.evaluate = evaluate;
			this.mode = mode;
		}
		
		public void run() {
			try {
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}

				InputGate i1, i2, i3;
				MultGate m1, m2, m3;
				OutputGate o1, o2, o3;
				AffineGate a1, a2, a3;
				RandomGate r1, r2, r3;

				r1 = preProcess.getRandomGate(0);
				r2 = preProcess.getRandomGate(1);
				r3 = preProcess.getRandomGate(2);
				i1 = preProcess.getInputGate(0);
				i2 = preProcess.getInputGate(1);
				i3 = preProcess.getInputGate(2);
				m1 = preProcess.getMultGate(0);
				m2 = preProcess.getMultGate(1);
				m3 = preProcess.getMultGate(2);
				a1 = preProcess.getAffineGate(0);
				a2 = preProcess.getAffineGate(1);
				a3 = preProcess.getAffineGate(2);
				o1 = preProcess.getOutputGate(0);
				o2 = preProcess.getOutputGate(1);
				o3 = preProcess.getOutputGate(2);

				if (mode.equals("input")) {
				
					// Provide 3 inputs
					if (i1 instanceof LocalInputGate)
						((LocalInputGate)i1).setInput(two);
					if (i2 instanceof LocalInputGate)
						((LocalInputGate)i2).setInput(three);
					if (i3 instanceof LocalInputGate)
						((LocalInputGate)i3).setInput(three);
					evaluate.evaluate(new AbstractGate[] { i1, i2, i3 });

				} else if (mode.equals("mult")) {
				
					// Do 3 multiplications
					m1.setInputGates(i1, i2);
					m2.setInputGates(i2, i1);
					m3.setInputGates(i3, i1);
					evaluate.evaluate(new AbstractGate[] { m1, m2, m3 });
				
				} else if (mode.equals("affine")) {

					// Do 3 affine gates
					a1.configure(fieldF.element(5), new AbstractGate[] {m2,i1}, new FieldElement[] {two, three});
					a2.configure(fieldF.zero(), new AbstractGate[] {a1, r3}, new FieldElement[] {fieldF.one(), fieldF.one()});
					a3.configure(fieldF.element(3), new AbstractGate[] {a1, r2}, new FieldElement[] {fieldF.one(), fieldF.one()});
					evaluate.evaluate(new AbstractGate[] { a1, a2, a3 });
				
				} else if (mode.equals("output")) {

					// Do 3 openings
					o1.setInput(m1);
					o2.setInput(a2);
					o3.setInput(r1);
					evaluate.evaluate(new AbstractGate[] { o1, o2, o3 });
				
				} else {
					throw new RuntimeException("UNKNOWN MODE: " + mode);
				}
				
			} catch (Exception e) {
				System.err.print(this.getName() + ": ");
				e.printStackTrace();
				this.failed = true; 
			}
		}
	}
	
	public void setUp() throws Exception {
		fieldF = new LongField(11);
		FieldPolynomial1D basePoly = new FieldPolynomial1D(new FieldElement[] {fieldF.element(7), fieldF.element(1), fieldF.element(1)});
		fieldG = new PolynomialField(basePoly);
		
		two = fieldF.element(2);
		three = fieldF.element(3);
		four = fieldF.element(4);
	}

	public void runStuff(int n, int t) throws InterruptedException, IOException, GeneralSecurityException, ClassNotFoundException {
		Activity testActivity = new Activity("Test");

		//int l = n - (2*t + 1);
		
		channelProvider = ChannelProvider.getDefaultInstance(n);
		parts = channelProvider.getParticipants();
		CommunicationChannel[] channels = channelProvider.getChannels();

		int rCount = 3;
		int mCount = 3;
		int aCount = 3;
		int oCount = 3;
		int iCount = 3;
		Participant[] inputProviders = new Participant[iCount];
		inputProviders[0] = parts[1];
		inputProviders[1] = parts[2];
		inputProviders[2] = parts[1];

		// Generate random pairs
		Random random = new Random();
		int pCount = rCount + 3*mCount + iCount;
		FieldPolynomial1D[] polys1t = new FieldPolynomial1D[pCount];
		FieldPolynomial1D[] polys2t = new FieldPolynomial1D[pCount];
		for (int i = 0; i < pCount; i++) {
			FieldElement secret = fieldF.element(random);
			polys1t[i] = new FieldPolynomial1D(secret, t, random);
			polys2t[i] = new FieldPolynomial1D(secret, 2*t, random);
		}
		
		// Create preprocess threads
		PreProcessThread[] preProcessors = new PreProcessThread[n];
		for (int i = 0; i < parts.length; i++) {
			// Generate random pairs
			RandomPair[] randomPairs = new RandomPair[pCount];
			for (int j = 0; j < randomPairs.length; j++) {
				FieldElement x = fieldF.element(i+1);
				FieldElement r1 = polys1t[j].eval(x);
				FieldElement r2 = polys2t[j].eval(x);
				randomPairs[j] = new RandomPair(r1, r2);
			}

			Disputes disputes = new Disputes();
			PreProcess preProcess = new PreProcess(new Random(), channels[i], t, testActivity.subActivity("PreProc"), rCount, inputProviders, mCount, aCount, oCount, fieldF, fieldG, disputes, randomPairs);
			preProcessors[i] = new PreProcessThread(i, preProcess);
		}

		// Test preprocessing 
		benchmarkPhase("PreProc", preProcessors, n, t);
				
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
		benchmarkPhase("Input", inputEvals, n, t);

		// Test multiplication
		benchmarkPhase("Mult", multEvals, n, t);

		// Test affine
		benchmarkPhase("Affine", affineEvals, n, t);

		// Test multiplication
		benchmarkPhase("Output", outputEvals, n, t);

		channelProvider.close();
	}

	private void benchmarkPhase(String name, Thread[] threads, int n, int t) throws InterruptedException {
		long startTime, stopTime;
		
		startTime = System.currentTimeMillis();
		// Start threads
		for (int i = 0; i < threads.length; i++) {
			threads[i].start();
		}

		// Wait for threads to complete
		for (int i = 0; i < threads.length; i++) {
			threads[i].join();
		}
		stopTime = System.currentTimeMillis();

		// Print and Reset PreProcess Stat:
		System.out.println(name + "\t"  + n + "\t"  + t + "\t"  + channelProvider.getStatistics() + "\t" + (stopTime - startTime));
		channelProvider.resetStatistics();
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println("Name\tn\tt\tMsg's\tBytes\ttime");

		for (int n = 4; n <= 50; n++) {
			int minT = n / 3 - 3;
			if (minT < 1)
				minT = 1;
			for (int t = minT; t < n / 3.0; t++) {
				NetworkSizePerformanceTest test = new NetworkSizePerformanceTest();
				test.setUp();
				test.runStuff(n ,t);
			}
		}
	}
}
