import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

@SuppressWarnings("unchecked")
class OSerializationTestClient {

	enum CommandType {
		CLIENT_REQUEST_A,
		CLIENT_REQUEST_B,
		CLIENT_REQUEST_C,
		SERVER_RESPONSE_A,
		SERVER_RESPONSE_B,
		SERVER_RESPONSE_C;
	}



	private static Socket clientSocket;
	private static ObjectOutputStream out;
	private static ObjectInputStream in;

	public void startConnection(String host, int port) {
		try {
			clientSocket = new Socket(host, port);
			out = new ObjectOutputStream(clientSocket.getOutputStream());
			in = new ObjectInputStream(clientSocket.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public Vector<Object> sendMessage(Vector<Object> msg) throws IOException, ClassNotFoundException {
		out.writeObject(msg);
		return (Vector<Object>) in.readObject();
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		OSerializationTestClient client = new OSerializationTestClient();

		System.out.println("Client connecting...");
		client.startConnection(args[0], Integer.parseInt(args[1]));
		System.out.println("Client connected, sending message");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (in != null) in.close();
			} catch (Exception e) {
				System.err.println("Failed at shutting down client.");
				e.printStackTrace();
			}
			try {
				if (out != null) out.close();
			} catch (Exception e) {
				System.err.println("Failed at shutting down client.");
				e.printStackTrace();
			}
			try {
				if (clientSocket != null) clientSocket.close();
			} catch (Exception e) {
				System.err.println("Failed at shutting down client.");
				e.printStackTrace();
			}
		}));

		Vector<Object> output = client.sendMessage(new Vector<>(Arrays.asList(CommandType.CLIENT_REQUEST_A, "Hello server!")));
		System.out.printf("Server responded: %s, %s\n", output.get(0).toString(), output.get(1).toString());
		System.out.println("Shutting down");
	}

}