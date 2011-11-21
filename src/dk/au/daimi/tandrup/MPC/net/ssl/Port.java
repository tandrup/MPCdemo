package dk.au.daimi.tandrup.MPC.net.ssl;

import java.security.cert.X509Certificate;


public interface Port {
	public boolean isConnected();
	public void setReceiveQueue(SignedMessageQueue queue);
	public void sendMessage(SignedMessage msg);
	public void start();
	public void stop();
	public X509Certificate getCertificate();
	public X509Certificate[] getCertificateChain();
}
