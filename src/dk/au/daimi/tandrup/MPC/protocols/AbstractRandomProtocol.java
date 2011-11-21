package dk.au.daimi.tandrup.MPC.protocols;

import java.util.Random;

import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public abstract class AbstractRandomProtocol extends AbstractProtocol {
	protected Random random;
	
	public AbstractRandomProtocol(Random random, CommunicationChannel channel, int threshold, Activity activity) {
		super(channel, threshold, activity);
		this.random = random;
	}
}
