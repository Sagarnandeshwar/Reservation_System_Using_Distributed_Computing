package Server.TCP;

import Server.Common.ResourceManager;
import Server.Common.Trace;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Vector;

/**
 * ResourceManager implemented as a server using TCP to communicate with the
 * middleware. This resource manager translates deserialized vectors into
 * {@link ResourceManager} calls and translates back its output into serialized
 * vectors to be sent back to the middleware.
 * @author Aidan Gomar, Yiwei Xiong, Ye Tong Zhou
 */
public class TCPResourceManager extends ResourceManager {

	/** Port of the middleware listener. Defaults to 7050 but should be:
	 * 7051 for Flights, 7052 for Cars, 7053 for Rooms */
	private static int serverPort = 7050;
	/** Own socket to connect to the middleware. */
	public static ServerSocket serverSocket;
	/** Socket obtained from the middleware. */
	public static Socket clientSocket;
	/** Use to send response objects to the middleware. */
	public static ObjectOutputStream mwOut;
	/** Use to receive request objects from the middleware. */
	public static ObjectInputStream mwIn;

	public TCPResourceManager(String name) {
		super(name);
	}

	/**
	 * Starts the main thread. Usage: TCPResourceManager [serverPort] [name]
	 * @param args [serverPort] [name]
	 */
	public static void main(String[] args) {
		// Argument processing
		if (args.length > 0) {
			try {
				serverPort = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				Trace.error("Unable to parse specified port, defaulting to %d.\n", serverPort);
				System.exit(1);
			}
		}
		String rmName = "DefaultTCPRM";
		if (args.length > 1) rmName = args[1];

		Trace.info("Starting resource manager \"%s\" on port %d\n", rmName, serverPort);

		// Try to initialize connection with the middleware.
		try {
			Trace.info("Waiting for MiddleWare at port: " + serverPort + "...");
			serverSocket = new ServerSocket(serverPort);
			clientSocket = serverSocket.accept();
			mwOut = new ObjectOutputStream(clientSocket.getOutputStream());
			mwIn = new ObjectInputStream(clientSocket.getInputStream());
		} catch (Exception e) {
			Trace.error((char)27 + "[31;1mResourceManager exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		Trace.info("Middleware connected! Starting resource manager...");

		// Adding shutdown script.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.err.printf("[%s]: Shutting down", Thread.currentThread().getName());
			try {
				System.err.println("Trying to shutdown client socket.");
				if (clientSocket != null) clientSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				System.err.println("Trying to shutdown server socket.");
				if (serverSocket != null) serverSocket.close(); // Also closes streams
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));

		// Creating a ResourceManager and running it.
		TCPResourceManager manager = new TCPResourceManager(rmName);
		manager.runResourceManager();
	}


	/**
	 * Main loop that reads requests from the input stream and sends responses
	 * to the output stream.
	 */
	@SuppressWarnings("unchecked")
	public void runResourceManager() {
		// Watch the while for connection issues.
		try {
			while (true) {
				int threadNum = Integer.MAX_VALUE;
				try {
					// Processing request
					Vector<Object> inputs = (Vector<Object>) mwIn.readObject(); // Block here
					Trace.info("Received: " + inputs);
					threadNum = (Integer) inputs.get(0);
					String methodName = (String) inputs.get(1);

					/* Translating vector elements into arguments by casting
					 * (starting index 2). Although each method call can
					 * theoretically result in an RMIException, we know they won't
					 * since they are all local method calls. Still, they will be
					 * caught and relayed to the Client should they happen. */
					Object outputObject;

					// Easy methods involving one type of resources only.
					if (methodName.equalsIgnoreCase( "addFlight" )) {
						outputObject = super.addFlight((int) inputs.get(2), (int) inputs.get(3), (int) inputs.get(4), (int) inputs.get(5));
					}
					else if (methodName.equalsIgnoreCase("addCars")) {
						outputObject = super.addCars((int) inputs.get(2), (String) inputs.get(3), (int) inputs.get(4), (int) inputs.get(5));
					}
					else if (methodName.equalsIgnoreCase("addRooms")) {
						outputObject = super.addRooms((int) inputs.get(2), (String) inputs.get(3), (int) inputs.get(4), (int) inputs.get(5));
					}
					else if (methodName.equalsIgnoreCase("deleteFlight")) {
						outputObject = super.deleteFlight((int) inputs.get(2), (int) inputs.get(3));
					}
					else if (methodName.equalsIgnoreCase("deleteCars")) {
						outputObject = super.deleteCars((int) inputs.get(2), (String) inputs.get(3));
					}
					else if (methodName.equalsIgnoreCase("deleteRooms")) {
						outputObject = super.deleteRooms((int) inputs.get(2), (String) inputs.get(3));
					}
					else if (methodName.equalsIgnoreCase("deleteCustomer")) {
						outputObject = super.deleteCustomer((int) inputs.get(2), (int) inputs.get(3));
					}
					else if (methodName.equalsIgnoreCase("queryFlight")) {
						outputObject = super.queryFlight((int) inputs.get(2), (int) inputs.get(3));
					}
					else if (methodName.equalsIgnoreCase("queryCars")) {
						outputObject = super.queryCars((int) inputs.get(2), (String) inputs.get(3));
					}
					else if (methodName.equalsIgnoreCase("queryRooms")) {
						outputObject = super.queryRooms((int) inputs.get(2), (String) inputs.get(3));
					}
					else if (methodName.equalsIgnoreCase("queryFlightPrice")) {
						outputObject = super.queryFlightPrice((int) inputs.get(2), (int) inputs.get(3));
					}
					else if (methodName.equalsIgnoreCase("queryCarsPrice")) {
						outputObject = super.queryCarsPrice((int) inputs.get(2), (String) inputs.get(3));
					}
					else if (methodName.equalsIgnoreCase("queryRoomsPrice")) {
						outputObject = super.queryRoomsPrice((int) inputs.get(2), (String) inputs.get(3));
					}

					// Middleware utility methods
					else if (methodName.equalsIgnoreCase("reserveItem")) {
						outputObject = super.reserveItem((int) inputs.get(2), (String) inputs.get(3));
					}
					else if (methodName.equalsIgnoreCase("cancelItem")) {
						outputObject = super.cancelItem((int) inputs.get(2), (String) inputs.get(3), (int) inputs.get(4));
					}
					else if (methodName.equalsIgnoreCase("reserveFlights")) {
						outputObject = super.reserveFlights((int) inputs.get(2), (int) inputs.get(3), (int[]) inputs.get(4));
					}



					// The following methods should NOT be called on RMs under
					// any condition. They should only be invoked on the
					// middleware as they involve clients or messing with more
					// than one type of resource (aka they cannot complete
					// relying entirely on local code).
					else if (methodName.equalsIgnoreCase("newCustomer1")) {
						outputObject = super.newCustomer((int) inputs.get(2));
					}
					else if (methodName.equalsIgnoreCase("newCustomer2")) {
						outputObject = super.newCustomer((int) inputs.get(2), (int) inputs.get(3) );
					}
					else if (methodName.equalsIgnoreCase("queryCustomerInfo")) {
						outputObject = super.queryCustomerInfo((int) inputs.get(2), (int) inputs.get(3));
					}
					else if (methodName.equalsIgnoreCase("reserveFlight")) {
						outputObject = super.reserveFlight((int) inputs.get(2), (int) inputs.get(3), (int) inputs.get(4));
					}
					else if (methodName.equalsIgnoreCase("reserveCar")) {
						outputObject = super.reserveCar((int) inputs.get(2), (int) inputs.get(3), (String) inputs.get(4));
					}
					else if (methodName.equalsIgnoreCase("reserveRoom")) {
						outputObject = super.reserveRoom((int) inputs.get(2), (int) inputs.get(3), (String) inputs.get(4));
					}
					else if (methodName.equalsIgnoreCase("bundle")) {
						outputObject = super.bundle((int) inputs.get(2), (int) inputs.get(3), (Vector<String>) inputs.get(4),
								(String) inputs.get(5), (boolean) inputs.get(6), (boolean) inputs.get(7));
					}

					// Unknown method called. Should not happen.
					else {
						throw new RuntimeException("Unknown command From Middleware: " + methodName);
					}

					Vector<Object> returnVector = new Vector<>(Arrays.asList(threadNum, outputObject, false));
					mwOut.writeObject(returnVector);
					Trace.info("Wrote: " + returnVector + '\n');
				}
				// RemoteException caught, notify MW of failure.
				catch (RemoteException e) {
					Vector<Object> returnVector = new Vector<>(Arrays.asList(threadNum, null, true));
					mwOut.writeObject(returnVector);
					Trace.info("Wrote: " + returnVector + '\n');
				}
			}

		} catch (IOException e) { // Connection error. Shutdown.
			Trace.error("Middleware disconnected from server (IOException): thread shutdown ordered.");
			System.exit(1);
		} catch (Exception e) {
			Trace.error("Error while running RM: thread shutdown ordered.");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
