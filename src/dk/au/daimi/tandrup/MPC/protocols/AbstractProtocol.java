package dk.au.daimi.tandrup.MPC.protocols;

import java.util.logging.Logger;

import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public abstract class AbstractProtocol {
	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	
	protected final CommunicationChannel channel;
	protected final int threshold;
	protected final Activity activity;

	public AbstractProtocol(CommunicationChannel channel, int threshold, Activity activity) {
		super();
		this.channel = channel;
		this.threshold = threshold;
		this.activity = activity;
	}
}
