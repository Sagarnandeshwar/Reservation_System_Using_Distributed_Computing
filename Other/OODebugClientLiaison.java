import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Vector;

public class OODebugClientLiaison {

	private static ServerSocket serverSocket;
	private static Socket clientSocket;
	private static ObjectInputStream in;
	private static ObjectOutputStream out;

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// Connecting to client.
		System.out.println("Waiting for client...");

		serverSocket = new ServerSocket(7050); // Hardcode
		clientSocket = serverSocket.accept();
		out = new ObjectOutputStream(clientSocket.getOutputStream());
		in = new ObjectInputStream(clientSocket.getInputStream());

		System.out.println("Client connected");

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

		// Loop listening for commands.
		try {
			while (true) { // Can't use in.read because it will corrupt deserialization.
				// Getting request and command.
				Vector<Object> clientRequest = (Vector<Object>) in.readObject(); // Block here
				System.out.println("Client sent: " + clientRequest);
				String command = (String) clientRequest.get(0);

				// Processing response.
				Vector<Object> response;
				switch (command) {
					case "addFlight": response = new Vector<>(Arrays.asList(true, false)); break;
					case "addCars": response = new Vector<>(Arrays.asList(true, false)); break;
					case "addRooms": response = new Vector<>(Arrays.asList(true, false)); break;
					case "newCustomer1": response = new Vector<>(Arrays.asList(1234, false)); break;
					case "newCustomer2": response = new Vector<>(Arrays.asList(true, false)); break;
					case "deleteFlight": response = new Vector<>(Arrays.asList(true, false)); break;
					case "deleteCars": response = new Vector<>(Arrays.asList(true, false)); break;
					case "deleteRooms": response = new Vector<>(Arrays.asList(true, false)); break;
					case "deleteCustomer": response = new Vector<>(Arrays.asList(true, false)); break;
					case "queryFlight": response = new Vector<>(Arrays.asList(1234, false)); break;
					case "queryCars": response = new Vector<>(Arrays.asList(1234, false)); break;
					case "queryRooms": response = new Vector<>(Arrays.asList(1234, false)); break;
					case "queryCustomerInfo": response = new Vector<>(Arrays.asList("Hello client", false)); break;
					case "queryFlightPrice": response = new Vector<>(Arrays.asList(1234, false)); break;
					case "queryCarsPrice": response = new Vector<>(Arrays.asList(1234, false)); break;
					case "queryRoomsPrice": response = new Vector<>(Arrays.asList(1234, false)); break;
					case "reserveFlight": response = new Vector<>(Arrays.asList(true, false)); break;
					case "reserveCar": response = new Vector<>(Arrays.asList(true, false)); break;
					case "reserveRoom": response = new Vector<>(Arrays.asList(true, false)); break;
					case "bundle": response = new Vector<>(Arrays.asList(true, false)); break;
					default: throw new RuntimeException("Invalid command received");
				}

				System.out.println("Reply sent: " + response);
				out.writeObject(response);
			}
		}
		catch (IOException e) {
			System.err.println("Client disconnected from server (IOException): thread shutdown ordered.");
			System.exit(0);
		}
		catch (Exception e) {
			System.err.println("Error while running server: thread shutdown ordered.");
			e.printStackTrace();
			System.exit(1);
		}
	}

}