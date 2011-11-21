package dk.au.daimi.tandrup.MPC.protocols;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.Lagrange;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.LongField;
import dk.au.daimi.tandrup.MPC.math.fields.polynomial.PolynomialField;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.ChannelProvider;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public class PreProcessTest {
	private ChannelProvider channelProvider;
	private Field fieldF, fieldG;

	@Before
	public void setUp() throws Exception {
		fieldF = new LongField(67);
		FieldPolynomial1D basePoly = new FieldPolynomial1D(new FieldElement[] {fieldF.element(6), fieldF.element(1), fieldF.element(0), fieldF.element(1)});
		fieldG = new PolynomialField(basePoly);
	}

	@After
	public void tearDown() throws Exception {
		if (channelProvider != null)
			channelProvider.close();
	}

	class ReceiveThread extends Thread {
		public boolean failed = false;
		public PreProcess preProcess;
		public Disputes disputes;
		
		public ReceiveThread(PreProcess preProcess, Disputes disputes) {
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

	@Test(timeout=500000)
	public void testRun() throws Exception {
		Activity testActivity = new Activity("PreProcess");

		int n = 5;
		int t = 1;
		//int l = n - (2*t + 1);
		
		channelProvider = ChannelProvider.getDefaultInstance(n);
		Participant[] parts = channelProvider.getParticipants();
		CommunicationChannel[] channels = channelProvider.getChannels();

		int rCount = 3;
		int mCount = 3;
		int aCount = 3;
		int oCount = 3;
		Participant[] inputProviders = new Participant[3];
		inputProviders[0] = parts[1];
		inputProviders[1] = parts[2];
		inputProviders[2] = parts[1];
		
		ReceiveThread[] receivers = new ReceiveThread[n];
		for (int i = 0; i < parts.length; i++) {
			Disputes disputes = new Disputes();
			PreProcess preProcess = new PreProcess(new Random(), channels[i], t, testActivity, rCount, inputProviders, mCount, aCount, oCount, fieldF, fieldG, disputes);
			receivers[i] = new ReceiveThread(preProcess, disputes);
			receivers[i].start();
		}

		for (int i = 0; i < receivers.length; i++) {
			ReceiveThread thread = receivers[i];

			thread.join();
			assertFalse(thread.failed);
			assertEquals(0, thread.disputes.size());
		}

	
		// Verify random gates
		for (int i = 0; i < rCount; i++) {
			FieldElement[] shares = new FieldElement[receivers.length];
			FieldElement[] shareIndexes = new FieldElement[receivers.length];
			for (int j = 0; j < receivers.length; j++) {
				ReceiveThread thread = receivers[j];
				
				shares[j] = thread.preProcess.getResult().getRandomGate(i).output();
				shareIndexes[j] = fieldF.element(j+1);
			}
			
			FieldPolynomial1D poly = Lagrange.interpolate(shareIndexes, shares);
			assertTrue("Degree too big: " + poly, poly.degree() <= t);
		}

		// Verify mult gates
		for (int i = 0; i < mCount; i++) {
			FieldElement[] shares = new FieldElement[receivers.length];
			FieldElement[] shareIndexes = new FieldElement[receivers.length];

			for (int j = 0; j < receivers.length; j++) {
				ReceiveThread thread = receivers[j];
				
				shares[j] = thread.preProcess.getResult().getMultGate(i).getAShare();
				shareIndexes[j] = fieldF.element(j+1);
			}
			FieldPolynomial1D polyA = Lagrange.interpolate(shareIndexes, shares);
			assertTrue("Degree too big: " + polyA, polyA.degree() <= t);

			for (int j = 0; j < receivers.length; j++) {
				ReceiveThread thread = receivers[j];
				
				shares[j] = thread.preProcess.getResult().getMultGate(i).getBShare();
				shareIndexes[j] = fieldF.element(j+1);
			}
			FieldPolynomial1D polyB = Lagrange.interpolate(shareIndexes, shares);
			assertTrue("Degree too big: " + polyB, polyB.degree() <= t);

			for (int j = 0; j < receivers.length; j++) {
				ReceiveThread thread = receivers[j];
				
				shares[j] = thread.preProcess.getResult().getMultGate(i).getCShare();
				shareIndexes[j] = fieldF.element(j+1);
			}
			FieldPolynomial1D polyC = Lagrange.interpolate(shareIndexes, shares);
			assertTrue("Degree too big: " + polyC, polyC.degree() <= t);
			
			FieldElement a = polyA.coefficient(0);
			FieldElement b = polyB.coefficient(0);
			FieldElement c = polyC.coefficient(0);
			System.out.println("a" + i + " = " + a);
			System.out.println("b" + i + " = " + b);
			System.out.println("c" + i + " = " + c);
			System.out.println("a" + i + "*b" + i + " = " + a.multiply(b));
			assertEquals(c, a.multiply(b));
		}

	}
}
