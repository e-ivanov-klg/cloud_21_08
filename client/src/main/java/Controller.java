import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;

public class Controller implements Initializable {
    public ListView<String> clientFileList;
    public ListView<String> serverFileList;

    private Socket socket;
    private static DataInputStream is;
    private static DataOutputStream os;

    private String clientPath = "client/ClientStorage";

    public static void stop() {
        try {
            os.writeUTF("quit");
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initialize(URL location, ResourceBundle resources) {
 //       clientText.setOnAction(this::sendMessage);
        getClientFileList();
        try {
            socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread inThread = new Thread(()-> {
                try {
                    readDataFromServer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            inThread.start();
            os.writeUTF("./dir");
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getClientFileList() {
        File dir = new File(clientPath);
        clientFileList.getItems().clear();
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            clientFileList.getItems().add(file.getName() + " (" + file.length() + " bytes)");
        }
    }

    private void readDataFromServer() {
        String message;
        Boolean quit = false;
        List<String> fileList = new ArrayList<>();
        try {
            while (!quit) {
                message = is.readUTF();
                switch (message) {
                    case "./filelist":
                        fileList.clear();
                        String[] params;
                        while (true) {
                            message = is.readUTF();
                            params = message.split(" ");
                            if (params[0].equals("./end")) {
                                break;
                            } else {
                                fileList.add(params[0] + " (" + params[1] + " bytes)");
                            }
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    serverFileList.getItems().clear();
                                    serverFileList.getItems().addAll(fileList);
                                }
                            });
                        }
                        break;
                    case "./download":
                        message = is.readUTF();
                        String [] buffer = message.split(" ");
                        String fileName = buffer[0];
                        long fileLength = is.readLong();
                        File file = new File(clientPath + "/" + fileName);
                        if (file.exists()) {
                            ChangeFileDialog dialog = new ChangeFileDialog(file);
                            Platform.runLater(dialog);
                            while (dialog.getResult() == null) {}
                            if (dialog.getResult().isPresent() && dialog.getResult().get().getText().equals("Replace")) {
                                try {
                                    file.delete();
                                    file.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                String [] fileNameParams = fileName.split("\\.");
                                file = new File(clientPath + "/" + fileNameParams[0] + "_copy." + fileNameParams[1]);
                            }
                        }
                        FileOutputStream fos = new FileOutputStream(file);
                        byte[] dataBuffer = new byte[256];
                        if (fileLength < 256) {
                         fileLength += 256;
                        }
                        int read = 0;
                        for (int i = 0; i < fileLength / 256; i++) {
                            read = is.read(dataBuffer);
                            fos.write(dataBuffer, 0, read);
                        }
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                getClientFileList();
                            }
                        });
                        break;
                    case "./quit":
                        quit = true;
                        break;
                } //switch
            } // while (true)
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void uploadBtnClick(ActionEvent actionEvent) {
        int selectedIndex = clientFileList.getFocusModel().getFocusedIndex();
        if (selectedIndex == -1) {
            return;
        }
        String [] params = clientFileList.getItems().get(selectedIndex).toString().split(" ");
        String fileName = params[0];
        String command = "./upload";
        File file = new File(clientPath + "/" + fileName);
        try (FileInputStream fis = new FileInputStream(file)) {
            os.writeUTF(command);
            os.writeUTF(fileName);
            os.writeLong(file.length());
            byte [] buffer = new byte[256];
            int byteCounter = 0;
            while ((byteCounter = fis.read(buffer)) != -1) {
                os.write(buffer, 0, byteCounter);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void downloadBtnClick(ActionEvent actionEvent) {
        int selectedIndex = serverFileList.getFocusModel().getFocusedIndex();
        if (selectedIndex == -1) {
            return;
        }
        String [] params = serverFileList.getItems().get(selectedIndex).toString().split(" ");
        String fileName = params[0];
        try {
            os.writeUTF("./download");
            os.writeUTF(fileName);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
