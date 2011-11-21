package dk.au.daimi.tandrup.MPC.net.ssltest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import dk.au.daimi.tandrup.MPC.net.SecurityManager;

public class SSLTestClient {
	public static void main(String[] args) throws GeneralSecurityException, IOException {
		String strServerName = "localhost"; // SSL Server Name
		int intSSLport = 4443; // Port where the SSL Server is listening
		PrintWriter out = null;
		BufferedReader in = null;

		SSLContext sslContext = SecurityManager.getSSLContext("user1.store");

		try {
			// Creating Client Sockets
			//SSLSocketFactory sslsocketfactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocketFactory sslsocketfactory = sslContext.getSocketFactory();
			SSLSocket sslSocket = (SSLSocket)sslsocketfactory.createSocket(strServerName,intSSLport);

			// Initializing the streams for Communication with the Server
			out = new PrintWriter(sslSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));

			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			String userInput = "Hello Testing ";
			out.println(userInput);

			while ((userInput = stdIn.readLine()) != null) {
				out.println(userInput);
				System.out.println("echo: " + in.readLine());
			}

			out.println(userInput);

			// Closing the Streams and the Socket
			out.close();
			in.close();
			stdIn.close();
			sslSocket.close();
		}

		catch(Exception exp)
		{
			System.out.println(" Exception occured .... " +exp);
			exp.printStackTrace();
		}
	}

}
