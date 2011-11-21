package dk.au.daimi.tandrup.MPC.protocols;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.Lagrange;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.LongField;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.ChannelProvider;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.Triples.Triple;

public class TriplesTest {
	private ChannelProvider channelProvider;
	private Field fieldF;

	@Before
	public void setUp() throws Exception {
		fieldF = new LongField(11);
/*		FieldPolynomial1D basePoly = new FieldPolynomial1D(new FieldElement[] {fieldF.element(7), fieldF.element(1), fieldF.element(1)});
		fieldG = new PolynomialField(basePoly);*/
	}

	@After
	public void tearDown() throws Exception {
		if (channelProvider != null)
			channelProvider.close();
	}

	class ReceiveThread extends Thread {
		public boolean failed = false;
		private Triples triples;
		public Triple[] result;
		private Disputes disputes;
		
		public ReceiveThread(Triples triples, Disputes disputes) {
			this.triples = triples;
			this.disputes = disputes;
		}
		
		public void run() {
			try {
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}
				
				result = triples.run();
			} catch (Exception e) {
				System.err.print(this.getName() + ": ");
				e.printStackTrace();
				this.failed = true; 
			}
		}
	}

	@Test(timeout=160000)
	public void testRun() throws Exception {
		Activity testActivity = new Activity("Triples");

		int n = 5;
		int t = 1;
		int l = n - (2*t + 1);

		// Generate random pairs
		Random random = new Random();
		FieldPolynomial1D[] polys1t = new FieldPolynomial1D[3*l];
		FieldPolynomial1D[] polys2t = new FieldPolynomial1D[3*l];
		for (int i = 0; i < 3*l; i++) {
			FieldElement secret = fieldF.element(random);
			polys1t[i] = new FieldPolynomial1D(secret, t, random);
			polys2t[i] = new FieldPolynomial1D(secret, 2*t, random);
		}
		
		channelProvider = ChannelProvider.getDefaultInstance(n);
		Participant[] parts = channelProvider.getParticipants();
		CommunicationChannel[] channels = channelProvider.getChannels();

		ReceiveThread[] receivers = new ReceiveThread[n];
		for (int i = 0; i < parts.length; i++) {
			// Generate random pairs
			RandomPair[] randomPairs = new RandomPair[3*l];
			for (int j = 0; j < randomPairs.length; j++) {
				FieldElement x = fieldF.element(i+1);
				FieldElement r1 = polys1t[j].eval(x);
				FieldElement r2 = polys2t[j].eval(x);
				randomPairs[j] = new RandomPair(r1, r2);
			}
			
			Disputes disputes = new Disputes();
			Triples triples = new Triples(channels[i], t, testActivity, l, randomPairs, fieldF);
			receivers[i] = new ReceiveThread(triples, disputes);
			receivers[i].start();
		}

		FieldElement[] shareIndexs = new FieldElement[n];
		FieldElement[] aShares = new FieldElement[n];
		FieldElement[] bShares = new FieldElement[n];
		FieldElement[] cShares = new FieldElement[n];
		
		for (int i = 0; i < receivers.length; i++) {
			ReceiveThread thread = receivers[i];

			thread.join();
			assertFalse("Thread failed", thread.failed);
			assertEquals(0, thread.disputes.size());
			
			shareIndexs[i] = fieldF.element(i+1);
			aShares[i] = thread.result[0].getA();
			bShares[i] = thread.result[0].getB();
			cShares[i] = thread.result[0].getC();
		}
		
		FieldPolynomial1D aPoly = Lagrange.interpolate(shareIndexs, aShares);
		FieldPolynomial1D bPoly = Lagrange.interpolate(shareIndexs, bShares);
		FieldPolynomial1D cPoly = Lagrange.interpolate(shareIndexs, cShares);
		
		assertTrue(aPoly.degree() <= t);
		assertTrue(bPoly.degree() <= t);
		assertTrue(cPoly.degree() <= t);

		System.out.println("ids = " + Arrays.toString(shareIndexs));

		System.out.println("[a] = " + Arrays.toString(aShares));
		System.out.println("a: " + aPoly);
		System.out.println("[b] = " + Arrays.toString(bShares));
		System.out.println("b: " + bPoly);
		System.out.println("[c] = " + Arrays.toString(cShares));
		System.out.println("c: " + cPoly);
		
		FieldElement a = aPoly.eval(fieldF.zero());
		FieldElement b = bPoly.eval(fieldF.zero());
		FieldElement c = cPoly.eval(fieldF.zero());

		System.out.println("a*b = " + a.multiply(b));
		
		assertEquals(c, a.multiply(b));
	}
}
