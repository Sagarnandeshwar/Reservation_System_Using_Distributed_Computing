import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ORMIRegBindPrinter {

	/**
	 * Prints out all bound names on a given RMIRegistry
	 * @param args [hostname: localhost] [port: 7050]
	 */
	public static void main(String[] args) {

		// Args handling
		int port = 7050;
		String host = "localhost";
		if (args.length != 0) {
			if (args.length != 2) {
				System.out.println("Usage: RMIRegBindPrinter <host> <port>");
				System.exit(0);
			}
			try {
				host = args[0];
				port = Integer.parseInt(args[1]);
			} catch (Exception e) {
				System.out.println("Error parsing args");
				e.printStackTrace();
				System.exit(1);
			}
		}

		// Connecting to reg
		Registry reg;
		System.out.println("Trying to connect to the registry");
		try {
			System.out.printf("Getting registry on [%s:%d]\n", host, port);
			reg = LocateRegistry.getRegistry(host, port);
		} catch (RemoteException e) {
			System.out.println("No registry on port or network error.");
			e.printStackTrace();
			return;
		}

		// Listing registry bound names
		System.out.printf("Getting names bound on registry at [%s:%d]\n", host, port);
		try {
			String[] binds = reg.list();
			if (binds.length == 0) {
				System.out.println("   No name binds in registry.");
			}
			else {
				for (String name : reg.list()) {
					System.out.printf("   %-20s\n", name);
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

}
