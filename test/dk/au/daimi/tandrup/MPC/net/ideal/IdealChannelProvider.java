package dk.au.daimi.tandrup.MPC.net.ideal;

import java.io.IOException;
import java.security.GeneralSecurityException;

import dk.au.daimi.tandrup.MPC.net.ChannelProvider;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public class IdealChannelProvider extends ChannelProvider {
	private int count;
	private IdealCommChannelHandler commHandler;
	private Participant[] participants;
	private CommunicationChannel[] channels;

	public IdealChannelProvider(int count) {
		this.count = count;
		generateChannels();
	}

	private void generateChannels() {
		commHandler = new IdealCommChannelHandler();
		
		participants = new Participant[count];
		for (int i = 0; i < participants.length; i++) {
			participants[i] = commHandler.createNewParticipant(i+1);
		}
		
		channels = new CommunicationChannel[count];
		for (int i = 0; i < channels.length; i++) {
			channels[i] = commHandler.newChannel(participants[i]);
		}
	}
	
	@Override
	public CommunicationChannel[] getChannels() {
		return channels;
	}

	@Override
	public Participant[] getParticipants() {
		return participants;
	}

	@Override
	public void close() throws IOException, GeneralSecurityException, ClassNotFoundException {
		for (CommunicationChannel ch : channels) {
			ch.close();
		}
	}

	@Override
	public String getStatistics() {
		return commHandler.getStatistics();
	}

	@Override
	public void resetStatistics() {
		commHandler.resetStatistics();
	}
}
