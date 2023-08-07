// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;

import java.util.*;
import java.rmi.RemoteException;

public class ResourceManager implements IResourceManager
{
	protected String m_name = "";
	protected final RMHashMap m_data = new RMHashMap();

	public ResourceManager(String p_name)
	{
		m_name = p_name;
	}

	// Reads a data item
	protected RMItem readData(int xid, String key)
	{
		synchronized(m_data) {
			RMItem item = m_data.get(key);
			if (item != null) {
				return (RMItem)item.clone();
			}
			return null;
		}
	}

	// Writes a data item
	protected void writeData(int xid, String key, RMItem value)
	{
		synchronized(m_data) {
			m_data.put(key, value);
		}
	}

	// Remove the item out of storage
	protected void removeData(int xid, String key)
	{
		synchronized(m_data) {
			m_data.remove(key);
		}
	}

	// Deletes the encar item
	protected boolean deleteItem(int xid, String key)
	{
		Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		// Check if there is such an item in the storage
		if (curObj == null)
		{
			Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return false;
		}
		else
		{
			if (curObj.getReserved() == 0)
			{
				removeData(xid, curObj.getKey());
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
				return true;
			}
			else
			{
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
				return false;
			}
		}
	}

	// Query the number of available seats/rooms/cars
	protected int queryNum(int xid, String key)
	{
		Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0;  
		if (curObj != null)
		{
			value = curObj.getCount();
		}
		Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
		return value;
	}    

	// Query the price of an item
	protected int queryPrice(int xid, String key)
	{
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0; 
		if (curObj != null)
		{
			value = curObj.getPrice();
		}
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
		return value;        
	}

	/**
	 * Method to mark one the items on this RM as reserved. The customerID field is unused. This method only updates
	 * the RM's local count of said item available. It does not update the customer. <b>Calling this method alone to
	 * reserve an item is insufficient.<b/> It <b>must</b> be called from
	 * Server.RMI.RMIMiddleware#reserveItem(int, int, String, String, String)
	 *
	 * @param xid Unused
	 * @param key Key of the item to reserve.
	 * @return A CSV delimited string. [0]: true/false / [1]: priceInteger
	 */
	// Reserve an item
	@Override
	public String reserveItem(int xid, String key) {
		Trace.info("RM::reserveItem(" + xid + ", " + key + ") called" );
		// Check if the item is reservable
		ReservableItem item = (ReservableItem)readData(xid, key);
		if (item == null) {
			Trace.warn("RM::reserveItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return false + ",-1";
		}
		else if (item.getCount() == 0) {
			Trace.warn("RM::reserveItem(" + xid + ", " + key + ") failed--No more items");
			return false + ",-1";
		}

		// Item available:
		else {
			// Decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);
			writeData(xid, item.getKey(), item);

			Trace.info("RM::reserveItem(" + xid + ", " + key + ") succeeded");
			return true + "," + item.getPrice();
		}
	}

	/**
	 * Analogous to {@link IResourceManager#reserveItem(int, String)}, but with the
	 * possibility to choose the number of the specified item to cancel. Just
	 * like it, <b>calling this method alone is not enough to completely cancel
	 * an item</b> as the customer must be updated from the middleware.
	 *
	 * @param xid Not used.
	 * @param key Key of the item to cancel.
	 * @return True if successful, false otherwise.
	 */
	@Override
	public boolean cancelItem(int xid, String key, int number) {
		Trace.info("RM::cancelItem(%d, %s, %d) called", xid, key, number);
		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(xid, String.valueOf(key));
		if (item == null) {
			Trace.error("RM::cancelItem() failed--Item does not exist");
			return false;
		}

		// Increase the number of specified item available in the storage
		item.setCount(item.getCount() + number); // One more available
		item.setReserved(item.getReserved() - number); // One less reserved
		writeData(xid, item.getKey(), item);
		Trace.info("RM::cancelItem() succeeded--%d remaining", item.getCount());
		return true;
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	@Override
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException
	{
		Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		Flight curObj = (Flight)readData(xid, Flight.getKey(flightNum));
		if (curObj == null)
		{
			// Doesn't exist yet, add it
			Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice + ", Key=" + newObj.getKey());
		}
		else
		{
			// Add seats to existing flight and update the price if greater than zero
			curObj.setCount(curObj.getCount() + flightSeats);
			if (flightPrice > 0)
			{
				curObj.setPrice(flightPrice);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice + ",key=" + curObj.getKey());
		}
		return true;
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	@Override
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException
	{
		Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Car curObj = (Car)readData(xid, Car.getKey(location));
		if (curObj == null)
		{
			// Car location doesn't exist yet, add it
			Car newObj = new Car(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price + ", key=" + newObj.getKey());
		}
		else
		{
			// Add count to existing car location and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price + ", key=" + curObj.getKey());
		}
		return true;
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	@Override
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException
	{
		Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Room curObj = (Room)readData(xid, Room.getKey(location));
		if (curObj == null)
		{
			// Room location doesn't exist yet, add it
			Room newObj = new Room(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price + newObj.getKey());
		} else {
			// Add count to existing object and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price + ", key=" + curObj.getKey());
		}
		return true;
	}

	// Deletes flight
	@Override
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException {
		Trace.info("RM::deleteFlight(%d, %d) called", xid, flightNum);
		return deleteItem(xid, Flight.getKey(flightNum));
	}

	// Delete cars at a location
	@Override
	public boolean deleteCars(int xid, String location) throws RemoteException {
		Trace.info("RM::deleteCars(%d, %s) called", xid, location);
		return deleteItem(xid, Car.getKey(location));
	}

	// Delete rooms at a location
	@Override
	public boolean deleteRooms(int xid, String location) throws RemoteException {
		Trace.info("RM:: deleteRooms(%d, %s) called", xid, location);
		return deleteItem(xid, Room.getKey(location));
	}

	// Returns the number of empty seats in this flight
	@Override
	public int queryFlight(int xid, int flightNum) throws RemoteException {
		Trace.info("RM::queryFlight(%d, %d) called", xid, flightNum);
		return queryNum(xid, Flight.getKey(flightNum));
	}

	// Returns the number of cars available at a location
	@Override
	public int queryCars(int xid, String location) throws RemoteException {
		Trace.info("RM:: queryCars(%d, %s) called", xid, location);
		return queryNum(xid, Car.getKey(location));
	}

	// Returns the amount of rooms available at a location
	@Override
	public int queryRooms(int xid, String location) throws RemoteException {
		Trace.info("RM:: queryRooms(%d, %s) called", xid, location);
		return queryNum(xid, Room.getKey(location));
	}

	// Returns price of a seat in this flight
	@Override
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException {
		Trace.info("RM:: queryFlightPrice(%d, %d) called", xid, flightNum);
		return queryPrice(xid, Flight.getKey(flightNum));
	}

	// Returns price of cars at this location
	@Override
	public int queryCarsPrice(int xid, String location) throws RemoteException {
		Trace.info("RM:: queryCarsPrice(%d, %s) called", xid, location);
		return queryPrice(xid, Car.getKey(location));
	}

	// Returns room price at this location
	@Override
	public int queryRoomsPrice(int xid, String location) throws RemoteException {
		Trace.info("RM:: queryRoomsPrice(%d, %s) called", xid, location);
		return queryPrice(xid, Room.getKey(location));
	}

	@Override
	public String queryCustomerInfo(int xid, int customerID) throws RemoteException
	{
		Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			// NOTE: don't change this--WC counts on this value indicating a customer does not exist...
			return "";
		}
		else
		{
			Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
			System.out.println(customer.getBill());
			return customer.getBill();
		}
	}

	@Override
	public int newCustomer(int xid) throws RemoteException
	{
		Trace.info("RM::newCustomer(" + xid + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(xid) +
			String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
			String.valueOf(Math.round(Math.random() * 100 + 1)));
		Customer customer = new Customer(cid);
		writeData(xid, customer.getKey(), customer);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	@Override
	public boolean newCustomer(int xid, int customerID) throws RemoteException
	{
		Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			customer = new Customer(customerID);
			writeData(xid, customer.getKey(), customer);
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
			return true;
		}
		else
		{
			Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
			return false;
		}
	}


	/** <b>DO NOT USE</b> */
	@Override
	public boolean deleteCustomer(int xid, int customerID) throws RemoteException {
		Trace.info("deleteCustomer(%d, %d) called", xid, customerID);
		throw new RuntimeException("This method should only be called on the middleware");
	}

	@Override
	/** <b>DO NOT USE</b> */
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException {
		Trace.info("reserveFlight(%d, %d, %d) called", xid, customerID, flightNum);
		throw new RuntimeException("This method should only be called on the middleware");
	}

	/**
	 * Helper method that tries to reserve multiple flights given a list of
	 * flight numbers. Assuming there is only one RM for flights and that it is
	 * single threaded, then this method should guarantee "atomicity".
	 * Relinquishes any reservations in the event that any of the requested
	 * flights are currently reserved.
	 * @param xid Unused
	 * @param flights Array of flight numbers requested.
	 * @return A CSV delimited String. [0]: true/false
	 *         / [1] - [end]: Integers representing prices
	 */
	@Override
	public String reserveFlights(int xid, int customerID, int[] flights) {
		Trace.info("reserveFlight(%d,%d,%s) called", xid, customerID, Arrays.toString(flights));

		int[] prices = new int[flights.length];

		synchronized (m_data) {
			int i = 0;
			boolean failure = false;
			for (; i < flights.length; i++) {
				// Break out in case of failure
				String[] ret = reserveItem(xid, Flight.getKey(flights[i])).split(",");
				if (!Boolean.parseBoolean(ret[0])) {
					failure = true;
					break;
				}
				prices[i] = Integer.parseInt(ret[1]);
			}

			// Cancel reservation in case of failure
			if (failure) {
				i--; // (Current wasn't reserved);
				for (; i >= 0; i--) {
					// Input inspired from reserveFlight
					cancelItem(xid, Flight.getKey(flights[i]), 1);
				}
				return false + ",";
			}
		}

		// Serializing return into string.
		String ret = Boolean.toString(true);
		for (int p : prices) {
			ret = ret + "," + p;
		}
		Trace.info("reserveFlight() output: %s", ret);
		return ret;
	}

	/** <b>DO NOT USE</b> */
	@Override
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException {
		throw new RuntimeException("This method should only be called on the middleware");
	}

	/** <b>DO NOT USE</b> */
	@Override
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException {
		throw new RuntimeException("This method should only be called on the middleware");
	}

	/** <b>DO NOT USE</b> */
	@Override
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
		throw new RuntimeException("This method should only be called on the middleware");
	}

	@Override
	public String getName() throws RemoteException
	{
		return m_name;
	}
}
 
