package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Random;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.messages.DisputeMessage;

public class RobustDoubleRandomWrapper extends AbstractRandomProtocol {
	private int l;

	private RobustDoubleRandom[] subProtocols;
	Disputes disputes;
	public RobustDoubleRandomWrapper(Random random, CommunicationChannel channel, int threshold, Activity activity, int l, Field F, Field G, Disputes disputes, Corruption corruption, DisputeMessage disputeMsg) {
		super(random, channel, threshold, activity);
		this.l = l;
		this.disputes = disputes;
		
		int n = channel.listConnectedParticipants().size();
		int blockSize = n - threshold;
		int blocks = (int)Math.ceil((double)l / blockSize);
		
		subProtocols = new RobustDoubleRandom[blocks];
		
		for (int i = 0; i < blocks; i++) {
			subProtocols[i]	 = new RobustDoubleRandom(random, channel, threshold, activity.subActivity(Integer.toString(i)), blockSize, F, G, disputes, corruption, disputeMsg);
		}
	}

	public RandomPair[] run() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException {
		RandomPair[] result = new RandomPair[l];
	
		int offset = 0;
		for (int i = 0; i < subProtocols.length; i++) {
			RandomPair[] subResult = subProtocols[i].run();
			subProtocols[i] = null;

			if (i == subProtocols.length - 1)
				System.arraycopy(subResult, 0, result, offset, l - offset);
			else
				System.arraycopy(subResult, 0, result, offset, subResult.length);
			offset += subResult.length;
		}

		return result;
	}	
}
