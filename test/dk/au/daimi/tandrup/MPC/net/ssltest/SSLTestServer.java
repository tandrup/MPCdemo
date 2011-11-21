package dk.au.daimi.tandrup.MPC.net.ssltest;

import java.io.*;
import java.security.PrivilegedActionException;

import javax.net.ssl.*;

import dk.au.daimi.tandrup.MPC.net.SecurityManager;

public class SSLTestServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int intSSLport = 4443; // Port where the SSL Server needs to listen for new requests from the client

		/*
		{
			// Registering the JSSE provider
			Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

			//Specifying the Keystore details
	        //System.setProperty("javax.net.debug", "all");
			System.setProperty("javax.net.ssl.trustStore","server1.store");
			System.setProperty("javax.net.ssl.trustStorePassword","secret");
			System.setProperty("javax.net.ssl.keyStore","server1.store");
			System.setProperty("javax.net.ssl.keyStorePassword","secret");

			// Enable debugging to view the handshake and communication which happens between the SSLClient and the SSLServer
			// System.setProperty("javax.net.debug","all");
		}*/

		try {
			SSLContext sslContext = SecurityManager.getSSLContext("user1.store");

			// Initialize the Server Socket
			SSLServerSocketFactory sslServerSocketfactory = sslContext.getServerSocketFactory();
			SSLServerSocket sslServerSocket = (SSLServerSocket)sslServerSocketfactory.createServerSocket(intSSLport);
			SSLSocket sslSocket = (SSLSocket)sslServerSocket.accept();

			// Create Input / Output Streams for communication with the client
			while(true)
			{
				PrintWriter out = new PrintWriter(sslSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(
						new InputStreamReader(
								sslSocket.getInputStream()));
				String inputLine;

				while ((inputLine = in.readLine()) != null) {
					out.println(inputLine);
					System.out.println(inputLine);
				}

				// Close the streams and the socket
				out.close();
				in.close();
				sslSocket.close();
				sslServerSocket.close();

			}
		}


		catch(Exception exp)
		{
			PrivilegedActionException priexp = new PrivilegedActionException(exp);
			System.out.println(" Priv exp --- " + priexp.getMessage());

			System.out.println(" Exception occured .... " +exp);
			exp.printStackTrace();
		}

	}

}
