package Client;

import Server.Interface.IResourceManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Vector;

/**
 * Custom implementation of the IResourceManager that translates all calls to
 * its methods into serialized vectors to be sent over TCP. This is to reduce
 * the amount of code to change, to better utilize {@link Client}'s methods,
 * and more easily allow a TCP and RMI clients to coexist in the codebase.
 * @author Aidan Gomar, Yiwei Xiong, Ye Tong Zhou
 */
public class TCPIResourceManagerWrapper implements IResourceManager {

	public static boolean DEBUG = false;

	/* Write into this to send to the Server's buffer. */
	private final ObjectOutputStream out;
	/* Read from this for the server's replies. */
	private final ObjectInputStream in;

	public TCPIResourceManagerWrapper(ObjectOutputStream out, ObjectInputStream in) {
		this.out = out;
		this.in = in;
	}

	/**
	 * This method gets a message from the middleware and decodes it. If the MW
	 * returned an object, it returns it. If the MW encountered an exception,
	 * this method throws a RemoteException.
	 * @param in Inputstream to read from (From the MW)
	 * @return Object returned by the MW (has to be cast)
	 * @throws RemoteException If the MW encountered an exception or if there
	 *                         was a network error.
	 */
	@SuppressWarnings("unchecked")
	private Object readObject(ObjectInputStream in) throws RemoteException {
		try {
			// Read an object from the inputstream
			var response = (Vector<Object>) in.readObject();
			// Throw an error if the server reports so, otherwise return server's return.
			if ((boolean) response.get(1)) throw new RemoteException("An exception has occurred at the server.");
			else return response.get(0);

		} catch (IOException|ClassNotFoundException e) {
			throw new RemoteException("Error reading server response");
		}
	}

	/**
	 * This method translates IResourceManager calls to messages that can be
	 * sent to the Middleware via TCP in the form of a vector of the method
	 * name and its arguments. The MW is expected to deserialize the vector and
	 * cast the elements according to the method called.
	 * @param args Interface method name, and then arguments to said method.
	 * @return Return object of the middleware.
	 * @throws RemoteException If any errors, including at the MW and RM.
	 */
	private Object sendMessage(Serializable... args) throws RemoteException {
		Object ret = null;
		try {
			Vector<Object> msg = new Vector<>(Arrays.asList(args));
			if (DEBUG) System.out.printf("Sending: %s\n", msg);
			out.writeObject(msg);
			ret = readObject(in);
			if (DEBUG) System.out.printf("Received: %s\n", ret);
		} catch (IOException e) {
			System.err.println("Network error, server might be down. Shutdown requested.");
			System.exit(1);
		}
		return ret;
	}

	@Override
	public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
		return (boolean) sendMessage("addFlight", id, flightNum, flightSeats, flightPrice);
	}

	@Override
	public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
		return (boolean) sendMessage("addCars", id, location, numCars, price);
	}

	@Override
	public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
		return (boolean) sendMessage("addRooms", id, location, numRooms, price);
	}

	@Override
	public int newCustomer(int id) throws RemoteException {
		return (int) sendMessage("newCustomer1", id);
	}

	@Override
	public boolean newCustomer(int id, int cid) throws RemoteException {
		return (boolean) sendMessage("newCustomer2", id, cid);
	}

	@Override
	public boolean deleteFlight(int id, int flightNum) throws RemoteException {
		return (boolean) sendMessage("deleteFlight", id, flightNum);
	}

	@Override
	public boolean deleteCars(int id, String location) throws RemoteException {
		return (boolean) sendMessage("deleteCars", id, location);
	}

	@Override
	public boolean deleteRooms(int id, String location) throws RemoteException {
		return (boolean) sendMessage("deleteRooms", id, location);
	}

	@Override
	public boolean deleteCustomer(int id, int customerID) throws RemoteException {
		return (boolean) sendMessage("deleteCustomer", id, customerID);
	}

	@Override
	public int queryFlight(int id, int flightNumber) throws RemoteException {
		return (int) sendMessage("queryFlight", id, flightNumber);
	}

	@Override
	public int queryCars(int id, String location) throws RemoteException {
		return (int) sendMessage("queryCars", id, location);
	}

	@Override
	public int queryRooms(int id, String location) throws RemoteException {
		return (int) sendMessage("queryRooms", id, location);
	}

	@Override
	public String queryCustomerInfo(int id, int customerID) throws RemoteException {
		return (String) sendMessage("queryCustomerInfo", id, customerID);
	}

	@Override
	public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
		return (int) sendMessage("queryFlightPrice", id, flightNumber);
	}

	@Override
	public int queryCarsPrice(int id, String location) throws RemoteException {
		return (int) sendMessage("queryCarsPrice", id, location);
	}

	@Override
	public int queryRoomsPrice(int id, String location) throws RemoteException {
		return (int) sendMessage("queryRoomsPrice", id, location);
	}

	@Override
	public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
		return (boolean) sendMessage("reserveFlight", id, customerID, flightNumber);
	}

	@Override
	public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
		return (boolean) sendMessage("reserveCar", id, customerID, location);
	}

	@Override
	public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
		return (boolean) sendMessage("reserveRoom", id, customerID, location);
	}

	@Override
	public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
		return (boolean) sendMessage("bundle", id, customerID, flightNumbers, location, car, room);
	}

	@Override
	public String getName() throws RemoteException {
		return (String) sendMessage("getName");
	}

	@Override
	public String reserveItem(int xid, String key) throws RemoteException {
		throw new RuntimeException("This should not be called by the user.");
	}

	@Override
	public boolean cancelItem(int xid, String key, int number) throws RemoteException {
		throw new RuntimeException("This should not be called by the user.");
	}

	@Override
	public String reserveFlights(int xid, int customerID, int[] flights) throws RemoteException {
		throw new RuntimeException("This should not be called by the user.");
	}
}
