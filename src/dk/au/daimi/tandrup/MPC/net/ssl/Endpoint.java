package dk.au.daimi.tandrup.MPC.net.ssl;

import java.net.InetAddress;

public class Endpoint {
	private InetAddress address;
	private int port;
	private String certificateSubjectDN;
	
	public Endpoint() {
		super();
	}
	
	public Endpoint(InetAddress address, int port, String certificateSubjectDN) {
		super();
		this.address = address;
		this.port = port;
		this.certificateSubjectDN = certificateSubjectDN;
	}

	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public String getCertificate() {
		return certificateSubjectDN;
	}

	public void setCertificate(String certificateSubjectDN) {
		this.certificateSubjectDN = certificateSubjectDN;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
