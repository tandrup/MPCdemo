package dk.au.daimi.tandrup.MPC.protocols;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.GeneralSecurityException;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.LongField;
import dk.au.daimi.tandrup.MPC.math.fields.polynomial.PolynomialField;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.ChannelProvider;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.gates.*;

public class FullTest {
	private ChannelProvider channelProvider;
	private Field fieldF, fieldG;
	FieldElement two, three;
	Participant[] parts;

	@Before
	public void setUp() throws Exception {
		fieldF = new LongField(67);
		FieldPolynomial1D basePoly = new FieldPolynomial1D(new FieldElement[] {fieldF.element(6), fieldF.element(1), fieldF.element(0), fieldF.element(1)});
		fieldG = new PolynomialField(basePoly);

		two = fieldF.element(2);
		three = fieldF.element(3);
	}

	@After
	public void tearDown() throws Exception {
		if (channelProvider != null)
			channelProvider.close();
	}

	class PreProcessThread extends Thread {
		public boolean failed = false;
		public PreProcess preProcess;
		public Disputes disputes;

		public PreProcessThread(PreProcess preProcess, Disputes disputes) {
			this.preProcess = preProcess;
			this.disputes = disputes;
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
		public boolean failed = false;
		public int id;
		public PreProcess preProcess;
		public Evaluate evaluate;
		public Disputes disputes;
		public InputGate i1, i2;
		public MultGate m1, m2;
		public OutputGate o1, o2, o3;
		public AffineGate a1, a2;
		public RandomGate r1, r2;

		public EvaluateThread(int id, PreProcess preProcess, Evaluate evaluate, Disputes disputes) {
			this.id = id;
			this.preProcess = preProcess;
			this.evaluate = evaluate;
			this.disputes = disputes;
		}

		public void run() {
			try {
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}

				r1 = preProcess.getResult().getRandomGate(0);
				r2 = preProcess.getResult().getRandomGate(1);
				i1 = preProcess.getResult().getInputGate(0);
				i2 = preProcess.getResult().getInputGate(1);
				m1 = preProcess.getResult().getMultGate(0);
				m2 = preProcess.getResult().getMultGate(1);
				a1 = preProcess.getResult().getAffineGate(0);
				a2 = preProcess.getResult().getAffineGate(1);
				o1 = preProcess.getResult().getOutputGate(0);
				o2 = preProcess.getResult().getOutputGate(1);
				o3 = preProcess.getResult().getOutputGate(2);

				o1.setInput(m1);

				a1.configure(fieldF.element(5), new AbstractGate[] {m2,i1}, new FieldElement[] {two, three});

				a2.configure(fieldF.zero(), new AbstractGate[] {a1, i2}, new FieldElement[] {fieldF.one(), fieldF.one()});

				o2.setInput(a2);

				o3.setInput(r1);

				if (i1 instanceof LocalInputGate)
					((LocalInputGate)i1).setInput(two);
				if (i2 instanceof LocalInputGate)
					((LocalInputGate)i2).setInput(three);

				m1.setInputGates(i1, i2);
				m2.setInputGates(i1, i1);

				evaluate.evaluate(new AbstractGate[] { o1, o2, o3 });

			} catch (Exception e) {
				System.err.print(this.getName() + ": ");
				e.printStackTrace();
				this.failed = true; 
			}
		}
	}

	@Test(timeout=600000)
	public void testRun() throws InterruptedException, IOException, GeneralSecurityException, ClassNotFoundException {
		Activity testActivity = new Activity("Test");

		// Setup logging
		Handler[] handlers = Logger.getLogger("").getHandlers();
		for ( int index = 0; index < handlers.length; index++ ) {
			handlers[index].setLevel( Level.INFO );
		}
		LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

		int n = 5;
		int t = 1;
		//int l = n - (2*t + 1);

		channelProvider = ChannelProvider.getDefaultInstance(n);
		parts = channelProvider.getParticipants();
		CommunicationChannel[] channels = channelProvider.getChannels();

		int rCount = 3;
		int mCount = 3;
		int aCount = 3;
		int oCount = 3;
		Participant[] inputProviders = new Participant[3];
		inputProviders[0] = parts[1];
		inputProviders[1] = parts[2];
		inputProviders[2] = parts[1];

		// Create threads
		PreProcessThread[] preProcessors = new PreProcessThread[n];
		EvaluateThread[] evaluators = new EvaluateThread[n];
		for (int i = 0; i < parts.length; i++) {
			Disputes disputes = new Disputes();
			PreProcess preProcess = new PreProcess(new Random(), channels[i], t, testActivity.subActivity("Pre"), rCount, inputProviders, mCount, aCount, oCount, fieldF, fieldG, disputes);
			Evaluate evaluate = new Evaluate(channels[i], t, testActivity.subActivity("Eval"));
			preProcessors[i] = new PreProcessThread(preProcess, disputes);
			evaluators[i] = new EvaluateThread(i, preProcess, evaluate, disputes);
		}

		long startTime, stopTime;

		startTime = System.currentTimeMillis();
		// Start preprocessing 
		for (int i = 0; i < preProcessors.length; i++) {
			preProcessors[i].start();
		}

		// Wait for preprocessing to complete
		for (int i = 0; i < preProcessors.length; i++) {
			preProcessors[i].join();
			assertFalse("Thread failed", preProcessors[i].failed);
			assertEquals("No disputes", 0, preProcessors[i].disputes.size());
		}
		stopTime = System.currentTimeMillis();

		// Print and Reset PreProcess Stat:
		System.out.println("PreProcess: " + channelProvider.getStatistics());
		System.out.println("Time spend: " + (stopTime - startTime) + " ms");
		channelProvider.resetStatistics();

		// Save preprocess result
		for (int i = 0; i < preProcessors.length; i++) {
			String pathname = "preprocess" + i +".bin";
			File outFile = new File(pathname);

			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outFile));
			out.writeObject(preProcessors[i].preProcess.getResult());
			out.close();

			System.out.println("Data written to: " + outFile.getPath());
		}

		startTime = System.currentTimeMillis();
		// Start evaluation
		for (int i = 0; i < evaluators.length; i++) {
			evaluators[i].start();
		}

		// Wait for evaluation to complete
		for (int i = 0; i < evaluators.length; i++) {
			evaluators[i].join();
			assertFalse("Thread failed", evaluators[i].failed);
			assertEquals("No disputes", 0, evaluators[i].disputes.size());
		}
		stopTime = System.currentTimeMillis();

		// Print and Reset Eval Stat:
		System.out.println("Evaluate:" + channelProvider.getStatistics());
		System.out.println("Time spend: " + (stopTime - startTime) + " ms");

		/*
		for (int i = 0; i < preProcessors.length; i++) {
			ClientThread thread = preProcessors[i];

			System.out.println(i + ": " + thread.r1);
			System.out.println(i + ": " + thread.r2);
			System.out.println(i + ": " + thread.i1);
			System.out.println(i + ": " + thread.i2);
			System.out.println(i + ": " + thread.m1);
			System.out.println(i + ": " + thread.m2);
			System.out.println(i + ": " + thread.a1);
			System.out.println(i + ": " + thread.a2);
			System.out.println(i + ": " + thread.o1);
			System.out.println(i + ": " + thread.o2);
			System.out.println(i + ": " + thread.o3);
		}
		 */
		for (int i = 0; i < evaluators.length; i++) {
			EvaluateThread thread = evaluators[i];
			assertEquals(fieldF.element(6), thread.o1.output());
			assertEquals(fieldF.element(22), thread.o2.output());
			assertTrue("o3 is not computed", thread.o3.isComputed());
			assertNotNull("o3 result is null", thread.o3.output());
		}

		channelProvider.close();
	}
}
