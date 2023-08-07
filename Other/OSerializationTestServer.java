import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Vector;

public class OSerializationTestServer {

	private static ServerSocket serverSocket;
	private static Socket clientSocket;
	private static ObjectInputStream in;
	private static ObjectOutputStream out;

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		System.out.println("Waiting for client...");

		serverSocket = new ServerSocket(1234);
		clientSocket = serverSocket.accept();
		out = new ObjectOutputStream(clientSocket.getOutputStream());
		in = new ObjectInputStream(clientSocket.getInputStream());

		System.out.println("Client connected");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (in != null) in.close();
			} catch (Exception e) {
				System.err.println("Failed at shutting down server.");
				e.printStackTrace();
			}
			try {
				if (out != null) out.close();
			} catch (Exception e) {
				System.err.println("Failed at shutting down server.");
				e.printStackTrace();
			}
			try {
				if (clientSocket != null) clientSocket.close();
			} catch (Exception e) {
				System.err.println("Failed at shutting down server.");
				e.printStackTrace();
			}
			try {
				if (serverSocket != null) serverSocket.close();
			} catch (Exception e) {
				System.err.println("Failed at shutting down server.");
				e.printStackTrace();
			}
		}));


		Vector<Object> clientRequest = (Vector<Object>) in.readObject(); // Block here
		System.out.printf("Client sent: %s, %s\n", clientRequest.get(0).toString(), clientRequest.get(1).toString());

		Vector<Object> serverResponse = new Vector<>(Arrays.asList(OSerializationTestClient.CommandType.SERVER_RESPONSE_A, "Hello client"));
		out.writeObject(serverResponse);
		System.out.println("Server sent response, shutting down");
	}

}
