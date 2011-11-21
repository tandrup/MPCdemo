package dk.au.daimi.tandrup.MPC.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import dk.au.daimi.tandrup.MPC.net.SecurityManager;

public class ClientPort extends AbstractPort {
	private InetAddress remoteAddress;
	private int remotePort;

	public ClientPort(InetAddress remoteAddress, int remotePort, KeyStore keyStore, SSLCommunicationChannel channel) throws NoSuchAlgorithmException {
		super(keyStore, channel);
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
	}

	private synchronized void openSocket() {
		logger.entering(this.getClass().getName(), "openSocket()");

		disconnected();
		
		if (!running)
			return;

		try {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException ex) {
					logger.severe("Error closing unused socket: " + ex);
				}
			}

			socket = null;

			while (socket == null || !socket.isConnected()) {
				try {
					SSLContext sslContext = SecurityManager.getSSLContext(keyStore);
					socket = (SSLSocket)sslContext.getSocketFactory().createSocket(remoteAddress, remotePort);
					socket.startHandshake();
				} catch (IOException ex) {
					logger.severe("Error opening socket: " + ex);
					Thread.sleep(1000);
				} catch (GeneralSecurityException ex) {
					logger.severe("Error opening socket: " + ex);
					Thread.sleep(1000);
				}
			}

			connected();
		
		} catch (InterruptedException ex) {
			logger.severe("Interrupted while opening socket: " + ex);
		}
	}

	public void start() {
		running = true;
		openSocket();
		receiver = new Thread(new ReceiverThread(), "ReceiverThread " + remoteAddress + ":" + remotePort);
		sender = new Thread(new SenderThread(), "SenderThread " + remoteAddress + ":" + remotePort);
		receiver.start();
		sender.start();
	}

	public void stop() {
		running = false;

		if (receiver != null)
			receiver.interrupt();
		if (sender != null)
			sender.interrupt();

		try {
			if (socket != null)
				socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class ReceiverThread implements Runnable {
		public void run() {
			try {
				while (running) {
					try {
						receiverLoop();
					} catch (IOException ex) {
						socket = null;
						openSocket();
					}
				}
			} catch (Exception ex) {
				logger.throwing(this.getClass().getName(), "run()", ex);
			}
		}
	}

	private class SenderThread implements Runnable {
		public void run() {
			try {
				while (running) {
					try {
						senderLoop();
					} catch (IOException ex) {
						socket = null;
						openSocket();
					}
				}
			} catch (Exception ex) {
				logger.throwing(this.getClass().getName(), "run()", ex);
			}
		}
	}
}
