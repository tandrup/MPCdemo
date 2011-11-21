package dk.au.daimi.tandrup.MPC.net.ssl;

import java.io.IOException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;


public class ServerPort extends AbstractPort {
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	public ServerPort(KeyStore keyStore, SSLCommunicationChannel channel) throws NoSuchAlgorithmException {
		super(keyStore, channel);
	}

	public ServerPort(KeyStore keyStore, SSLCommunicationChannel channel, SSLSocket socket) throws NoSuchAlgorithmException {
		super(keyStore, channel);
		this.socket = socket;
	}

	public void start() {
		running = true;
		sender = new Thread(new SenderThread(), "SenderThread");
		receiver = new Thread(new ReceiverThread(), "ReceiverThread");
		receiver.start();
		sender.start();
	}

	public void stop() {
		running = false;
		sender.interrupt();
		receiver.interrupt();
	}

	public synchronized void setSocket(SSLSocket socket) {
		logger.info("ServerPort.setSocket()");
		this.socket = socket;
		notifyAll();
	}

	private synchronized void waitForNewSocket() throws InterruptedException {
		logger.entering(this.getClass().getName(), "waitForNewSocket");
		disconnected();
		while (socket == null) {
			wait();
		}
		connected();
		logger.exiting(this.getClass().getName(), "waitForNewSocket");
	}

	private class ReceiverThread implements Runnable {
		public void run() {
			try {
				while (running) {
					try {
						receiverLoop();
					} catch (IOException ex) {
						socket = null;
						waitForNewSocket();
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
						waitForNewSocket();
					}
				}
			} catch (Exception ex) {
				logger.throwing(this.getClass().getName(), "run()", ex);
			}
		}
	}
}
