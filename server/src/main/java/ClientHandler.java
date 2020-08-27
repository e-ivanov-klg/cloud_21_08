import com.sun.org.apache.xpath.internal.operations.Bool;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientHandler implements Runnable {

    private final DataInputStream is;
    private final DataOutputStream os;
    private final IOServer server;
    private final Socket socket;
    private static int counter = 0;
    private final String name;
    private String clientPath = "server/ServerStorage";

    public ClientHandler(Socket socket, IOServer ioServer) throws IOException {
        server = ioServer;
        this.socket = socket;
        counter ++;
        name = "user#" + counter;
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        System.out.println("client handled: ip = " + socket.getInetAddress());
        System.out.println("Nick:" + name);
    }

    public void run() {
        Boolean quit = false;
        String fileName;
        File file;
        while (!quit) {
            try {
                String message = is.readUTF();
                System.out.println("Message from client: " + message);
                switch (message) {
                    case "./quit":
                        os.writeUTF("disconnected");
                        Thread.sleep(1000);
                        os.close();
                        is.close();
                        socket.close();
                        System.out.println("client " + name + " disconnected");
                        quit = true;
                        break;
                    case "./upload":
                        fileName = is.readUTF();
                        long fileLength = is.readLong();
                        file = new File(clientPath + "/" + fileName);
                        file.createNewFile();
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            byte[] buffer = new byte[256];
                            if (fileLength < 256) {
                                fileLength += 256;
                            }
                            int read = 0;
                            for (int i = 0; i < fileLength / 256; i++) {
                                read = is.read(buffer);
                                fos.write(buffer, 0, read);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        sendFileList();
                        break;
                    case "./dir":
                        sendFileList();
                        break;
                    case "./download":
                        fileName = is.readUTF();
                        file = new File(clientPath + "/" + fileName);
                        if (!file.exists()) {
                            System.out.println("File not found ! " + fileName);
                            os.writeUTF("File not found !");
                            os.flush();
                            break;
                        }
                        try (FileInputStream fis = new FileInputStream(file)) {
                            os.writeUTF("./download");
                            os.writeUTF(fileName);
                            os.writeLong(file.length());
                            byte[] buffer = new byte[256];
                            int read = 0;
                            while ((read = fis.read(buffer)) != -1) {
                                os.write(buffer, 0, read);
                            }
                            os.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendFileList() {
        File dir = new File(clientPath);
        try {
        os.writeUTF("./filelist");
        for (File file : Objects.requireNonNull(dir.listFiles())) {
                os.writeUTF(file.getName() + " " + file.length());
        }
        os.writeUTF("./end");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) throws IOException {
        os.writeUTF(message);
        os.flush();
    }
}
