package dk.au.daimi.tandrup.MPC.net.ssl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;

import dk.au.daimi.tandrup.MPC.net.SecurityManager;

public class ConnectionListener {
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private boolean running;
	private Thread listener;
	private SSLServerSocket server;
	private ConnectionHandler connHandler;

	public ConnectionListener(int port, KeyStore store, ConnectionHandler connHandler) throws IOException, GeneralSecurityException {
		SSLContext sslContext = SecurityManager.getSSLContext(store);
		server = (SSLServerSocket)sslContext.getServerSocketFactory().createServerSocket(port);
		
		// Require client cert.
		server.setNeedClientAuth(true);
		
		this.connHandler = connHandler;
		running = true;
		listener = new Thread(new ListenerThread(), "ConnectionListener " + connHandler);
		listener.start();
	}
	
	public void close() throws IOException {
		running = false;
		listener.interrupt();
		server.close();
	}

	private class ListenerThread implements Runnable {
		public void run() {
			try {
				while (running) {
					try {
						SSLSocket s = (SSLSocket)server.accept();
						logger.fine("Received connection from " + s.getRemoteSocketAddress() + " starting SSL handshake");
						s.startHandshake();
						logger.fine("Handshake with " + s.getRemoteSocketAddress() + " completed");
						connHandler.incomingConnection(s);
					} catch (IOException ex) {
						logger.throwing(this.getClass().getName(), "run", ex);
						Thread.sleep(1000);
					} catch (RuntimeException ex) {
						logger.throwing(this.getClass().getName(), "run", ex);
						throw ex;
					}
				}
			} catch (InterruptedException ex) {
				logger.throwing(this.getClass().getName(), "run", ex);
			}
		}
	}
}
