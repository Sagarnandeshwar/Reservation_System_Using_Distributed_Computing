package Server.TCP;

import Server.Common.Trace;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Vector;

/**
 * Utility thread that communicates with a given resource manager.
 */
public class TCPRMLiaison extends Thread {
	private Socket clientSocket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	/**
	 * Creates resource manager liaison threads given a host and port.
	 */
	public TCPRMLiaison(String serverHost, int serverPort, String threadName) {
		setName(threadName);
		// Connect to specified server (Resource Manager)
		try {
			boolean first = true;
			while (true) {
				try {
					clientSocket = new Socket();
					clientSocket.connect(new InetSocketAddress(serverHost, serverPort));
					/* Write into this to send to the Server's buffer. */
					out = new ObjectOutputStream(clientSocket.getOutputStream());
					/* Read from this for the server's replies. */
					in = new ObjectInputStream(clientSocket.getInputStream());
					break;
				}
				// UnknownHostException is subclass of IOException.
				// Idk if other non fatal exceptions should be here
				catch (IOException ignored) {
					if (first) {
						System.out.println("Waiting for resource manager at [" + serverHost + ":" + serverPort + "]");
						first = false;
					}
					// This will fail here if RM's aren't started up yet.
					Thread.sleep(500);
				}
			}
		} catch (Exception e) {
			System.err.println((char)27 + "[31;1mLiaisonFactory exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Connection successful!");
	}

	/** Takes any and all responses it gets from the RM and forwards them to
	 * the appropriate client liaison thread. */
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		// Create shutdown hook.
		Runtime.getRuntime().addShutdownHook(new Thread( () -> {
			Trace.info("Shutting down RM liaison");
			try {
				if (clientSocket != null) clientSocket.close();
			} catch (Exception ignored) {}
		}));

		while (true) {
			try {
				// Take any response and give it to the appropriate thread's
				// queue, which should unblock it.
				Vector<Object> response =  (Vector<Object>) in.readObject();
				int originThread = (int) response.get(0);
				Trace.info("Received: %s. Forwarding to thread #%d", response, originThread);
				synchronized (TCPMiddleware.clientLiaisonMap) {
					TCPMiddleware.clientLiaisonMap.get(originThread).put(response); // FIXME broken
				}

			} catch (Exception e) {
				Trace.error("Problem connecting to MW. Shutting down.");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	/**
	 * Sends a message to a resource manager. This is only expected to be called
	 * from {@link TCPMiddleware.TCPClientLiaison} for the purpose
	 * of sending a message. Furthermore, the calling ClientLiaison is expected
	 * to block by trying to retrieve the response from its queue.
	 * */
	synchronized public void sendMessage(Vector<Object> msg) {
		try {
			out.writeObject(msg);
		} catch (Exception e) {
			System.err.println("Network error, RM might be down. Shutdown requested.");
			System.exit(1);
		}
	}
}