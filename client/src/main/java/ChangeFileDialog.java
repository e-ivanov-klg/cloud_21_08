import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.File;
import java.util.Optional;

public class ChangeFileDialog implements Runnable{
    private File file;
    private Optional<ButtonType> result;

    public ChangeFileDialog(File file) {
        this.file = file;
        result = null;
    }

    public Optional<ButtonType> getResult() {
        return result;
    }

    @Override
    public void run() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.getButtonTypes().clear();
        alert.getButtonTypes().add(new ButtonType("Replace", ButtonBar.ButtonData.OTHER));
        alert.getButtonTypes().add(new ButtonType("Copy", ButtonBar.ButtonData.OTHER));
        alert.setTitle("File Download");
        alert.setHeaderText("File " + file.getName() + " is exists!.");
        alert.setContentText("Replace or copy file?");
        result = alert.showAndWait();
    }
}
