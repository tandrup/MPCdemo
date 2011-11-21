package dk.au.daimi.tandrup.MPC.protocols;

import static org.junit.Assert.*;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.BigIntegerFieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.ChannelProvider;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public class VssTest {
	private ChannelProvider channelProvider;
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		if (channelProvider != null)
			channelProvider.close();
	}

	@Test(timeout=30000)
	public void test1() throws IOException, GeneralSecurityException, InterruptedException, ClassNotFoundException {
		Activity testActivity = new Activity("test");

		channelProvider = ChannelProvider.getDefaultInstance(4);

		Participant dealer = channelProvider.getParticipant(4);

		Participant part1 = channelProvider.getParticipant(1);
		Participant part2 = channelProvider.getParticipant(2);
		Participant part3 = channelProvider.getParticipant(3);
		
		Collection<Participant> parts = new ArrayList<Participant>();
		parts.add(part1);
		parts.add(part2);
		parts.add(part3);
		parts.add(dealer);

		CommunicationChannel commDealer = channelProvider.getChannel(4);
		
		CommunicationChannel comm1 = channelProvider.getChannel(1);
		CommunicationChannel comm2 = channelProvider.getChannel(2);
		CommunicationChannel comm3 = channelProvider.getChannel(3);

		int t = 2;

		FieldElement secret = new BigIntegerFieldElement(42, BigInteger.valueOf(101));

		final Vss vssDealer = new Vss(testActivity, new Random(), commDealer, t, secret.field(), dealer, parts);

		final Vss vss1 = new Vss(testActivity, new Random(), comm1, t, secret.field(), dealer, parts);
		final Vss vss2 = new Vss(testActivity, new Random(), comm2, t, secret.field(), dealer, parts);
		final Vss vss3 = new Vss(testActivity, new Random(), comm3, t, secret.field(), dealer, parts);

		class UnitTestThread extends Thread {
			public boolean failed = false;

			public UnitTestThread(String arg0) {
				super(arg0);
			}
		}
		
		UnitTestThread t1 = new UnitTestThread("vss1.receive()") {
			public void run() {
				try {
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
					FieldElement s = vss1.receive();
					System.out.println(s);
				} catch (Exception e) {
					System.err.print(this.getName() + ": ");
					e.printStackTrace();
					this.failed = true; 
				}
			}
		};
		
		UnitTestThread t2 = new UnitTestThread("vss2.receive()") {
			public void run() {
				try {
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
					FieldElement s = vss2.receive();
					System.out.println(s);
				} catch (Exception e) {
					System.err.print(this.getName() + ": ");
					e.printStackTrace();
					this.failed = true; 
				}
			}
		};
		
		UnitTestThread t3 = new UnitTestThread("vss3.receive()") {
			public void run() {
				try {
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
					FieldElement s = vss3.receive();
					System.out.println(s);
				} catch (Exception e) {
					System.err.print(this.getName() + ": ");
					e.printStackTrace();
					this.failed = true; 
				}
			}
		};
		
		t1.start();
		t2.start();
		t3.start();

		vssDealer.share(secret);
		
			t1.join();
			t2.join();	
			t3.join();	
		
		assertFalse(t1.failed);
		assertFalse(t2.failed);
		assertFalse(t3.failed);
	}

}
