package dk.au.daimi.tandrup.MPC.math.fields.integer;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.fields.AbstractFieldTest;

public class LongFieldTest extends AbstractFieldTest {

	@Before
	public void setUp() throws Exception {
		field = new LongField(11);
		two = field.element(2);
		five = field.element(5);
	}

	@Test
	public void testToString() {
		assertEquals("2 mod 11", two.toString());
		assertEquals("5 mod 11", five.toString());
	}
	
	@Test
	public void testPowSpecial() {
		field = new LongField(101);
		assertEquals(field.element(87), field.element(3).pow(40));
	}

}
