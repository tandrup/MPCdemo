package dk.au.daimi.tandrup.MPC.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import dk.au.daimi.tandrup.MPC.net.ssl.ConnectionHandler;
import dk.au.daimi.tandrup.MPC.net.ssl.ConnectionListener;

public class SSLPerformanceTest implements ConnectionHandler{
	private static final int CLIENTS = 50;
	private static final int PORT = 7958;
	private static final int BUFFER_SIZE = 1024*8;
	private static final int REPEAT_BUFFER = 20;
	
	private List<Double> transferRates = 
		Collections.synchronizedList(new ArrayList<Double>());

	private List<SSLSocket> clientSockets = 
		Collections.synchronizedList(new ArrayList<SSLSocket>());

	private List<SSLSocket> serverSockets = 
		Collections.synchronizedList(new ArrayList<SSLSocket>());

	private List<OutputStream> outStreams = 
		Collections.synchronizedList(new ArrayList<OutputStream>());

	private List<InputStream> inStreams = 
		Collections.synchronizedList(new ArrayList<InputStream>());

	protected synchronized void addTransferRate(double rate) {
		transferRates.add(rate);
		notify();
	}
	
	public void runTest() throws Exception {
		KeyStore keyStore = SecurityManager.getJavaKeyStoreFromFile("server1.store");
		
		SSLContext sslContext = SecurityManager.getSSLContext(keyStore);
		
		ConnectionListener connListener = new ConnectionListener(PORT, keyStore, this);
		
		System.out.println("Opening clients");

		for (int i = 0; i < CLIENTS; i++) {
			SSLSocket clientSocket = (SSLSocket)sslContext.getSocketFactory().createSocket(InetAddress.getByName("localhost"), PORT);
			clientSocket.startHandshake();
			clientSockets.add(clientSocket);
			inStreams.add(clientSocket.getInputStream());
			outStreams.add(clientSocket.getOutputStream());

			System.out.println(clientSocket.getSession().getCipherSuite());

			Thread.sleep(0);
			System.out.print(".");
			if ((i+1) % 80 == 0)
				System.out.println(" " + (i+1));
		}
		System.out.println(" Done!");
		
		Thread.sleep(500);
		
		System.out.println(InetAddress.getByName("localhost"));
		
		Sender sender = new Sender();
		Receiver receiver = new Receiver();
		
		sender.start();
		receiver.start();
		
		sender.join();
		receiver.join();
		
		System.out.println("Done");
		
		connListener.close();
	}
	
	public void incomingConnection(SSLSocket serverSocket) {
		try {
			serverSockets.add(serverSocket);
			inStreams.add(serverSocket.getInputStream());
			outStreams.add(serverSocket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		SSLPerformanceTest me = new SSLPerformanceTest();
		me.runTest();
	}
	
	private class Sender extends Thread {
		public void run() {
			byte[] buf = new byte[BUFFER_SIZE];
			new Random().nextBytes(buf);
			
			long byteCount = 0;
			
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < REPEAT_BUFFER; i++) {
				synchronized (outStreams) {
					Iterator<OutputStream> iter = outStreams.iterator();
					while (iter.hasNext()) {
						OutputStream out = iter.next();
						try {
							out.write(buf);
							byteCount += buf.length;
							if (i == REPEAT_BUFFER - 1)
								out.flush();
							Thread.sleep(0);
						} catch (Exception e) {
							e.printStackTrace();
						}					
					}
				}
			}
			long stopTime = System.currentTimeMillis();
			
			double rate = (double)byteCount / (stopTime - startTime);
			System.out.println("Send rate: " + rate);
		}
	}

	private class Receiver extends Thread {
		public void run() {
			byte[] buf = new byte[BUFFER_SIZE];
			long byteCount = 0;
			
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < REPEAT_BUFFER; i++) {
				synchronized (inStreams) {
					Iterator<InputStream> iter = inStreams.iterator();
					while (iter.hasNext()) {
						InputStream in = iter.next();
						try {
							byteCount += in.read(buf);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			long stopTime = System.currentTimeMillis();
			
			double rate = (double)byteCount / (stopTime - startTime);
			System.out.println("Receive rate: " + rate);

			synchronized (inStreams) {
				Iterator<InputStream> iter = inStreams.iterator();
				while (iter.hasNext()) {
					InputStream in = iter.next();
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
