package dk.au.daimi.tandrup.MPC.protocols;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.LongField;
import dk.au.daimi.tandrup.MPC.math.fields.polynomial.PolynomialField;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.SecurityManager;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.net.ssl.Endpoint;
import dk.au.daimi.tandrup.MPC.net.ssl.SSLCommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.gates.*;

public class FullTestWithSSL {
	private Field fieldF, fieldG;
	FieldElement two, three;

	@Before
	public void setUp() throws Exception {
		fieldF = new LongField(11);
		FieldPolynomial1D basePoly = new FieldPolynomial1D(new FieldElement[] {fieldF.element(7), fieldF.element(1), fieldF.element(1)});
		fieldG = new PolynomialField(basePoly);
		
		two = fieldF.element(2);
		three = fieldF.element(3);
	}

	class ClientThread extends Thread {
		public boolean failed = false;
		public int id;
		public CommunicationChannel channel;
		public PreProcess preProcess;
		public Evaluate evaluate;
		public Disputes disputes;
		public InputGate i1, i2;
		public MultGate m1, m2;
		public OutputGate o1, o2, o3;
		public AffineGate a1, a2;
		public RandomGate r1, r2;

		public ClientThread(int id, CommunicationChannel channel, PreProcess preProcess, Evaluate evaluate, Disputes disputes) {
			this.id = id;
			this.channel = channel;
			this.preProcess = preProcess;
			this.evaluate = evaluate;
			this.disputes = disputes;
		}
		
		public void run() {
			try {
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}
				
				channel.startSession();

				try { Thread.sleep(100); }
				catch (InterruptedException e) {}

				preProcess.run();
				
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

	@Test(timeout=15000)
	public void testRun() throws InterruptedException, IOException, GeneralSecurityException {
		Activity testActivity = new Activity("Test");

		int n = 5;
		int t = 1;
		//int l = n - (2*t + 1);
		
		KeyStore[] stores = new KeyStore[n];
		for (int i = 0; i < stores.length; i++) {
			stores[i] = SecurityManager.getJavaKeyStoreFromFile("server" + (i+1) + ".store");
		}

		SSLCommunicationChannel channels[] = new SSLCommunicationChannel[n];
		for (int i = 0; i < channels.length; i++) {
			channels[i] = new SSLCommunicationChannel(8001 + i, stores[i], new Random());
		}
		
		X509Certificate[] certs = new X509Certificate[n];
		PrivateKey[] keys = new PrivateKey[n];
		for (int i = 0; i < certs.length; i++) {
			certs[i] = (X509Certificate)stores[i].getCertificate("server" + (i+1));
			assertNotNull("cert" + (i+1), certs[i]);
			keys[i] = (PrivateKey)stores[i].getKey("server" + (i+1), "secret".toCharArray());
		}

		Endpoint[] endpoints = new Endpoint[n];
		for (int i = 0; i < endpoints.length; i++) {
			endpoints[i] = new Endpoint(InetAddress.getLocalHost(), 8001 + i, certs[i].getSubjectX500Principal().getName());
		}

		for (int i = 0; i < channels.length; i++) {
			List<Endpoint> others = new ArrayList<Endpoint>(n-1);
			for (int j = 0; j < endpoints.length; j++) {
				if (j != i)
					others.add(endpoints[j]);
			}
			channels[i].init(others, certs[i], keys[i], 2000);
		}

		Participant[] parts = new Participant[n];
		for (int i = 0; i < parts.length; i++) {
			parts[i] = channels[i].localParticipant();
		}

		int rCount = 3;
		int mCount = 3;
		int aCount = 3;
		int oCount = 3;
		Participant[] inputProviders = new Participant[3];
		inputProviders[0] = parts[1];
		inputProviders[1] = parts[2];
		inputProviders[2] = parts[1];

		ClientThread[] preProcessors = new ClientThread[n];
		for (int i = 0; i < channels.length; i++) {
			Disputes disputes = new Disputes();
			PreProcess preProcess = new PreProcess(new Random(), channels[i], t, testActivity, rCount, inputProviders, mCount, aCount, oCount, fieldF, fieldG, disputes);
			Evaluate evaluate = new Evaluate(channels[i], t, testActivity.subActivity("Eval"));
			preProcessors[i] = new ClientThread(i, channels[i], preProcess, evaluate, disputes);
			preProcessors[i].start();
		}

		for (int i = 0; i < preProcessors.length; i++) {
			ClientThread thread = preProcessors[i];

			thread.join();
			assertFalse("Thread failed", thread.failed);
			assertEquals("No disputes", 0, thread.disputes.size());
		}

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

		for (int i = 0; i < preProcessors.length; i++) {
			ClientThread thread = preProcessors[i];
			assertEquals(fieldF.element(6), thread.o1.output());
			assertEquals(fieldF.element(0), thread.o2.output());
			assertTrue("o3 is not computed", thread.o3.isComputed());
			assertNotNull("o3 result is null", thread.o3.output());
		}
	}
}
