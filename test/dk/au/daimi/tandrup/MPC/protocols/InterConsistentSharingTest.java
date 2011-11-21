package dk.au.daimi.tandrup.MPC.protocols;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.Lagrange;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.BigIntegerField;
import dk.au.daimi.tandrup.MPC.math.fields.polynomial.PolynomialField;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.ChannelProvider;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.messages.DisputeMessage;

public class InterConsistentSharingTest {
	private Field fieldF, fieldG;
	private ChannelProvider channelProvider;
	
	@Before
	public void setUp() throws Exception {
		fieldF = new BigIntegerField(BigInteger.valueOf(11));
		FieldPolynomial1D basePoly = new FieldPolynomial1D(new FieldElement[] {fieldF.element(7), fieldF.element(1), fieldF.element(1)});
		fieldG = new PolynomialField(basePoly);
	}

	@After
	public void tearDown() throws Exception {
		if (channelProvider != null)
			channelProvider.close();
	}
	
	class ReceiveThread extends Thread {
		public boolean failed = false;
		private InterConsistentSharing sharing;
		private int l;
		public FieldElement[][] result;
		public Disputes disputes;
		
		public ReceiveThread(InterConsistentSharing sharing, int l, Disputes disputes) {
			this.sharing = sharing;
			this.l = l;
			this.disputes = disputes;
		}
		
		public void run() {
			try {
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}
				
				result = sharing.receive(l);
			} catch (Exception e) {
				System.err.print(this.getName() + ": ");
				e.printStackTrace();
				this.failed = true; 
			}
		}
	}

	@Test(timeout=60000)
	public void testReceive() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException {
		Activity testActivity = new Activity("Sharing");

		int n = 5;
		int t = 1;
		int l = n - (2*t + 1);

		channelProvider = ChannelProvider.getDefaultInstance(n+1);

		Participant[] parts = new Participant[n];
		CommunicationChannel[] channels = new CommunicationChannel[n];
		for (int i = 0; i < parts.length; i++) {
			parts[i] = channelProvider.getParticipants()[i];
			channels[i] = channelProvider.getChannels()[i];
		}
		Participant dealer = channelProvider.getParticipants()[n];
		CommunicationChannel dealerChannel = channelProvider.getChannels()[n];

		FieldElement secretA = fieldF.element(42);
		FieldElement secretB = fieldF.element(73);
		FieldElement[] secrets = new FieldElement[] {secretA, secretB};
		assertEquals(l, secrets.length);
		System.out.println("Secrets: " + secrets);

		DisputeMessage dispMsg = new DisputeMessage(channels[0].localParticipant());

		ReceiveThread[] receivers = new ReceiveThread[n];
		for (int i = 0; i < parts.length; i++) {
			Disputes disputes = new Disputes();
			Corruption corruption = new Corruption();
			InterConsistentSharing sharing = new InterConsistentSharing(new Random(), channels[i], t, testActivity, dealer, disputes, corruption, dispMsg, fieldF, fieldG);
			receivers[i] = new ReceiveThread(sharing, l, disputes);
			assertEquals(0, disputes.size());
			receivers[i].start();
		}
		
		Disputes dealerDisputes = new Disputes();
		Corruption dealerCorruption = new Corruption();
		InterConsistentSharing dealerSharing = new InterConsistentSharing(new Random(), dealerChannel, t, testActivity, dealer, dealerDisputes, dealerCorruption, dispMsg, fieldF, fieldG);
		
		FieldElement[][] dealerShares = dealerSharing.share(secrets);
		
		for (ReceiveThread thread : receivers) {
			thread.join();
			assertFalse(thread.failed);
			System.out.print("Result 1t: ");
			for (int i = 0; i < thread.result[0].length; i++) {
				if (i > 0)
					System.out.print(", ");
				System.out.print(thread.result[0][i]);
			}
			System.out.println("");
			System.out.print("Result 2t: ");
			for (int i = 0; i < thread.result[1].length; i++) {
				if (i > 0)
					System.out.print(", ");
				System.out.print(thread.result[1][i]);
			}
			System.out.println("");
			System.out.println(thread.disputes);
			assertEquals(0, thread.disputes.size());
		}
		
		System.out.print("Dealer Result 1t: ");
		for (int i = 0; i < dealerShares[0].length; i++) {
			if (i > 0)
				System.out.print(", ");
			System.out.print(dealerShares[0][i]);
		}
		System.out.println("");
		System.out.print("Dealer Result 2t: ");
		for (int i = 0; i < dealerShares[1].length; i++) {
			if (i > 0)
				System.out.print(", ");
			System.out.print(dealerShares[1][i]);
		}
		System.out.println("");
		
		FieldPolynomial1D polyA1t = reconstructPoly(n, receivers, parts, dealer, dealerShares, 0, 0);
		System.out.println(polyA1t);
		FieldPolynomial1D polyA2t = reconstructPoly(n, receivers, parts, dealer, dealerShares, 0, 1);
		System.out.println(polyA2t);
		
		FieldPolynomial1D polyB1t = reconstructPoly(n, receivers, parts, dealer, dealerShares, 1, 0);
		System.out.println(polyB1t);
		FieldPolynomial1D polyB2t = reconstructPoly(n, receivers, parts, dealer, dealerShares, 1, 1);
		System.out.println(polyB2t);
		
		assertEquals(secretA, polyA1t.eval(fieldF.zero()));
		assertEquals(secretA, polyA2t.eval(fieldF.zero()));
		assertEquals(secretB, polyB1t.eval(fieldF.zero()));
		assertEquals(secretB, polyB2t.eval(fieldF.zero()));

		assertTrue(polyA1t.degree() <= 1);
		assertTrue(polyA2t.degree() <= 2);
		assertTrue(polyB1t.degree() <= 1);
		assertTrue(polyB2t.degree() <= 2);
}
	
	private FieldPolynomial1D reconstructPoly(int n, ReceiveThread[] receivers, Participant[] parts, Participant dealer, FieldElement[][] dealerShares, int value, int t) {
		FieldElement[] shares = new FieldElement[n+1];
		FieldElement[] sharesids = new FieldElement[n+1];
		for (int i = 0; i < receivers.length; i++) {
			shares[i] = receivers[i].result[t][value];
			sharesids[i] = fieldF.element(parts[i].getID());
		}
		shares[n] = dealerShares[t][value];
		sharesids[n] = fieldF.element(dealer.getID());
		FieldPolynomial1D poly = Lagrange.interpolate(sharesids, shares);
		return poly;
	}
}
