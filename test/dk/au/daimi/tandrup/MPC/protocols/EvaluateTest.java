package dk.au.daimi.tandrup.MPC.protocols;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.LongField;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.ChannelProvider;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.gates.AbstractGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.InputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.LocalInputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.MultGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.OutputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.RemoteInputGate;

public class EvaluateTest {
	private Field fieldF;
	FieldElement two, three;
	Participant[] parts;
	private ChannelProvider channelProvider;

	@Before
	public void setUp() throws Exception {
		fieldF = new LongField(11);
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
		public int id;
		public Evaluate evaluate;
		public Disputes disputes;
		public InputGate i1, i2;
		public MultGate m1, m2;
		public OutputGate o1, o2;

		public PreProcessThread(int id, Evaluate evaluate, Disputes disputes) {
			this.id = id;
			this.evaluate = evaluate;
			this.disputes = disputes;
		}
		
		public void run() {
			try {
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}
				
				//preProcess.run();
				
				switch (id) {
				case 0:
					i1 = new RemoteInputGate(0, parts[1], fieldF.element(6));
					i2 = new RemoteInputGate(1, parts[2], fieldF.element(0));
					m1 = new MultGate(0, fieldF.element(4), fieldF.element(9), fieldF.element(5));
					m2 = new MultGate(1, fieldF.element(9), fieldF.element(5), fieldF.element(1));
					break;
				case 1:
					i1 = new LocalInputGate(0, parts[1], fieldF.element(2), fieldF.element(10));
					i2 = new RemoteInputGate(1, parts[2], fieldF.element(3));
					m1 = new MultGate(0, fieldF.element(4), fieldF.element(6), fieldF.element(6));
					m2 = new MultGate(1, fieldF.element(4), fieldF.element(2), fieldF.element(0));
					break;
				case 2:
					i1 = new RemoteInputGate(0, parts[1], fieldF.element(9));
					i2 = new LocalInputGate(1, parts[2], fieldF.element(6), fieldF.element(8));
					m1 = new MultGate(0, fieldF.element(4), fieldF.element(3), fieldF.element(7));
					m2 = new MultGate(1, fieldF.element(10), fieldF.element(10), fieldF.element(10));
					break;
				case 3:
					i1 = new RemoteInputGate(0, parts[1], fieldF.element(5));
					i2 = new RemoteInputGate(1, parts[2], fieldF.element(9));
					m1 = new MultGate(0, fieldF.element(4), fieldF.element(0), fieldF.element(8));
					m2 = new MultGate(1, fieldF.element(5), fieldF.element(7), fieldF.element(9));
					break;
				case 4:
					i1 = new RemoteInputGate(0, parts[1], fieldF.element(1));
					i2 = new RemoteInputGate(1, parts[2], fieldF.element(1));
					m1 = new MultGate(0, fieldF.element(4), fieldF.element(8), fieldF.element(9));
					m2 = new MultGate(1, fieldF.element(0), fieldF.element(4), fieldF.element(8));
					break;
				}
				
				o1 = new OutputGate(0);
				o2 = new OutputGate(1);
				o1.setInput(m1);
				o2.setInput(m2);
				
				if (i1 instanceof LocalInputGate)
					((LocalInputGate)i1).setInput(two);
				if (i2 instanceof LocalInputGate)
					((LocalInputGate)i2).setInput(three);
				
				m1.setInputGates(i1, i2);
				m2.setInputGates(i1, i2);

				evaluate.evaluate(new AbstractGate[] { o1, o2 });

			} catch (Exception e) {
				System.err.print(this.getName() + ": ");
				e.printStackTrace();
				this.failed = true; 
			}
		}
	}

	@Test(timeout=5000)
	public void testRun() throws InterruptedException, IOException, GeneralSecurityException, ClassNotFoundException {
		Activity testActivity = new Activity("Test");

		int n = 5;
		int t = 1;
		//int l = n - (2*t + 1);

		channelProvider = ChannelProvider.getDefaultInstance(n);

		parts = channelProvider.getParticipants();
		CommunicationChannel[] channels = channelProvider.getChannels();

		PreProcessThread[] preProcessors = new PreProcessThread[n];
		for (int i = 0; i < channels.length; i++) {
			Disputes disputes = new Disputes();
			Evaluate evaluate = new Evaluate(channels[i], t, testActivity.subActivity("Eval"));
			preProcessors[i] = new PreProcessThread(i, evaluate, disputes);
			preProcessors[i].start();
		}

		for (int i = 0; i < preProcessors.length; i++) {
			PreProcessThread thread = preProcessors[i];

			thread.join();
			assertFalse("Thread failed", thread.failed);
			assertEquals("No disputes", 0, thread.disputes.size());
		}

		for (int i = 0; i < preProcessors.length; i++) {
			PreProcessThread thread = preProcessors[i];

			//System.out.println(i + ": " + thread.i1);
			//System.out.println(i + ": " + thread.i2);
			//System.out.println(i + ": " + thread.m1);
			System.out.println(i + ": " + thread.o1);
			//System.out.println(i + ": " + thread.m2);
			System.out.println(i + ": " + thread.o2);
		}

		for (int i = 0; i < preProcessors.length; i++) {
			PreProcessThread thread = preProcessors[i];
			assertEquals(fieldF.element(6), thread.o1.output());
			assertEquals(fieldF.element(6), thread.o2.output());
		}
		
		channelProvider.close();
	}
}
