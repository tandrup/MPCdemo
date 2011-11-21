package dk.au.daimi.tandrup.MPC.protocols;

import static org.junit.Assert.*;

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
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.ChannelProvider;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public class ConsistentSharingTest {
	private Field field;
	private ChannelProvider channelProvider;
	
	@Before
	public void setUp() throws Exception {
		field = new BigIntegerField(BigInteger.valueOf(101));
	}

	@After
	public void tearDown() throws Exception {
		if (channelProvider != null)
			channelProvider.close();
	}

	@Test(timeout=60000)
	public void testReceive() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException {
		Activity testActivity = new Activity("Sharing");

		int n = 5;
		int t = 1;
		final int l = n - (2*t + 1);

		channelProvider = ChannelProvider.getDefaultInstance(n+1);
		Participant[] parts = new Participant[n];
		for (int i = 0; i < parts.length; i++) {
			parts[i] = channelProvider.getParticipant(i+1);
		}

		Participant dealer = channelProvider.getParticipant(n+1);;
		
		CommunicationChannel[] channels = new CommunicationChannel[n];
		for (int i = 0; i < channels.length; i++) {
			channels[i] = channelProvider.getChannels()[i];
		}

		CommunicationChannel dealerChannel = channelProvider.getChannels()[n];

		FieldElement secretA = field.element(42);
		FieldElement secretB = field.element(73);
		FieldElement[] secrets = new FieldElement[] {secretA, secretB};
		assertEquals(l, secrets.length);
		//System.out.println("Secrets: " + secrets);
		
		class ReceiveThread extends Thread {
			public boolean failed = false;
			private ConsistentSharing sharing;
			public FieldElement[] result;
			
			public ReceiveThread(ConsistentSharing sharing) {
				this.sharing = sharing;
			}
			
			public void run() {
				try {
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
					
					result = sharing.receive(field, l);
				} catch (Exception e) {
					System.err.print(this.getName() + ": ");
					e.printStackTrace();
					this.failed = true; 
				}
			}
		}

		ReceiveThread[] receivers = new ReceiveThread[n];
		for (int i = 0; i < parts.length; i++) {
			Disputes disputes = new Disputes();
			ConsistentSharing sharing = new ConsistentSharing(new Random(), channels[i], t, testActivity, dealer, t, disputes);
			receivers[i] = new ReceiveThread(sharing);
			receivers[i].start();
		}
		
		Disputes dealerDisputes = new Disputes();
		ConsistentSharing dealerSharing = new ConsistentSharing(new Random(), dealerChannel, t, testActivity, dealer, t, dealerDisputes);
		
		FieldElement[] dealerShares = dealerSharing.share(secrets);
		
		for (ReceiveThread thread : receivers) {
			thread.join();
			assertFalse(thread.failed);
			System.out.print("Result: ");
			for (int i = 0; i < thread.result.length; i++) {
				if (i > 0)
					System.out.print(", ");
				System.out.print(thread.result[i]);
			}
			System.out.println("");
		}
		
		System.out.print("Dealer Result: ");
		for (int i = 0; i < dealerShares.length; i++) {
			if (i > 0)
				System.out.print(", ");
			System.out.print(dealerShares[i]);
		}
		System.out.println("");
		
		FieldElement[] sharesA = new FieldElement[n+1];
		FieldElement[] sharesAids = new FieldElement[n+1];
		for (int i = 0; i < receivers.length; i++) {
			sharesA[i] = receivers[i].result[0];
			sharesAids[i] = field.element(parts[i].getID());
		}
		sharesA[n] = dealerShares[0];
		sharesAids[n] = field.element(dealer.getID());
		FieldPolynomial1D polyA = Lagrange.interpolate(sharesAids, sharesA);
		System.out.println(polyA);
		
		channelProvider.close();
	}
}
