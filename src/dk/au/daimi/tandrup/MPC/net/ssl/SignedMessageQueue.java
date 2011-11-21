package dk.au.daimi.tandrup.MPC.net.ssl;

import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.Participant;

public interface SignedMessageQueue {
	public SignedMessage receive();
	
	public SignedMessage receive(long timeout);
	
	public SignedMessage receive(Participant from);
	
	public SignedMessage receive(Participant from, long timeout);
	
	public SignedMessage receive(Activity activity);

	public SignedMessage receive(Activity activity, long timeout);

	public SignedMessage receive(Participant from, Activity activity);

	public SignedMessage receive(Participant from, Activity activity, long timeout);
	
	public void add(SignedMessage msg);
}
