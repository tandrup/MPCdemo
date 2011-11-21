package dk.au.daimi.tandrup.MPC.net.ssl;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Random;
import java.util.SortedSet;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.net.messages.StringMessage;
import dk.au.daimi.tandrup.MPC.net.messages.TransferMessage;
import dk.au.daimi.tandrup.MPC.net.ssl.SSLCommunicationChannel;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.SecurityManager;

public class SSLCommunicationChannelTest {
	private SSLCommunicationChannel ch1;
	private SSLCommunicationChannel ch2;
	private SSLCommunicationChannel ch3;
	private Exception lastException = null;

	@Before
	public void setUp() throws Exception {
	}

	@Test(timeout=10000)
	public void test1() throws Exception {
		KeyStore store1 = SecurityManager.getJavaKeyStoreFromFile("server1.store");
		KeyStore store2 = SecurityManager.getJavaKeyStoreFromFile("server2.store");
		KeyStore store3 = SecurityManager.getJavaKeyStoreFromFile("server3.store");
		
		ch1 = new SSLCommunicationChannel(8001, store1, new Random());
		ch2 = new SSLCommunicationChannel(8002, store2, new Random());
		ch3 = new SSLCommunicationChannel(8003, store3, new Random());

		SSLCommunicationChannel[] chs = new SSLCommunicationChannel[] {ch1, ch2, ch3};
		
		X509Certificate cert1 = (X509Certificate)store1.getCertificate("server1");
		X509Certificate cert2 = (X509Certificate)store2.getCertificate("server2");
		X509Certificate cert3 = (X509Certificate)store3.getCertificate("server3");

		assertNotNull("cert1", cert1);
		assertNotNull("cert2", cert2);
		assertNotNull("cert3", cert3);

		Endpoint e1 = new Endpoint(InetAddress.getLocalHost(), 8001, cert1.getSubjectX500Principal().getName());
		Endpoint e2 = new Endpoint(InetAddress.getLocalHost(), 8002, cert2.getSubjectX500Principal().getName());
		Endpoint e3 = new Endpoint(InetAddress.getLocalHost(), 8003, cert3.getSubjectX500Principal().getName());
		
		Participant p1 = new TestParticipant(8001, cert1);
		Participant p2 = new TestParticipant(8002, cert2);
		Participant p3 = new TestParticipant(8003, cert3);
		
		PrivateKey key1 = (PrivateKey)store1.getKey("server1", "secret".toCharArray());
		PrivateKey key2 = (PrivateKey)store2.getKey("server2", "secret".toCharArray());
		PrivateKey key3 = (PrivateKey)store3.getKey("server3", "secret".toCharArray());

		assertNotNull("key1", key1);
		assertNotNull("key2", key2);
		assertNotNull("key3", key3);
		
		ch1.init(Arrays.asList(new Endpoint[] {e2, e3}), cert1, key1, 1000);
		ch2.init(Arrays.asList(new Endpoint[] {e1, e3}), cert2, key2, 1000);
		ch3.init(Arrays.asList(new Endpoint[] {e1, e2}), cert3, key3, 1000);

		Thread client1 = new Thread(
				new Runnable() {
					public void run() {
						try {
							ch1.startSession();
						} catch (Exception e) {
							e.printStackTrace();
							lastException = e;
						}
					}
				}, 
				"client1");
		Thread client2 = new Thread(
				new Runnable() {
					public void run() {
						try {
							ch2.startSession();
						} catch (Exception e) {
							e.printStackTrace();
							lastException = e;
						}
					}
				}, 
				"client2");
		Thread client3 = new Thread(
				new Runnable() {
					public void run() {
						try {
							ch3.startSession();
						} catch (Exception e) {
							e.printStackTrace();
							lastException = e;
						}
					}
				}, 
				"client3");
		
		client1.start();
		client2.start();
		client3.start();
		
		client1.join();
		client2.join();
		client3.join();

		if (lastException != null)
			throw lastException;
		
		System.out.println(ch1.listConnectedParticipants());
		System.out.println(ch2.listConnectedParticipants());
		System.out.println(ch3.listConnectedParticipants());

		assertEquals("Channel 1 local ID", 1, ch1.localParticipant().getID());
		assertEquals("Channel 2 local ID", 2, ch2.localParticipant().getID());
		assertEquals("Channel 3 local ID", 3, ch3.localParticipant().getID());
		
		for (int i = 0; i < chs.length; i++) {
			SortedSet<? extends Participant>	connParts = chs[i].listConnectedParticipants();
			assertEquals("Ch" + (i+1) + " no of participants", 3, connParts.size());
			int id = 1;
			for (Participant participant : connParts) {
				assertEquals("Ch" + (i+1) + " id of " + participant, id++, participant.getID());
			}
		}
		
		Activity activity = new Activity("Test");

		Serializable msg, exp;

		{
			msg = new StringMessage("Hej hej");
			exp = new StringMessage("Hej hej");
			ch1.send(p2, activity.subActivity("1>2"), msg);
			IChannelData sMsg = ch2.receive(activity.subActivity("1>2"));
			System.out.println(sMsg);
			assertNotNull("Message result is null", sMsg);
			assertEquals("1 -> 2", exp, sMsg.getObject());
		}

		// Test loopback
		{
			msg = new StringMessage("Hej hej");
			exp = new StringMessage("Hej hej");
			ch1.send(p1, activity.subActivity("1>1"), msg);
			IChannelData sMsg = ch1.receive(activity.subActivity("1>1"));
			System.out.println(sMsg);
			assertNotNull("Message result is null", sMsg);
			assertEquals("1 -> 1", exp, sMsg.getObject());
		}

		{
			msg = new StringMessage("Mojn mojn");
			exp = new StringMessage("Mojn mojn");
			ch3.send(p1, activity.subActivity("3>1"), msg);
			ch2.send(p1, activity.subActivity("2>1"), msg);
			IChannelData sMsg1 = ch1.receive(activity.subActivity("2>1"));
			IChannelData sMsg2 = ch1.receive(activity.subActivity("3>1"));
			System.out.println(sMsg2);
			assertEquals("3 -> 1", exp, sMsg2.getObject());
			System.out.println(sMsg1);
			assertEquals("2 -> 1", exp, sMsg1.getObject());
		}

		{
			IChannelData sMsg;
			
			msg = new StringMessage("Dav dav");
			exp = new StringMessage("Dav dav");		
			ch2.broadcast(activity.subActivity("2>*"), msg);
			sMsg = ch3.receive(activity.subActivity("2>*"));
			System.out.println(sMsg);
			assertEquals("2 -> 3", exp, sMsg.getObject());
			sMsg = ch1.receive(activity.subActivity("2>*"));
			System.out.println(sMsg);
			assertEquals("2 -> 1", exp, sMsg.getObject());
		
			ch1.transfer(p3, activity.subActivity("Trans"), sMsg);
			IChannelData sMsg2 = ch3.receive(activity.subActivity("Trans"));
			TransferMessage transMsg = (TransferMessage)sMsg2.getObject();
			System.out.println(transMsg);
			assertNotNull("Message result is null", transMsg);
			assertNotNull("Message result is null", transMsg.getSignedMessage());
			assertNotNull("Sender is null", transMsg.getSignedMessage().getSender());
			assertEquals(sMsg, transMsg.getSignedMessage());
		}
	}
	
	private class TestParticipant implements Participant {
		private int port = 0;
		private X509Certificate cert;
		
		public TestParticipant(int port, X509Certificate cert) {
			this.port = port;
			this.cert = cert;
		}
		
		public X509Certificate getCertificate() {
			return cert;
		}

		
		public String getCertificateSubjectDN() {
			return cert.getSubjectX500Principal().getName();
		}

		public InetAddress getInetAddress() {
			try {
				return InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return null;
			}
		}

		public int getPort() {
			return port;
		}

		public int getID() {
			// FIXME Auto-generated method stub
			return 0;
		}

		@Override
		public int hashCode() {
			return cert.hashCode() + port;
		}

		public boolean equals(Participant other) {
			return (this.getCertificateSubjectDN().equals(other.getCertificateSubjectDN()));
		}

		public int compareTo(Participant other) {
			return this.getCertificateSubjectDN().compareTo(other.getCertificateSubjectDN());
		}
	}
}
