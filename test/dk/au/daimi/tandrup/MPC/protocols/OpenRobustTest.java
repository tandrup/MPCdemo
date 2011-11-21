package dk.au.daimi.tandrup.MPC.protocols;

import static org.junit.Assert.*;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.BigIntegerField;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.ChannelProvider;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public class OpenRobustTest {
	private ChannelProvider channelProvider;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		if (channelProvider != null)
			channelProvider.close();
	}

	@Test//(timeout=10000)
	public void testRun() throws Exception {
		Activity testActivity = new Activity("test");

		int n = 5;
		int t = 1;
		int l = n - (2*t + 1);
		
		channelProvider = ChannelProvider.getDefaultInstance(n);
		CommunicationChannel[] channels = channelProvider.getChannels();

		Field field = new BigIntegerField(BigInteger.valueOf(101));
		
		FieldElement secretA = field.element(42);
		FieldElement secretB = field.element(76);
		FieldElement[] secrets = new FieldElement[] {secretA, secretB};
		
		assertEquals(l, secrets.length);
		
		FieldPolynomial1D polyA = new FieldPolynomial1D(secretA, t, new Random());
		FieldPolynomial1D polyB = new FieldPolynomial1D(secretB, t, new Random());
		
		FieldElement[][] shares = new FieldElement[n][2];
		for (int i = 0; i < shares.length; i++) {
			shares[i] = new FieldElement[] {
					polyA.eval(field.element(i+1)), 
					polyB.eval(field.element(i+1))};			
		}
		
		OpenRobust[] opens = new OpenRobust[n];
		for (int i = 0; i < opens.length; i++) {
			opens[i] = new OpenRobust(channels[i], t, testActivity, t, shares[i]);
		}
		
		
		class OpenThread extends Thread {
			public boolean failed = false;
			private OpenRobust open;
			public FieldElement[] result;
			
			public OpenThread(OpenRobust open) {
				this.open = open;
			}
			
			public void run() {
				try {
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
					
					result = open.run();
				} catch (Exception e) {
					System.err.print(this.getName() + ": ");
					e.printStackTrace();
					this.failed = true; 
				}
			}
		}
		
		OpenThread[] openThreads = new OpenThread[n];
		for (int i = 0; i < openThreads.length; i++) {
			openThreads[i] = new OpenThread(opens[i]);
			openThreads[i].start();
		}

		for (int i = 0; i < openThreads.length; i++) {
			openThreads[i].join();
			assertFalse(openThreads[i].failed);
		}

		printResult(secrets);
		for (int i = 0; i < openThreads.length; i++) {
			printResult(openThreads[i].result);
			assertEquals(secrets, openThreads[i].result);
		}
	}
	
	private void printResult(FieldElement[] result) {
		System.out.print("[");
		for (int i = 0; i < result.length; i++) {			
			if (i > 0)
				System.out.print(", ");
			System.out.print(result[i]);
		}
		System.out.println("]");
	}
	
	public static void main(String[] args) throws IOException, GeneralSecurityException, InterruptedException, ClassNotFoundException {
		Activity testActivity = new Activity("test");

		int n = 5;
		int t = 1;
		
		ChannelProvider channelProvider = ChannelProvider.getDefaultInstance(n);
		CommunicationChannel[] channels = channelProvider.getChannels();

		Field field = new BigIntegerField(BigInteger.valueOf(101));
		
		FieldElement secretA = field.element(42);
		FieldElement secretB = field.element(76);
		
		FieldPolynomial1D polyA = new FieldPolynomial1D(secretA, t, new Random());
		FieldPolynomial1D polyB = new FieldPolynomial1D(secretB, t, new Random());
		
		FieldElement[][] shares = new FieldElement[n][2];
		for (int i = 0; i < shares.length; i++) {
			shares[i] = new FieldElement[] {
					polyA.eval(field.element(i+1)), 
					polyB.eval(field.element(i+1))};			
		}
		
		OpenRobust[] opens = new OpenRobust[n];
		for (int i = 0; i < opens.length; i++) {
			opens[i] = new OpenRobust(channels[i], t, testActivity, t, shares[i]);
		}
		
		
		class OpenThread extends Thread {
			private OpenRobust open;
			
			public OpenThread(OpenRobust open) {
				super("OpenRobustTest " + open);
				this.open = open;
			}
			
			public void run() {
				try {
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
					
					open.run();
				} catch (Exception e) {
					System.err.print(this.getName() + ": ");
					e.printStackTrace();
				}
			}
		}
		
		OpenThread[] openThreads = new OpenThread[n];
		for (int i = 0; i < openThreads.length; i++) {
			openThreads[i] = new OpenThread(opens[i]);
			openThreads[i].start();
		}

		for (int i = 0; i < openThreads.length; i++) {
			openThreads[i].join();
		}
	}
}
