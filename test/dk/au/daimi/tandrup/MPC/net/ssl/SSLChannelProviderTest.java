package dk.au.daimi.tandrup.MPC.net.ssl;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.net.messages.StringMessage;

public class SSLChannelProviderTest {
	Participant part1, part2, part3;
	CommunicationChannel ch1, ch2, ch3;
	Activity activity = new Activity("Test");
	
	@Before
	public void setUp() throws Exception {
	}

	@Test(timeout=10000)
	public void test1() throws Exception {
		SSLChannelProvider chProvider = new SSLChannelProvider(3);
		
		part1 = chProvider.getParticipant(1);
		part2 = chProvider.getParticipant(2);
		part3 = chProvider.getParticipant(3);
		
		ch1 = chProvider.getChannel(1);
		ch2 = chProvider.getChannel(2);
		ch3 = chProvider.getChannel(3);

		Thread t1 = new Thread("Test1") {
			public void run() {
				try {
					checkReceivedData(ch2.receive(activity, part1));
					checkReceivedData(ch2.receive(activity.subActivity("Bla"), part1));
					checkReceivedData(ch2.receive(activity, part1));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		t1.start();
		Thread t2 = new Thread("Test2") {
			public void run() {
				try {
					checkReceivedData(ch3.receive(activity.subActivity("Bla"), part1));
					checkReceivedData(ch3.receive(activity, part1));
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
		};
		t2.start();
		
		Thread.sleep(10);
		
		ch1.broadcast(activity, new StringMessage("Hej"));

		ch1.send(part2, activity, new StringMessage("Hej"));
		
		for (Participant part : ch1.listConnectedParticipants()) {
			ch1.send(part, activity.subActivity("Bla"), new StringMessage("Hej"));
		}

		Thread.sleep(1000);

		t1.join();
		t2.join();
		
		ch1.send(part2, activity, new StringMessage("Hej"));
		ch1.send(part3, activity, new StringMessage("Hej"));
		ch1.send(part2, activity, new StringMessage("Hej"));
		checkReceivedData(ch2.receive(activity, part1));
		checkReceivedData(ch2.receive(activity, part1));
		checkReceivedData(ch3.receive(activity, part1));
	}
	
	private void checkReceivedData(IChannelData chData) throws IOException, ClassNotFoundException {
		assertNotNull("ChannelData null", chData);
		assertEquals("StringMessage(Hej)", chData.getObject().toString());
	}
}
