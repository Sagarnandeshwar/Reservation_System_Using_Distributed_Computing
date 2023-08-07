package Server.RMI;

import Server.Common.*;
import Server.Interface.IResourceManager;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

/**
 * Singleton middleware with which the clients should interact with. Calls the
 * remote method of the appropriate RMI resource manager server. Is itself a
 * RM for customers so as to not have to make a remote call to a customer RM
 * for most reserve operations.
 *
 * This subclass merely takes care of network connections and getting the remote
 * references to the RM Interfaces via RMI. How the middleware interacts with
 * said interface references is common with TCP.
 *
 * @author Aidan Gomar, Ye Tong Zhou, Yiwei Xiong
 */
public class RMIMiddleware extends Middleware {

	private static final String s_serverName = "Middleware", s_rmiPrefix = "group_50_";

	/** Hardcoded default registry name binds for the resource managers */
	private static final String RM_FLIGHT_BIND = "group_50_Flights", RM_CAR_BIND = "group_50_Cars", RM_ROOM_BIND = "group_50_Rooms";

	/** Hosts for the resource managers (default is localhost) */
	private static String rm_flight_host = "localhost", rm_car_host = "localhost", rm_room_host = "localhost";

	/* Hardcoded ports for the registries in which the resource managers are. */
	private static final int rm_flight_port = 7050, rm_car_port = 7050, rm_room_port = 7050;

	public RMIMiddleware(String p_name) {
		super(p_name);
	}

	public static void main(String[] args) {
		// Argument processing
		if (args.length != 3) {
			Trace.info("Middleware initialized with localhost as host for all resource managers");
		}
		else {
			rm_flight_host = args[0];
			rm_car_host = args[1];
			rm_room_host = args[2];
		}

		Trace.info("Initializing middleware with: \n   flight RM: [%s:%d/%s]\n   car RM: [%s:%d/%s]" +
						"\n   room RM [%s:%d/%s]\n", rm_flight_host, rm_flight_port, RM_FLIGHT_BIND, rm_car_host, rm_car_port,
				RM_CAR_BIND, rm_room_host, rm_room_port, RM_ROOM_BIND);

		// Try to grab the remote resource managers references
		try {
			flightManager = getResourceManager(rm_flight_host, rm_flight_port, RM_FLIGHT_BIND);
			carManager = getResourceManager(rm_car_host, rm_car_port, RM_CAR_BIND);
			roomManager = getResourceManager(rm_room_host, rm_room_port, RM_ROOM_BIND);
		} catch (Exception e) {
			Trace.error("Unexpected error while retrieving remote managers.");
			e.printStackTrace();
			System.exit(1);
		}

		// Startup the middleware (inspired by RMIResourceManager)
		try {

			// Create reference to export
			Trace.info("MW: Exporting object \"Middleware\"...");
			RMIMiddleware middleware = new RMIMiddleware("Middleware");
			IResourceManager rm = (IResourceManager)UnicastRemoteObject.exportObject(middleware, 0);
			Trace.info("Done");

			// Get registry and bind RM reference to it
			Registry reg;
			try {
				reg = LocateRegistry.createRegistry(7050);
			} catch (RemoteException e) { // Already exists
				reg = LocateRegistry.getRegistry(7050);
			}
			reg.rebind(s_rmiPrefix + s_serverName, rm);
			Trace.info("MW: Registry acquired.");

			// Create shutdown script to unbind itself from RMIRegistry
			final Registry registry = reg;
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					registry.unbind(s_rmiPrefix + s_serverName);
					Trace.info("Middleware unbound from registry");
				} catch (Exception e) {
					Trace.error("Failed at shutting down middleware");
					e.printStackTrace();
					System.exit(1);
				}
			}));

			// Create and install a security manager
			Trace.info("MW: Creating registry... ");
			if (System.getSecurityManager() == null) {
				System.setSecurityManager(new SecurityManager());
			}
			Trace.info("Done");


		} catch (Exception e) {
			Trace.error("Error while initializing middleware");
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * Utility method that gets a remote resource manager. Inspired by 
	 * RMIClient#connectServer(String, int, String)
	 * @param host Address of the resource manager host.
	 * @param port Port of the registry at the host.
	 * @param bind Name bind for the resource manager at the host.
	 * @return Requested remote resource manager.
	 * @throws Exception Any unexpected exception (non remote related).
	 */
	private static IResourceManager getResourceManager(String host, int port, String bind) throws Exception {
		boolean first = true;
		while (true) {
			try {
				// Periodically try to connect to the resource manager until it works
				IResourceManager ret = (IResourceManager) LocateRegistry.getRegistry(host, port).lookup(bind);
				Trace.info("Successfully connected to [%s:%d/%s]", host, port, bind);
				return ret;
			} catch (NotBoundException|RemoteException e) {
				if (first) {
					Trace.info("Waiting for [%s:%d/%s]...", host, port, bind);
					first = false;
				}
			}
			Thread.sleep(500);
		}
	}





}
