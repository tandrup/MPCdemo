package dk.au.daimi.tandrup.MPC.protocols;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	ConsistentSharingTest.class,
	DisputesTest.class,
	DoubleRandomTest.class,
	EvaluateTest.class,
	FlipTest.class,
	FullTest.class,
	InterConsistentSharingTest.class,
	OpenRobustTest.class,
	OpenTest.class,
	PreProcessTest.class,
	RobustDoubleRandomTest.class,
	TriplesTest.class,
	VssTest.class
})
public class AllProtocolTests {
}
