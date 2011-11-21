package dk.au.daimi.tandrup.MPC.net.ssl;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.security.KeyStore;
import java.security.PrivateKey;

import javax.net.ssl.SSLSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.net.messages.StringControlMessage;
import dk.au.daimi.tandrup.MPC.net.ssl.ClientPort;
import dk.au.daimi.tandrup.MPC.net.ssl.ConnectionListener;
import dk.au.daimi.tandrup.MPC.net.ssl.LinkedListMessageQueue;
import dk.au.daimi.tandrup.MPC.net.ssl.ServerPort;
import dk.au.daimi.tandrup.MPC.net.ssl.SignedMessage;

import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.SecurityManager;

public class PortTest {
	KeyStore clientStore, serverStore;
	DummyConnectionHandler connHandler;
	ConnectionListener connListener;
	ServerPort server;
	
	@Before
	public void setUp() throws Exception {
	}
	
	@Test(timeout=5000)
	public void testConnectionCreation() throws Exception {
		clientStore = SecurityManager.getJavaKeyStoreFromFile("user1.store");
		serverStore = SecurityManager.getJavaKeyStoreFromFile("server1.store");
		
		connHandler = new DummyConnectionHandler();
		connListener = new ConnectionListener(50000, serverStore, connHandler);

		assertNull(connHandler.lastSocket);
		
		server = new ServerPort(serverStore, null);
		ClientPort client = new ClientPort(InetAddress.getLocalHost(), 50000, clientStore, null);
		LinkedListMessageQueue clientQueue = new LinkedListMessageQueue();
		client.setReceiveQueue(clientQueue);
		client.start();

		// Wait for client to connect to server
		Thread.sleep(100);
		
		assertNotNull(connHandler.lastSocket);
		
		LinkedListMessageQueue serverQueue = new LinkedListMessageQueue();
		server.setReceiveQueue(serverQueue);
		
		server.start();
		
		PrivateKey pkUser1 = (PrivateKey)clientStore.getKey("user1", "secret".toCharArray());
		PrivateKey pkServer1 = (PrivateKey)serverStore.getKey("server1", "secret".toCharArray());

		Activity activity = new Activity("Test");
		
		SignedMessage userMsgHej = new SignedMessage(activity, 1, new StringControlMessage("Hej hej"), pkUser1);
		SignedMessage userMsgMojn = new SignedMessage(activity, 2, new StringControlMessage("Mojn mojn"), pkUser1);

		SignedMessage serverMsgHej = new SignedMessage(activity, 3, new StringControlMessage("Hej hej"), pkServer1);
		SignedMessage serverMsgMojn = new SignedMessage(activity, 4, new StringControlMessage("Mojn mojn"), pkServer1);

		// Test Client -> Server
		client.sendMessage(userMsgHej);
		SignedMessage recv = serverQueue.receive(1000);
		StringControlMessage msgRecv = (StringControlMessage)recv.getObject();
		assertEquals("Hej hej", msgRecv.getMessage());
		
		// Test Server -> Client 
		server.sendMessage(serverMsgHej);
		recv = clientQueue.receive(1000);
		msgRecv = (StringControlMessage)recv.getObject();
		assertEquals("Hej hej", msgRecv.getMessage());
		
		// Stop client and wait 100 msec
		client.stop();
		Thread.sleep(1000);
		
		// Test Server -> Client making the server wait for client to restart connection buffer
		server.sendMessage(serverMsgMojn);
		server.sendMessage(serverMsgHej);
		Thread.sleep(1000);
		client.start();
		recv = clientQueue.receive(1000);
		msgRecv = (StringControlMessage)recv.getObject();
		assertEquals("Mojn mojn", msgRecv.getMessage());

		// Test Server -> Client 
		recv = clientQueue.receive(1000);
		msgRecv = (StringControlMessage)recv.getObject();
		assertEquals("Hej hej", msgRecv.getMessage());

		// Test Server -> Client 
		server.sendMessage(serverMsgMojn);
		recv = clientQueue.receive(1000);
		msgRecv = (StringControlMessage)recv.getObject();
		assertEquals("Mojn mojn", msgRecv.getMessage());

		// Test Client -> Server
		client.sendMessage(userMsgMojn);
		recv = serverQueue.receive(1000);
		msgRecv = (StringControlMessage)recv.getObject();
		assertEquals("Mojn mojn", msgRecv.getMessage());
		
		client.stop();
		server.stop();
		connListener.close();
	}

	@After
	public void tearDown() throws Exception {
	}
	
	private class DummyConnectionHandler implements ConnectionHandler {
		public SSLSocket lastSocket;
		
		public void incomingConnection(SSLSocket socket) {
			System.out.println("DummyConnectionHandler.incomingConnection(" + socket + ")");
			lastSocket = socket;
			server.setSocket(socket);
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		new PortTest().testConnectionCreation();
	}
}
