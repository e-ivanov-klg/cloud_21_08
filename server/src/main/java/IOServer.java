import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IOServer {

    private ServerSocket server;
    private ConcurrentLinkedQueue<ClientHandler> queue;
    private boolean isRunning = true;

    public void stop() {
        isRunning = false;
    }

    public IOServer() {
        try {
            queue = new ConcurrentLinkedQueue<ClientHandler>();
            server = new ServerSocket(8189);
            System.out.println("Server started on 8189");
            while (true) {
                Socket socket = server.accept();
                ClientHandler client = new ClientHandler(socket, this);
                queue.add(client);
                new Thread(client).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void kick(ClientHandler clientHandler) {
        queue.remove(clientHandler);
    }

    public static void main(String[] args) {
        IOServer server = new IOServer();
    }
}
