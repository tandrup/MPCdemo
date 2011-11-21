package dk.au.daimi.tandrup.MPC.net;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ActivityTest {

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testEquals() {
		Activity a1a = new Activity("test1");
		Activity a1b = new Activity("test1");
		Activity a2 = a1a.subActivity("subting sdjhsdj");
		Activity a3 = a1b.subActivity("subting sdjhsdj");
		Activity a4 = a1a.subActivity("Anden sub sdjhsdj");

		assertTrue(a2.equals(a2));
		assertTrue(a3.equals(a3));
		assertTrue(a4.equals(a4));

		assertTrue(a2.equals(a3));
		assertTrue(a3.equals(a2));
		
		assertFalse(a2.equals(a4));
		assertFalse(a3.equals(a4));
		assertFalse(a4.equals(a2));
		assertFalse(a4.equals(a3));
	}

}
