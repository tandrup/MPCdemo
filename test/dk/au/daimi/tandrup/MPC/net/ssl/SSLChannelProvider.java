package dk.au.daimi.tandrup.MPC.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dk.au.daimi.tandrup.MPC.net.ChannelProvider;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.SecurityManager;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public class SSLChannelProvider extends ChannelProvider {
	private int count;
	private Participant[] participants;
	private SSLCommunicationChannel[] channels;

	private Random random = SecureRandom.getInstance("SHA1PRNG");

	public SSLChannelProvider(int count) throws IOException, GeneralSecurityException, InterruptedException {
		this.count = count;
		generateChannels();
	}

	private void generateChannels() throws IOException, GeneralSecurityException, InterruptedException {
		logger.config("Loading " + count + " keystores");
		KeyStore[] stores = new KeyStore[count];
		for (int i = 0; i < stores.length; i++) {
			stores[i] = SecurityManager.getJavaKeyStoreFromFile("server" + (i+1) + ".store");
		}

		logger.config("Creating communication channels from port 8001 up to " + (8000 + count));
		channels = new SSLCommunicationChannel[count];
		for (int i = 0; i < channels.length; i++) {
			channels[i] = new SSLCommunicationChannel(8001 + i, stores[i], random);
		}

		X509Certificate[] certs = new X509Certificate[count];
		PrivateKey[] keys = new PrivateKey[count];
		for (int i = 0; i < certs.length; i++) {
			certs[i] = (X509Certificate)stores[i].getCertificate("server" + (i+1));
			if (certs[i] == null)
				throw new GeneralSecurityException("Could not retrieve certificate for server" + (i+1));
			keys[i] = (PrivateKey)stores[i].getKey("server" + (i+1), "secret".toCharArray());
		}

		Endpoint[] endpoints = new Endpoint[count];
		for (int i = 0; i < endpoints.length; i++) {
			endpoints[i] = new Endpoint(InetAddress.getLocalHost(), 8001 + i, certs[i].getSubjectX500Principal().getName());
		}

		// Init channels
		logger.fine("Initializing channels");
		for (int i = 0; i < channels.length; i++) {
			List<Endpoint> others = new ArrayList<Endpoint>(count-1);
			for (int j = 0; j < endpoints.length; j++) {
				if (j != i)
					others.add(endpoints[j]);
			}
			channels[i].init(others, certs[i], keys[i], 500000);
		}

		// Start session in channels
		logger.info("Start session in channels");
		class StarterThread extends Thread {
			boolean failed = false;
			SSLCommunicationChannel channel;
			StarterThread(SSLCommunicationChannel channel) {
				super("StarterThread for " + channel);
				this.channel = channel;
			}
			public void run() {
				try {
					channel.startSession();
				} catch (Exception e) {
					failed = true;
					e.printStackTrace();
				}
			}
		}

		StarterThread[] starterThreads = new StarterThread[channels.length];
		for (int i = 0; i < starterThreads.length; i++) {
			starterThreads[i] = new StarterThread(channels[i]);
			starterThreads[i].start();
		}
		for (int i = 0; i < starterThreads.length; i++) {
			starterThreads[i].join();
			if (starterThreads[i].failed)
				throw new IllegalStateException("Failed to start session");
		}

		logger.info("Sessions started");

		participants = new Participant[count];
		for (int i = 0; i < participants.length; i++) {
			participants[i] = channels[i].localParticipant();
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
	public void close() throws IOException {
		for (SSLCommunicationChannel ch : channels) {
			if (ch != null)
				ch.close();
		}
	}

	@Override
	public String getStatistics() {
		return "NA\tNA";
	}

	@Override
	public void resetStatistics() {
		// TODO Auto-generated method stub
		
	}
}
