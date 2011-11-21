package dk.au.daimi.tandrup.MPC.net;

import java.net.InetAddress;
import java.security.cert.X509Certificate;

public interface Participant extends Comparable<Participant> {
	public InetAddress getInetAddress();
	public int getPort();
	public X509Certificate getCertificate();
	public String getCertificateSubjectDN();
	public int getID();
	public boolean equals(Participant other);
}
