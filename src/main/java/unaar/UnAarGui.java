package unaar;

import java.io.File;
import java.io.IOException;

import arc.archive.ArchiveInput;
import arc.archive.ArchiveRegistry;
import arc.mf.client.archive.Archive;
import arc.streams.StreamCopy;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class UnAarGui {

    private File _archiveFile;
    private Scene _scene;

    private ProgressBar _progressBar;
    private Text _progressText;
    private Button _button;

    private Task<Void> _task;

    UnAarGui(File archiveFile) {
        _archiveFile = archiveFile;
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 20, 20, 20));

        Text inputFileText = new Text(
                "Extracting " + _archiveFile.getAbsolutePath() + "...");
        gridPane.add(inputFileText, 0, 0);

        _progressBar = new ProgressBar();
        _progressBar.prefWidthProperty().bind(gridPane.widthProperty());
        gridPane.add(_progressBar, 0, 1);

        _progressText = new Text();
        gridPane.add(_progressText, 0, 2);

        _button = new Button("Cancel");
        _button.setOnAction(event -> {
            if ("Cancel".equalsIgnoreCase(_button.getText())) {
                if (_task != null && _task.isRunning()
                        && !_task.isCancelled()) {
                    _task.cancel();
                    setButtonState("Cancel", true);
                }
            } else {
                Platform.exit();
                System.exit(0);
            }
        });
        _button.setAlignment(Pos.CENTER);
        _button.setPrefWidth(88);
        gridPane.add(_button, 0, 3);
        GridPane.setConstraints(_button, 0, 3, 1, 1, HPos.RIGHT, VPos.CENTER);
        _scene = new Scene(gridPane, 800.0, 180.0, Color.WHITE);

    }

    public void showOnStage(Stage stage) {
        stage.setTitle("AAR Extractor");
        stage.setScene(_scene);
        stage.show();
        extractArchive(_archiveFile, stage);
    }

    private void extractArchive(File archiveFile, Stage stage) {
        if (_task != null && _task.isRunning()) {
            _task.cancel();
        }
        _task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Archive.declareSupportForAllTypes();
                ArchiveInput ai = null;
                try {
                    File outputDir = makeOutputDir(archiveFile);
                    ai = ArchiveRegistry.createInput(archiveFile);
                    long nbEntries = ai.nbEntries(true);
                    if (nbEntries > 0) {
                        long nbExtracted = 0;
                        setProgress(0.0);
                        ArchiveInput.Entry e;
                        while ((e = ai.next()) != null) {
                            if (isCancelled()) {
                                break;
                            }
                            File f = new File(outputDir, e.name());
                            setProgressText(f.getAbsolutePath(), false);
                            if (e.isDirectory()) {
                                f.mkdirs();
                            } else {
                                f.getParentFile().mkdirs();
                                StreamCopy.copy(e.stream(), f);
                            }
                            nbExtracted++;
                            setProgress(
                                    (double) nbExtracted / (double) nbEntries);
                        }
                        if (isCancelled()) {
                            setProgressText("Cancelled.", true);
                        } else {
                            setProgressText("Complete!", false);
                            setProgress(1.0);
                        }
                        setButtonState("OK", false);
                    }
                } catch (Throwable e) {
                    setProgressText("Error: " + e.getMessage(), true);
                    setButtonState("OK", false);
                }
                return null;
            }
        };
        new Thread(_task).start();
    }

    private void setButtonState(String text, boolean disable) {
        Platform.runLater(() -> {
            _button.setText(text);
            _button.setDisable(disable);
        });
    }

    private void setProgress(double progress) {
        Platform.runLater(() -> {
            _progressBar.setProgress(progress);
        });
    }

    private void setProgressText(String text, boolean error) {
        Platform.runLater(() -> {
            _progressText.setFill(error ? Color.RED : Color.BLACK);
            _progressText.setText(text);
        });
    }

    private static File makeOutputDir(File archiveFile) throws Throwable {
        String outputDirName = archiveFile.getName();
        if (outputDirName.endsWith(".aar") || outputDirName.endsWith(".AAR")) {
            outputDirName = outputDirName.substring(0,
                    outputDirName.length() - 4);
        }
        File outputDir = new File(archiveFile.getParentFile(), outputDirName);
        int n = 0;
        while (outputDir.exists()) {
            n++;
            outputDir = new File(archiveFile.getParentFile(),
                    outputDirName + "(" + n + ")");
        }

        if (outputDir.mkdirs()) {
            return outputDir;
        } else {
            throw new IOException("Failed to create directory: "
                    + outputDir.getAbsolutePath());
        }
    }
}
