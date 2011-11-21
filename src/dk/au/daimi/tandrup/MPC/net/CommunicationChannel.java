package dk.au.daimi.tandrup.MPC.net;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.Collection;

public interface CommunicationChannel {
	/**
	 * Start the session by negotiating session id
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public void startSession() throws IOException, GeneralSecurityException, ClassNotFoundException;
	
	public void close() throws IOException, GeneralSecurityException, ClassNotFoundException;
	
	public void send(Participant recv, Activity activity, Serializable obj) throws IOException, GeneralSecurityException, ClassNotFoundException;
	
	public void broadcast(Activity activity, Serializable obj) throws IOException, GeneralSecurityException, ClassNotFoundException;
	
	public void transfer(Participant recv, Activity activity, IChannelData msg) throws IOException, GeneralSecurityException, ClassNotFoundException;
	
	public IChannelData receive(Activity activity) throws IOException, GeneralSecurityException, ClassNotFoundException;
	
	public IChannelData receive(Activity activity, Participant participant) throws IOException, GeneralSecurityException, ClassNotFoundException;
	
	public Collection<IChannelData> receiveFromEachParticipant(Activity activity) throws IOException, GeneralSecurityException, ClassNotFoundException;

	public Collection<IChannelData> receiveFromEachParticipant(Activity activity, Collection<? extends Participant> participants) throws IOException, GeneralSecurityException, ClassNotFoundException;

	public Collection<? extends Participant> listConnectedParticipants();
	
	public Participant localParticipant();
}
