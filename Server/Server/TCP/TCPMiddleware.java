package Server.TCP;

import Server.Common.Car;
import Server.Common.Customer;
import Server.Common.Flight;
import Server.Common.Middleware;
import Server.Common.RMHashMap;
import Server.Common.ReservedItem;
import Server.Common.Room;
import Server.Common.Trace;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class TCPMiddleware extends Middleware {

	public static TCPMiddleware instance;

	/** Single server socket to accept incoming client connection requests. */
	private static ServerSocket serverSocket;

	/** Default addresses of the hosts for the resource managers. */
	private static String flightRMHost = "localhost", carRMHost = "localhost", roomRMHost = "localhost";
	/** Default ports for the resource managers. (Different in case of same host) */
	private static final int flightRMPort = 7051, carRMPort = 7052, roomRMPort = 7053;

	/** Liaison threads to actually communicate with the resource managers. */
	private static TCPRMLiaison flightLiaison, carLiaison, roomLiaison;

	/** FIXME This will probably be unused. */
	public static final ConcurrentHashMap<Integer, BlockingQueue<Object>> clientLiaisonMap = new ConcurrentHashMap<>();

	private TCPMiddleware(String p_name) {
		super(p_name);
	}

	/**
	 * The main middleware thread does little other than:
	 * 1. Connect to the resource managers.
	 * 2. Loop and spawn client liaison threads for incoming clients
	 * */
	public static void main(String[] args) {

		// Creating static instance to let threads use its interface methods.
		instance = new TCPMiddleware("Middleware");

		// Adding shutdown script.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			Trace.error("[%s]: Shutting down middleware main thread\n", Thread.currentThread().getName());
			try {
				Trace.error("Trying to shutdown middleware main server socket.");
				if (serverSocket != null) serverSocket.close(); // Also closes streams
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));

		// Parsing arguments
		if (args.length > 0) {
			if (args.length != 3) {
				Trace.error("Usage: [flightHost] [carHost] [roomHost]");
				System.exit(1);
			}
			flightRMHost = args[0];
			carRMHost = args[0];
			roomRMHost = args[0];
		}

		/* Connecting to the resource managers. Each constructor is blocking
		* while waiting to connect. */
		flightLiaison = new TCPRMLiaison(flightRMHost, flightRMPort, "FlightLiaison");
		carLiaison = new TCPRMLiaison(carRMHost, carRMPort, "CarLiaison");
		roomLiaison = new TCPRMLiaison(roomRMHost, roomRMPort, "RoomLiaison");
		flightLiaison.start();
		carLiaison.start();
		roomLiaison.start();

		// Spawn & store client liaisons in a loop.
		try {
			serverSocket = new ServerSocket(7050); // Hardcode
			int numThreads = 1;
			while (true) {
				try {
					// Blocks here waiting for new clients
					Trace.info("Waiting for client...");
					Socket newClientSocket = serverSocket.accept();
					Trace.info("Connected! Launching new liaison thread.");

					// Creating a new liaison thread for new client and register
					// its receiving queue.
					BlockingQueue<Object> q = new ArrayBlockingQueue<>(1);
					TCPClientLiaison newLiaison = new TCPClientLiaison(numThreads, newClientSocket, q);
					clientLiaisonMap.put(numThreads, q);
					numThreads++;
					newLiaison.start();
				} catch (IOException e) {
					Trace.warn("Failed to connect to a potential client");
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			Trace.error("Fatal exception: Shutting down");
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * Sends a message to the appropriate RM via its RM liaison and block
	 * while waiting for a response. Only this client liaison thread will
	 * block, not the RM receiving thread nor the middleware main.
	 * @param v Message to send to the RM, with threadID added to index 0.
	 * @param destination Strings for "flight", "car", "room"
	 */
	@SuppressWarnings("unchecked")
	public static Vector<Object> sendMessage(Vector<Object> v, String destination, int threadID) {
		// Format message with ThreadID added
		Vector<Object> forwardedMessage = new Vector<>(v);
		forwardedMessage.add(0, threadID);
		Trace.info("Sending to %s: %s", destination, v);

		// Actually sending (each RM liaison synchronized).
		if (destination.equalsIgnoreCase("flight")) {
			flightLiaison.sendMessage(forwardedMessage);
		}
		else if (destination.equalsIgnoreCase("car")) {
			carLiaison.sendMessage(forwardedMessage);
		}
		else if (destination.equalsIgnoreCase("room")) {
			roomLiaison.sendMessage(forwardedMessage);
		}

		/* Getting the response (blocking until RM liaison thread deposits
		 * the response object into the queue). */
		Vector<Object> response;
		try {
			// Recall [threadID, returnValue, isRemoteException]
			response = (Vector<Object>) TCPMiddleware.clientLiaisonMap.get(threadID).take();
			response.remove(0); // Discard ThreadID since no longer needed.
			Trace.info("Received response: %s", response);
			return response;
		} catch (Exception e) {
			Trace.error("Error while getting response from RM. Shutting down.");
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		throw new RuntimeException("Unreachable part of code reached."); // This should not happen
	}

	/**
	 * Test class to test the shutdown hook behavior of a worker thread.
	 */
	static class TCPClientLiaison extends Thread implements Runnable {

		private final Socket clientSocket;
		private ObjectInputStream in;
		private ObjectOutputStream out;

		/**
		 * Let MW receiving threads deposit RM responses into this queue.
		 */
		private final BlockingQueue<Object> receivingQueue;
		int id = 0;

		/**
		 * Create a new Client liaison thread that will interact with the client
		 * connecting through the given socket. Does not start it.
		 *
		 * @param clientSocket Socket a new client is using.
		 */
		public TCPClientLiaison(int id, Socket clientSocket, BlockingQueue<Object> receivingQueue) {
			super("CL#" + id);
			this.id = id;
			this.clientSocket = clientSocket;
			this.receivingQueue = receivingQueue;
		}

		/**
		 * This method should only be used by the MW RM Liaison receiving
		 * thread to deposit response messages.
		 *
		 * @param response Response from this thread's request.
		 * @throws InterruptedException In case of failure.
		 */
		public synchronized void receive(Vector<Object> response) throws InterruptedException {
			this.receivingQueue.put(response);
		}

		@SuppressWarnings("unchecked")
		public void run() {

			Trace.info("Starting new liaison thread for new client. ID: %d\n", id);

			// Add shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					if (clientSocket != null) clientSocket.close();
				} catch (Exception e) {
					Trace.error("Client liaison may have not shut down correctly.");
					e.printStackTrace();
				}
			}));

			// Try to get output streams
			try {
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				in = new ObjectInputStream(clientSocket.getInputStream());
			} catch (Exception e) {
				Trace.error("Error connecting to client");
				e.printStackTrace();
				interrupt();
			}

			// Loop listening for commands.
			try {
				// Can't use in.read() because it will corrupt deserialization.
				while (true) {
					// Getting request and command by blocking here.
					Vector<Object> inputs = (Vector<Object>) in.readObject();
					Trace.info("Client sent: " + inputs);

					// Preparing to forward the message.
					String command = (String) inputs.get(0);

					// Processing response.
					Vector<Object> response;
					try {
						// Methods that are simple forwards (no need to unpack, then repack for use in interface)
						if (command.equalsIgnoreCase("addFlight")
								|| command.equalsIgnoreCase("deleteFlight")
								|| command.equalsIgnoreCase("queryFlight")
								|| command.equalsIgnoreCase("queryFlightPrice")) {
							response = TCPMiddleware.sendMessage(inputs, "flight", id);
						}
						else if (command.equalsIgnoreCase("addCars")
								|| command.equalsIgnoreCase("deleteCars")
								|| command.equalsIgnoreCase("queryCars")
								|| command.equalsIgnoreCase("queryCarsPrice")) {
							response = TCPMiddleware.sendMessage(inputs, "car", id);
						}
						else if (command.equalsIgnoreCase("addRooms")
								|| command.equalsIgnoreCase("deleteRooms")
								|| command.equalsIgnoreCase("queryRooms")
								|| command.equalsIgnoreCase("queryRoomsPrice")) {
							response = TCPMiddleware.sendMessage(inputs, "room", id);
						}

						// Methods involving customer and only customer
						else if (command.equalsIgnoreCase("newCustomer1")) {
							// ["newCustomer1", xid]
							int r = TCPMiddleware.instance.newCustomer( (int) inputs.get(1));
							response = new Vector<>(Arrays.asList( r, false ));
						}
						else if (command.equalsIgnoreCase("newCustomer2")) {
							// ["newCustomer2", xid, customerID]
							boolean r = TCPMiddleware.instance.newCustomer( (int) inputs.get(1), (int) inputs.get(2) );
							response = new Vector<>(Arrays.asList( r, false ));
						}
						else if (command.equalsIgnoreCase("deleteCustomer")) {
							// ["deleteCustomer", xid, customerID]
							boolean r = this.deleteCustomer( (int) inputs.get(1), (int) inputs.get(2) );
							response = new Vector<>(Arrays.asList( r, false ));
						}
						else if (command.equalsIgnoreCase("queryCustomerInfo")) {
							// ["queryCustomerInfo", xid, customerID]
							String r = TCPMiddleware.instance.queryCustomerInfo( (int) inputs.get(1), (int) inputs.get(2) );
							response = new Vector<>(Arrays.asList( r, false ));
						}

						// Methods involving more than 2 resources
						else if (command.equalsIgnoreCase("reserveFlight")) {
							// ["reserveFlight", xid, customerID, flightNum]
							boolean r = reserveItem((int) inputs.get(1), /* xid */
									(int) inputs.get(2), /* customerID */
									Flight.getKey( (int) inputs.get(3) ), /* key */
									String.valueOf( (int) inputs.get(3)), /* location */
									"flight" /* objectType */
							);
							response = new Vector<>(Arrays.asList( r, false ));
						}
						else if (command.equalsIgnoreCase("reserveCar")) {
							// ["reserveCars", xid, customerID, "location"]
							boolean r = reserveItem((int) inputs.get(1),
									(int) inputs.get(2),
									Car.getKey( (String) inputs.get(3) ),
									(String) inputs.get(3),
									"car"
							);
							response = new Vector<>(Arrays.asList( r, false ));
						}
						else if (command.equalsIgnoreCase("reserveRoom")) {
							// ["reserveRoom", xid, customerID, "location"]
							boolean r = reserveItem((int) inputs.get(1),
									(int) inputs.get(2),
									Room.getKey( (String) inputs.get(3) ),
									(String) inputs.get(3),
									"room"
							);
							response = new Vector<>(Arrays.asList( r, false ));
						}
						else if (command.equalsIgnoreCase("bundle")) {
							boolean r = bundle((int) inputs.get(1),
									(int) inputs.get(2),
									(Vector<String>) inputs.get(3),
									(String) inputs.get(4),
									(boolean) inputs.get(5),
									(boolean) inputs.get(6));
							response = new Vector<>(Arrays.asList( r, false ));
						}

						// Unknown command case
						else {
							throw new RuntimeException("Invalid command received");
						}

					} catch (Exception e) {
						Trace.info("Command %s has thrown exception \"%s\"!", command, e.toString());
						// Exception thrown by method: let client know.
						response = new Vector<>(Arrays.asList(null, true));
					}

					// Send back response.
					Trace.info("Reply sent: " + response + "\n ========================= \n");
					out.writeObject(response);
				}
			} catch (IOException e) {
				Trace.error("Client disconnected from server (IOException): thread shutdown ordered.");
				interrupt();
			} catch (Exception e) {
				Trace.error("Error while running server: thread shutdown ordered.");
				e.printStackTrace();
				interrupt();
			}
		}

		/**
		 * This method is a carbon copy of {@link Server.Common.ResourceManager#reserveItem(int, String)} )}
		 * that has been adapted to use TCP. It does NOT override anything.
		 */
		protected boolean reserveItem(int xid, int customerID, String key, String location, String objectType ) throws RemoteException {
			Trace.info("reserveItem(xid=%d,cid=%d,key=%s,loc=%s,objType=%s) called",xid,customerID,key,location,objectType);
			// Get customer information from MW hashmap
			Customer customer = (Customer)TCPMiddleware.instance.readData(xid, Customer.getKey(customerID));
			if (customer == null) {
				Trace.warn("reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
				return false;
			}

		/* Try to make the appropriate RM reserve an item. The RM will not be updated if this fails, it will be updated
		if this succeeds. i.e. if the RM fails to reserve, then its internal state has not changed. */
			Vector<Object> msg = new Vector<>( Arrays.asList("reserveItem", xid, key) );

			Vector<Object> response;
			if (objectType.equalsIgnoreCase("flight")) {
				response = sendMessage(msg, "flight", id);
			}
			else if(objectType.equalsIgnoreCase("car")) {
				response = sendMessage(msg, "car", id);
			}
			else if (objectType.equalsIgnoreCase("room")) {
				response = sendMessage(msg, "room", id);
			}
			else {
				Trace.error("Trying to reserve item of a type that does not exist.");
				return false;
			}

			Trace.info("Received reply: %s", response);
			String[] managerResponse = ( (String) response.get(0) ).split(",");

			// Only update the customer if the RM managed to reserve.
			if ( Boolean.parseBoolean(managerResponse[0]) ) {
				customer.reserve(key, location, Integer.parseInt(managerResponse[1]));
				TCPMiddleware.instance.writeData(xid, customer.getKey(), customer);
				return true;
			}
			// Reservation failed
			return false;
		}

		/**
		 * This method is a carbon copy of {@link Server.Common.ResourceManager#bundle(int, int, Vector, String, boolean, boolean)}
		 * that has been adapted to use TCP. It does NOT override anything.
		 */
		protected boolean bundle(int xid, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
			// ============================== Getting Started ==============================

			Trace.info("MW::bundle(" + xid + ", " + customerID + ", " + flightNumbers + ", " + location + ", " + car + ", " + room + ") called");
			// Validate input and convert into integers
			int[] flights;
			try {
				flights = flightNumbers.stream().mapToInt(Integer::parseInt).toArray();
			} catch (NumberFormatException e) {
				Trace.warn("MW::bundle() failed -- All flights must be numbers");
				return false;
			}

			// Load customer info from MW hashmap
			Customer customer = (Customer) instance.readData(xid, Customer.getKey(customerID));
			if (customer == null) {
				Trace.warn("MW::bundle(" + xid + ", " + customerID + ", " + flightNumbers + ", " + location + ")  failed--customer doesn't exist");
				return false;
			}

			// ========================= Trying to reserve =========================
			/* Try to reserve cars and rooms first since cancelling a bunch of
			 * flights at the same time will be difficult and should be left to the
			 * flights RM to decide to fail. */
			int carPrice = Integer.MIN_VALUE; // Get these for bookkeeping
			int roomPrice = Integer.MIN_VALUE;

			if (car) {
				// String[] ret = carManager.reserveItem(xid, Car.getKey(location)).split(",");
				// The following line of code is an absolute monstrosity, and I am sorry
				String[] ret = (  (String) sendMessage(new Vector<>(Arrays.asList("reserveItem", xid, Car.getKey(location))), "car", id).get(0)  ).split(",");
				if (!Boolean.parseBoolean(ret[0])) {
					Trace.warn("MW::bundle() failed because it was unavailable to reserve a car.");
					return false;
				}
				carPrice = Integer.parseInt(ret[1]);
			}

			if (room) {
				// String[] ret = roomManager.reserveItem(xid, Room.getKey(location)).split(",");
				// The following line of code is an absolute monstrosity, and I am sorry
				String[] ret = (  (String) sendMessage(new Vector<>(Arrays.asList("reserveItem", xid, Room.getKey(location))), "room", id).get(0)  ).split(",");
				if (!Boolean.parseBoolean(ret[0])) {
					Trace.warn("MW::bundle() failed because it was unavailable to reserve a room.");
					if (car) carManager.cancelItem(xid, Car.getKey(location), 1); // Since car could be reserved above
					return false;
				}
				roomPrice = Integer.parseInt(ret[1]);
			}

			// Try to reserve every requested flights (all or nothing)
			// String[] result = flightManager.reserveFlights(xid, customerID, flights).split(",");
			// The following line of code is an absolute monstrosity, and I am sorry
			String[] result = (  (String) sendMessage(new Vector<>(Arrays.asList("reserveFlights", xid, customerID, flights)), "flight", id).get(0)  ).split(",");

			// Reservation failure: Cancel all reservations and fail
			if (!Boolean.parseBoolean(result[0])) {
				Trace.error("MW::bundle failed to reserve all requested flights");
				if (car) {
					// carManager.cancelItem(xid, Car.getKey(location), 1);
					sendMessage(new Vector<>(Arrays.asList( "cancelItem", xid, Car.getKey(location), 1 )), "car", id);
				}
				if (room) {
					// roomManager.cancelItem(xid, Room.getKey(location), 1);
					sendMessage(new Vector<>(Arrays.asList( "cancelItem", xid, Room.getKey(location), 1 )), "room", id);
				}
				return false;
			}

			// ================== All reservations successful: update customer ==================
			// Parse flight prices
			int[] flightPrices = new int[result.length - 1];
			for (int i = 1; i < result.length; i++) flightPrices[i - 1] = Integer.parseInt(result[i]);

			if (car) {
				customer.reserve(Car.getKey(location), location, carPrice);
				instance.writeData(xid, customer.getKey(), customer);
			}

			if (room) {
				customer.reserve(Room.getKey(location), location, roomPrice);
				instance.writeData(xid, customer.getKey(), customer);
			}

			for (int i = 0; i < flights.length; i++) {
				customer.reserve(Flight.getKey(flights[i]), location, flightPrices[i]);
				instance.writeData(xid, customer.getKey(), customer);
			}

			return true;
		}

		/**
		 * This method is a carbon copy of {@link Server.Common.ResourceManager#deleteCustomer(int, int)}
		 * that has been adapted to use TCP. It does NOT override anything.
		 */
		protected boolean deleteCustomer(int xid, int customerID) throws RemoteException {
			// Getting customer
			Trace.info("MW::deleteCustomer(" + xid + ", " + customerID + ") called");
			Customer customer = (Customer) instance.readData(xid, Customer.getKey(customerID));
			if (customer == null) {
				Trace.warn("MW::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
				return false;
			}
			// Increase the reserved numbers of all reservable items which the customer reserved.
			RMHashMap reservations = customer.getReservations();
			boolean error = false;
			for (String reservedKey : reservations.keySet()) {
				// Identify type of item from the key (hardcoded, see Car.java, Room.java, Flight.java)
				ReservedItem reserveditem = customer.getReservedItem(reservedKey);

				Trace.info("MW::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times");

				String itemType = reserveditem.getKey().split("-")[0]; // car-xxxxxx
				if (itemType.equalsIgnoreCase("flight")) {
					// flightManager.cancelItem(xid, reserveditem.getKey(), reserveditem.getCount());
					sendMessage(new Vector<>( Arrays.asList( "cancelItem", xid, reserveditem.getKey(), reserveditem.getCount() ) ), "flight", id);
				}
				else if (itemType.equalsIgnoreCase("car")) {
					// carManager.cancelItem(xid, reserveditem.getKey(), reserveditem.getCount());
					sendMessage(new Vector<>( Arrays.asList( "cancelItem", xid, reserveditem.getKey(), reserveditem.getCount() ) ), "car", id);
				}
				else if (itemType.equalsIgnoreCase("room")) {
					// roomManager.cancelItem(xid, reserveditem.getKey(), reserveditem.getCount());
					sendMessage(new Vector<>( Arrays.asList( "cancelItem", xid, reserveditem.getKey(), reserveditem.getCount() ) ), "room", id);
				}
				else {
					Trace.error("The customer has an item keyed \"%s\" with wrongly formatted key.");
					error = true;
				}
			}

			// Unable to delete all reservations for a customer.
			if (error) {
				Trace.error("An error has occurred and not all items were cancelled. The customer has not been deleted");
				return false;
			}
			Trace.info("MW: Successfully cancelled all items reserved for client");

			// Remove the customer from the storage
			instance.removeData(xid, customer.getKey());
			Trace.info("MW::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
			return true;
		}

	}
}
