package dk.au.daimi.tandrup.MPC.protocols;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

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
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.messages.DisputeMessage;

public class RobustDoubleRandomTest {
	private ChannelProvider channelProvider;
	private Field fieldF, fieldG;

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

	@Test(timeout=60000)
	public void testRun() throws Exception {
		final FieldElement[] res1 = new FieldElement[3];
		final FieldElement[] ids1 = new FieldElement[3];
		final FieldElement[] res2 = new FieldElement[3];
		final FieldElement[] ids2 = new FieldElement[3];
		
		Activity testActivity = new Activity("DoubleRandom");

		channelProvider = ChannelProvider.getDefaultInstance(3);

		CommunicationChannel comm1 = channelProvider.getChannel(1);
		CommunicationChannel comm2 = channelProvider.getChannel(2);
		CommunicationChannel comm3 = channelProvider.getChannel(3);

		int n = 3;
		int t = 1;
		int l = n - t;

		Disputes disputes = new Disputes();
		Corruption corruption = new Corruption();
		
		DisputeMessage dispMsg = new DisputeMessage(comm1.localParticipant());
		
		final RobustDoubleRandom f1 = new RobustDoubleRandom(new Random(), comm1, t, testActivity, l, fieldF, fieldG, disputes, corruption, dispMsg);
		final RobustDoubleRandom f2 = new RobustDoubleRandom(new Random(), comm2, t, testActivity, l, fieldF, fieldG, disputes, corruption, dispMsg);
		final RobustDoubleRandom f3 = new RobustDoubleRandom(new Random(), comm3, t, testActivity, l, fieldF, fieldG, disputes, corruption, dispMsg);
		
		class UnitTestThread extends Thread {
			public boolean failed = false;
		}
		
		UnitTestThread t1 = new UnitTestThread() {
			public void run() {
				try {
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
					RandomPair[] ret = f1.run();
					System.out.println(Arrays.toString(ret));
					res1[0] = ret[0].getR1();
					ids1[0] = ret[0].getR1().field().element(1);
					res2[0] = ret[0].getR2();
					ids2[0] = ret[0].getR2().field().element(1);
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
					RandomPair[] ret = f2.run();
					System.out.println(Arrays.toString(ret));
					res1[1] = ret[0].getR1();
					ids1[1] = ret[0].getR1().field().element(2);
					res2[1] = ret[0].getR2();
					ids2[1] = ret[0].getR2().field().element(2);
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
					RandomPair[] ret = f3.run();
					System.out.println(Arrays.toString(ret));
					res1[2] = ret[0].getR1();
					ids1[2] = ret[0].getR1().field().element(3);
					res2[2] = ret[0].getR2();
					ids2[2] = ret[0].getR2().field().element(3);
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
		
		assertFalse(t1.failed);
		assertFalse(t2.failed);
		assertFalse(t3.failed);

		FieldPolynomial1D poly1 = Lagrange.interpolate(ids1, res1);
		FieldPolynomial1D poly2 = Lagrange.interpolate(ids2, res2);
		
		System.out.println(poly1);
		System.out.println(poly2);
		
		assertEquals(poly1.eval(fieldF.zero()), poly2.eval(fieldF.zero()));
	}
}
