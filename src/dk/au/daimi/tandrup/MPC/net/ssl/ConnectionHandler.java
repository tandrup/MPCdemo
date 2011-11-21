package dk.au.daimi.tandrup.MPC.net.ssl;

import javax.net.ssl.SSLSocket;

public interface ConnectionHandler {
	public void incomingConnection(SSLSocket socket);
}
