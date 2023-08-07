package Server.Common;

import Server.Interface.IResourceManager;
import Server.RMI.RMIResourceManager;

import java.rmi.RemoteException;
import java.util.Vector;

/**
 * Extracted from the initial implementation of the RMIResourceManager so that
 * it can be reused with TCP with an adapter interface similar to the Client.
 */
public abstract class Middleware extends RMIResourceManager {

	/** References to each resource manager. Could either be an adapter or an RMI remote reference. */
	protected static IResourceManager flightManager, carManager, roomManager;

	public Middleware(String p_name) {
		super(p_name);
	}

	/**
	 * Reserves an item for a user given its key. Modified from the original in
	 * that it will only partially forward the call to the remote RM. To know
	 * which RM we must forward to, a field {@code objectType} has been added to
	 * figure out which RM to use (and to make adding more RMs easier).
	 *
	 * Furthermore, it has been modified so as to limit the number of remote
	 * method calls for easier TCP conversion.
	 * @param xid Not used
	 * @param customerID Customer to whom to reserve the item.
	 * @param key Key of the item to reserve.
	 * @param location Location of the item to reserve.
	 * @param objectType Either
	 * @return True if the reservation was successful, false otherwise.
	 */
	protected boolean reserveItem(int xid, int customerID, String key, String location, String objectType ) throws RemoteException
	{
		Trace.info("MW::reserveItem(%d,%d,%s,%s,%s) called",xid,customerID,key,location,objectType);
		// Get customer information from MW hashmap
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null) {
			Trace.warn("MW::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
			return false;
		}

		/* Try to make the appropriate RM reserve an item. The RM will not be updated if this fails, it will be updated
		if this succeeds. i.e. if the RM fails to reserve, then its internal state has not changed. */
		String[] managerResponse;
		if (objectType.equalsIgnoreCase("flight")) {
			managerResponse = flightManager.reserveItem( xid, key).split(",");
		}
		else if(objectType.equalsIgnoreCase("car")) {
			managerResponse = carManager.reserveItem(xid, key).split(",");
		}
		else if (objectType.equalsIgnoreCase("room")) {
			managerResponse = roomManager.reserveItem(xid, key).split(",");
		}
		else {
			Trace.error("Trying to reserve item of a type that does not exist.");
			return false;
		}

		// Only update the customer if the RM managed to reserve.
		if ( Boolean.parseBoolean(managerResponse[0]) ) {
			customer.reserve(key, location, Integer.parseInt(managerResponse[1]));
			writeData(xid, customer.getKey(), customer);
			return true;
		}
		// Reservation failed
		return false;
	}

	/* =================== SIMPLE FORWARDS TO A RM ===================
	 * aka does not touch customers */

	/** Simple forward to appropriate resource manager */
	@Override
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
		Trace.info("MW::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + "," + flightPrice + ") forwarded");
		return flightManager.addFlight(xid, flightNum, flightSeats, flightPrice);
	}

	/** Simple forward to appropriate resource manager */
	@Override
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException {
		Trace.info("MW::addCars(" + xid + ", " + location + ", " + count + "," + price + ") forwarded");
		return carManager.addCars(xid, location, count, price);
	}

	/** Simple forward to appropriate resource manager */
	@Override
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException {
		Trace.info("MW::addRooms(" + xid + ", " + location + ", " + count + "," + price + ") forwarded");
		return roomManager.addRooms(xid, location, count, price);
	}

	/** Simple forward to appropriate resource manager */
	@Override
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException {
		Trace.info("MW::deleteFlight(" + xid + ", " + flightNum + ") forwarded");
		return flightManager.deleteFlight(xid, flightNum);
	}

	/** Simple forward to appropriate resource manager */
	@Override
	public boolean deleteCars(int xid, String location) throws RemoteException {
		Trace.info("MW::deleteCars(" + xid + ", " + location + ") forwarded");
		return carManager.deleteCars(xid, location);
	}

	/** Simple forward to appropriate resource manager */
	@Override
	public boolean deleteRooms(int xid, String location) throws RemoteException {
		Trace.info("MW::deleteRooms(" + xid + ", " + location + ") forwarded");
		return roomManager.deleteRooms(xid, location);
	}

	/** Simple forward to appropriate resource manager */
	@Override
	public int queryFlight(int xid, int flightNum) throws RemoteException {
		Trace.info("MW::queryFlight(" + xid + ", " + flightNum + ") forwarded");
		return flightManager.queryFlight(xid, flightNum);
	}

	/** Simple forward to appropriate resource manager */
	@Override
	public int queryCars(int xid, String location) throws RemoteException {
		Trace.info("MW::queryCars(" + xid + ", " + location + ") forwarded");
		return carManager.queryCars(xid, location);
	}

	/** Simple forward to appropriate resource manager */
	@Override
	public int queryRooms(int xid, String location) throws RemoteException {
		Trace.info("MW::queryRooms(" + xid + ", " + location + ") forwarded");
		return roomManager.queryRooms(xid, location);
	}

	/** Simple forward to appropriate resource manager */
	@Override
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException {
		Trace.info("MW::queryFlightPrice(" + xid + ", " + flightNum + ") forwarded");
		return flightManager.queryFlightPrice(xid, flightNum);
	}

	/** Simple forward to appropriate resource manager */
	@Override
	public int queryCarsPrice(int xid, String location) throws RemoteException {
		Trace.info("MW::queryCarsPrice(" + xid + ", " + location + ") forwarded");
		return carManager.queryCarsPrice(xid, location);
	}

	/** Simple forward to appropriate resource manager */
	@Override
	public int queryRoomsPrice(int xid, String location) throws RemoteException {
		Trace.info("MW::queryRoomsPrice(" + xid + ", " + location + ") forwarded");
		return roomManager.queryRoomsPrice(xid, location);
	}

	/* =================== Overwritten reserve methods =================== */

	@Override
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException {
		Trace.info("MW::reserveFlight(" + xid + ", " + customerID + ", " + flightNum + ") called");
		return reserveItem( xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum), "flight");
	}

	@Override
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException {
		Trace.info("MW::reserveCar(" + xid + ", " + customerID + ", " + location + ") called");
		return reserveItem(xid, customerID, Car.getKey(location), location, "car");
	}

	@Override
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException {
		Trace.info("MW::reserveRoom(" + xid + ", " + customerID + ", " + location + ") called");
		return reserveItem(xid, customerID, Room.getKey(location), location, "room");
	}

	/**
	 * Method that will try to reserve ALL flights submitted and a car and/or
	 * room at the specified destination if desired. Either all requested
	 * reservations are made, or none are and the method returns an error.
	 * @param xid Not used
	 * @param customerID ID of the customer for which the reservation is made.
	 * @param flightNumbers List of the ID's all flights to be reserved.
	 * @param location Destination location (not sure AS not clear)
	 * @param car Whether a car should be reserved at the destination.
	 * @param room Whether a room should be reserved at the destination.
	 * @return True if success, false if any failure.
	 */
	@Override
	public boolean bundle(int xid, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
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
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
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
			/* NOTE, not calling reserveCar() since we do not want to commit
			the reservation to the customer just yet in case of success. We want
			to do so at the end once all reservations are successful. This
			should make it easier to backtrack in case of any and all
			reservation failure. */
			String[] ret = carManager.reserveItem(xid, Car.getKey(location)).split(",");
			if (!Boolean.parseBoolean(ret[0])) {
				Trace.warn("MW::bundle() failed because it was unavailable to reserve a car.");
				return false;
			}
			carPrice = Integer.parseInt(ret[1]);
		}

		if (room) {
			String[] ret = roomManager.reserveItem(xid, Room.getKey(location)).split(",");
			if (!Boolean.parseBoolean(ret[0])) {
				Trace.warn("MW::bundle() failed because it was unavailable to reserve a room.");
				if (car) carManager.cancelItem(xid, Car.getKey(location), 1); // Since car could be reserved above
				return false;
			}
			roomPrice = Integer.parseInt(ret[1]);
		}

		// Try to reserve every requested flights (all or nothing)
		String[] result = flightManager.reserveFlights(xid, customerID, flights).split(",");

		// Reservation failure: Cancel all reservations and fail
		if (!Boolean.parseBoolean(result[0])) {
			Trace.error("MW::bundle failed to reserve all requested flights");
			if (car) carManager.cancelItem(xid, Car.getKey(location), 1);
			if (room) roomManager.cancelItem(xid, Room.getKey(location), 1);
			return false;
		}

		// ================== All reservations successful: update customer ==================
		// Parse flight prices
		int[] flightPrices = new int[result.length - 1];
		for (int i = 1; i < result.length; i++) flightPrices[i - 1] = Integer.parseInt(result[i]);

		if (car) {
			customer.reserve(Car.getKey(location), location, carPrice);
			writeData(xid, customer.getKey(), customer);
		}

		if (room) {
			customer.reserve(Room.getKey(location), location, roomPrice);
			writeData(xid, customer.getKey(), customer);
		}

		for (int i = 0; i < flights.length; i++) {
			customer.reserve(Flight.getKey(flights[i]), location, flightPrices[i]);
			writeData(xid, customer.getKey(), customer);
		}

		return true;
	}

	/**
	 * Deletes a customer from the Middleware's hashmap and also increments the
	 * number of free items for all items the customer has reserved.
	 * @param xid Not used
	 * @param customerID Customer for whom to cancel everything.
	 * @return True if successful, false otherwise.
	 */
	@Override
	public boolean deleteCustomer(int xid, int customerID) throws RemoteException {
		// Getting customer
		Trace.info("MW::deleteCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
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
				flightManager.cancelItem(xid, reserveditem.getKey(), reserveditem.getCount());
			}
			else if (itemType.equalsIgnoreCase("car")) {
				carManager.cancelItem(xid, reserveditem.getKey(), reserveditem.getCount());
			}
			else if (itemType.equalsIgnoreCase("room")) {
				roomManager.cancelItem(xid, reserveditem.getKey(), reserveditem.getCount());
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
		removeData(xid, customer.getKey());
		Trace.info("MW::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
		return true;
	}


}
