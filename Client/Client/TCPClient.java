package Client;

import java.net.*;
import java.io.*;

/**
 * Client using TCP to communicate. To simplify the amount of code to change,
 * {@link Client#m_resourceManager} was replaced from an RMI remote object to
 * a custom {@link TCPIResourceManagerWrapper} that converts all method calls
 * into a serialized vector being sent over TCP
 * @author Aidan Gomar, Yiwei Xiong, Ye Tong Zhou
 */
public class TCPClient extends Client{

	/** Hostname of the machine running the Middleware. */
	private static String serverHost = "localhost";
	/** Port to the Middleware. */
	private static int serverPort = 7050;

	private static Socket clientSocket;
	private static ObjectOutputStream out;
	private static ObjectInputStream in;


	@Override
	public void connectServer() {
		try {
			boolean first = true;
			while (true) {
				try {
					/* Socket this client is using to connect to a server.
					 * Should be fine to run more than one client at once */
					clientSocket = new Socket();
					clientSocket.connect(new InetSocketAddress(serverHost, serverPort));
					/* Write into this to send to the Server's buffer. */
					out = new ObjectOutputStream(clientSocket.getOutputStream());
					/* Read from this for the server's replies. */
					in = new ObjectInputStream(clientSocket.getInputStream());
					m_resourceManager = new TCPIResourceManagerWrapper(out, in);
					break;
				}
				// UnknownHostException is subclass of IOException.
				// Idk if other non fatal exceptions should be here
				catch (IOException e) {
					if (first) {
						System.out.println("Waiting for [" + serverHost + ":" + serverPort + "]");
						first = false;
					}
					Thread.sleep(500);
				}
			}
		} catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

	}

	public static void main(String[] args) {

		// Argument processing
		if (args.length > 0) serverHost = args[0];
		if (args.length > 1) {
			try {
				serverPort = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				System.err.printf("Unable to parse specified port, defaulting to %d.\n", serverPort);
			}
		}
		if (args.length > 2) {
			System.err.println("Too many arguments");
			System.exit(1);
		}

		System.out.printf("Starting client with middleware at [%s, %d]\n", serverHost, serverPort);

		// Adding shutdown script.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.err.println("Shutting down");
			try {
				System.err.println("Trying to shutdown client socket.");
				if (clientSocket != null) clientSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));

		try {
			// Starting an instance of the client.
			TCPClient client = new TCPClient();
			client.connectServer();
			client.start();
		} catch (Exception e) {
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

	}

}
