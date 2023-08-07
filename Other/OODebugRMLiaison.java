import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class OODebugRMLiaison {
	private Socket clientSocket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	private OODebugRMLiaison(Socket clientSocket, ObjectOutputStream out, ObjectInputStream in) {
		this.clientSocket = clientSocket; this.out = out; this.in = in;
	}

	/**
	 * Creates resource manager liaison threads given a host and port.
	 * @return Flyweight instance.
	 */
	public static void main(String[] args) {
		String serverHost = args[0];
		int serverPort = Integer.parseInt(args[1]);


		// Connect to specified server (RM)
		Socket clientSocket;
		ObjectOutputStream out;
		ObjectInputStream in;
		OODebugRMLiaison liaison = null;
		try {
			boolean first = true;
			while (true) {
				try {
					clientSocket = new Socket();
					clientSocket.setSoTimeout(1000);
					clientSocket.connect(new InetSocketAddress(serverHost, serverPort));
					/* Write into this to send to the Server's buffer. */
					out = new ObjectOutputStream(clientSocket.getOutputStream());
					/* Read from this for the server's replies. */
					in = new ObjectInputStream(clientSocket.getInputStream());
					liaison = new OODebugRMLiaison(clientSocket, out, in);
					break;
				}
				// UnknownHostException is subclass of IOException.
				// Idk if other non fatal exceptions should be here
				catch (IOException e) {
					if (first) {
						System.out.println("Waiting for [" + serverHost + ":" + serverPort + "]");
						first = false;
					}
				}
			}
		} catch (Exception e) {
			System.err.println((char)27 + "[31;1mLiaisonFactory exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		// Recreate the world
		liaison.sendMessage("addflight", 0, 1,3,300);
		liaison.sendMessage("addflight", 0, 2,2,300);
		liaison.sendMessage("addflight", 0, 3,10,100);
		liaison.sendMessage("addflight", 0, 4,3,150);
		liaison.sendMessage("addflight", 0, 5,1,300);
		liaison.sendMessage("addcars", 0, "montreal",10,50);
		liaison.sendMessage("addcars", 0, "toronto",20,75);
		liaison.sendMessage("addcars", 0, "edmonton",3,30);
		liaison.sendMessage("addcars", 0, "vancouver",15,35);
		liaison.sendMessage("addcars", 0, "ottawa",2,40);
		liaison.sendMessage("addcars", 0, "halifax",1,20);
		liaison.sendMessage("addrooms", 0, "montreal",10,200);
		liaison.sendMessage("addrooms", 0, "toronto",30,500);
		liaison.sendMessage("addrooms", 0, "edmonton",5,175);
		liaison.sendMessage("addrooms", 0, "vancouver",4,230);
		liaison.sendMessage("addrooms", 0, "ottawa",3,250);
		liaison.sendMessage("addrooms", 0, "halifax",1,100);
		liaison.sendMessage("newcustomer2", 0, 1);
		liaison.sendMessage("newcustomer2", 0, 2);
		liaison.sendMessage("newcustomer2", 0, 3);
		liaison.sendMessage("newcustomer2", 0, 4);
		liaison.sendMessage("newcustomer2", 0, 5);

		// Test the methods
		liaison.sendMessage("queryFlight", 0, 5);
		liaison.sendMessage("queryFlightPrice", 0, 5);
		liaison.sendMessage("deleteFlight", 0, 5);
		liaison.sendMessage("queryFlight", 0, 5);
		liaison.sendMessage("queryFlightPrice", 0, 5);




		System.out.println("Test over, shutting down");


	}


	/**
	 * Utility method to send a message.
	 */
	public Vector<Object> sendMessage(Object... args) {
		Vector<Object> v = new Vector<>();
		v.add(Integer.MIN_VALUE);
		v.addAll(List.of(args));
		return sendMessage(v);
	}

	/** Sends a message to a resource manager and returns the output.
	 * Currently "blocking" since it has to wait for a reply rather than
	 * immediately starting to receive again. */
	@SuppressWarnings("unchecked")
	synchronized public Vector<Object> sendMessage(Vector<Object> msg) {
		Vector<Object> ret = null;
		try {
			System.out.printf("Sending over: %s\n", msg);
			out.writeObject(msg);
			ret = (Vector<Object>) in.readObject();
			System.out.printf("Received: %s\n\n", ret);
			return ret;
		} catch (Exception e) {
			System.err.println("Network error, RM might be down. Shutdown requested.");
			System.exit(1);
		}
		return ret;
	}
}