package dk.au.daimi.tandrup.MPC.protocols;

import java.util.concurrent.ExecutionException;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public class OpenRobustWrapper extends AbstractProtocol {
	//private int l;
	private final int sharesCount;
	private final OpenRobust[] subProtocols;
	
	public OpenRobustWrapper(CommunicationChannel channel, int threshold, Activity activity, int d, FieldElement[] shares) {
		super(channel, threshold, activity);
		
		sharesCount = shares.length;
		
		int n = channel.listConnectedParticipants().size();
		int l = n - (2 * threshold + 1);
		int blocks = (int)Math.ceil((double)shares.length / l);

		subProtocols = new OpenRobust[blocks];
		
		for (int i = 0; i < subProtocols.length; i++) {
			FieldElement[] subShares;
			
			if (l > shares.length - i*l)
				subShares = new FieldElement[shares.length - i*l];
			else
				subShares = new FieldElement[l];
				
			// Copy shares to sub array
			System.arraycopy(shares, i*l, subShares, 0, subShares.length);

			// Initialize protocol
			subProtocols[i] = new OpenRobust(channel, threshold, activity.subActivity(Integer.toString(i)), d, subShares);
		}
	}
	
	public FieldElement[] run() throws InterruptedException, ExecutionException {
		FieldElement[] result = new FieldElement[sharesCount];

		int offset = 0;
		for (int i = 0; i < subProtocols.length; i++) {
			FieldElement[] subResult = subProtocols[i].run();
			System.arraycopy(subResult, 0, result, offset, subResult.length);
			offset += subResult.length;
			subProtocols[i] = null;
		}
		
		return result;
	}
}
