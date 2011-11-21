package dk.au.daimi.tandrup.MPC.net.ideal;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.net.messages.StringMessage;

public class IdealCommChannelHandlerTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test(timeout=5000)
	public void testNewChannel() throws IOException, GeneralSecurityException, ClassNotFoundException {
		IdealCommChannelHandler commHandler = new IdealCommChannelHandler();
		
		Activity testActivity = new Activity("test");
		
		Participant part1 = commHandler.createNewParticipant(1);
		Participant part2 = commHandler.createNewParticipant(2);
		Participant part3 = commHandler.createNewParticipant(3);
		
		CommunicationChannel comm1 = commHandler.newChannel(part1);
		CommunicationChannel comm2 = commHandler.newChannel(part2);
		CommunicationChannel comm3 = commHandler.newChannel(part3);
		
		System.out.println("Step 1");
		comm1.send(part2, testActivity, new StringMessage("hej"));
		comm3.send(part2, testActivity, new StringMessage("hej"));
		
		System.out.println("Step 2");
		Collection<IChannelData> chDatas = comm2.receiveFromEachParticipant(testActivity);
		System.out.println("Step 3");
		assertEquals(2, chDatas.size());
		for (IChannelData chData : chDatas) {
			Serializable msg = chData.getObject();
			System.out.println(msg);
			assertEquals("hej", ((StringMessage)msg).getMessage());
		}
	}

}
