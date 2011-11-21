package dk.au.daimi.tandrup.MPC.net;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;

import dk.au.daimi.tandrup.MPC.net.ideal.IdealChannelProvider;
import dk.au.daimi.tandrup.MPC.net.ssl.SSLChannelProvider;

public abstract class ChannelProvider {
	protected transient static final Logger logger = Logger.getLogger(ChannelProvider.class.getName());

	private static final boolean IDEAL = true;

	private static ChannelProvider lastProvider = null;
	
	public abstract Participant[] getParticipants();

	public Participant getParticipant(int id) {
		return getParticipants()[id-1];
	}

	public abstract CommunicationChannel[] getChannels();

	public CommunicationChannel getChannel(int id) {
		return getChannels()[id-1];
	}
	
	public abstract void close() throws IOException, GeneralSecurityException, ClassNotFoundException;

	public abstract String getStatistics();
	public abstract void resetStatistics();
	
	public static ChannelProvider getDefaultInstance(int count) throws IOException, GeneralSecurityException, InterruptedException, ClassNotFoundException  {
		try {
			if (lastProvider != null) {
				lastProvider.close();
				Thread.sleep(100);
			}
		} catch (Exception ex) {
			logger.warning(ex.toString());
		}
		
		if (IDEAL)
			lastProvider = getIdealInstance(count);
		else
			lastProvider = getSSLInstance(count);
		
		return lastProvider;
	}
	
	protected static ChannelProvider getSSLInstance(int count) throws IOException, GeneralSecurityException, InterruptedException  {
		logger.info("Generating SSL Channel Provider");
		return new SSLChannelProvider(count);
	}

	protected static ChannelProvider getIdealInstance(int count) {
		logger.info("Generating Ideal Channel Provider");
		return new IdealChannelProvider(count);
	}

}
