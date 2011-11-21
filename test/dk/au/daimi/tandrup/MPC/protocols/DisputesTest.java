package dk.au.daimi.tandrup.MPC.protocols;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.net.ChannelProvider;
import dk.au.daimi.tandrup.MPC.net.Participant;

public class DisputesTest {
	private ChannelProvider channelProvider;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		if (channelProvider != null)
			channelProvider.close();
	}

	@Test
	public void test() throws Exception {
		Disputes disp = new Disputes();
		
		channelProvider = ChannelProvider.getDefaultInstance(6);
		
		Participant part1 = channelProvider.getParticipant(1);
		Participant part2 = channelProvider.getParticipant(2);
		Participant part3 = channelProvider.getParticipant(3);
		Participant part4 = channelProvider.getParticipant(4);
		Participant part5 = channelProvider.getParticipant(5);
		Participant part6 = channelProvider.getParticipant(6);
		
		disp.add(part2, part4);

		disp.add(part4, part2);
		
		assertEquals(1, disp.size());

		disp.add(part4, part1);

		assertEquals(2, disp.size());

		assertFalse(disp.contains(part6));
		assertTrue(disp.contains(part1));
		assertTrue(disp.contains(part2));
		assertFalse(disp.contains(part3));
		assertTrue(disp.contains(part4));
		assertFalse(disp.contains(part5));
		
		channelProvider.close();
	}
}
