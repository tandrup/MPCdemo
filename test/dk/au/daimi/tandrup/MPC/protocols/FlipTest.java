package dk.au.daimi.tandrup.MPC.protocols;

import static org.junit.Assert.*;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.Lagrange;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.BigIntegerFieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.ChannelProvider;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public class FlipTest {
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
	public void testRun() throws InterruptedException, IOException, GeneralSecurityException, ClassNotFoundException {
		final FieldElement[] res = new FieldElement[3];
		final FieldElement[] ids = new FieldElement[3];
		
		channelProvider = ChannelProvider.getDefaultInstance(3);
		
		Activity testActivity = new Activity("test");

		CommunicationChannel comm1 = channelProvider.getChannel(1);
		CommunicationChannel comm2 = channelProvider.getChannel(2);
		CommunicationChannel comm3 = channelProvider.getChannel(3);

		int t = 2;

		Field field = (new BigIntegerFieldElement(0, BigInteger.valueOf(101))).field();
		
		final Flip f1 = new Flip(field, new Random(), comm1, t, testActivity);
		final Flip f2 = new Flip(field, new Random(), comm2, t, testActivity);
		final Flip f3 = new Flip(field, new Random(), comm3, t, testActivity);
		
		class UnitTestThread extends Thread {
			public boolean failed = false;
		}
		
		UnitTestThread t1 = new UnitTestThread() {
			public void run() {
				try {
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
					FieldElement s = f1.run();;
					res[0] = s;
					ids[0] = s.field().element(1);
					System.out.println(s);
				} catch (Exception e) {
					System.err.print(this.getName() + ": ");
					e.printStackTrace();
					this.failed = true; 
				}
			}
		};
		
		UnitTestThread t2 = new UnitTestThread() {
			public void run() {
				try {
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
					FieldElement s = f2.run();;
					res[1] = s;
					ids[1] = s.field().element(2);
					System.out.println(s);
				} catch (Exception e) {
					System.err.print(this.getName() + ": ");
					e.printStackTrace();
					this.failed = true; 
				}
			}
		};

		UnitTestThread t3 = new UnitTestThread() {
			public void run() {
				try {
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
					FieldElement s = f3.run();;
					res[2] = s;
					ids[2] = s.field().element(3);
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
		
		Thread.sleep(100);
		
		t1.join();
		t2.join();
		t3.join();
		
		assertFalse("Thread1 failed", t1.failed);
		assertFalse("Thread2 failed", t2.failed);
		assertFalse("Thread3 failed", t3.failed);
		
		System.out.println(Lagrange.interpolate(ids, res));
		
		channelProvider.close();
	}
}
