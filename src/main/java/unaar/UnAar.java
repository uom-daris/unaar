package unaar;

import java.io.File;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@SuppressWarnings("restriction")
public class UnAar extends Application {

    private static Logger _logger;

    static Logger logger() {
        if (_logger == null) {
            _logger = Logger.getLogger("unaar.log");
            try {
                FileHandler fh = new FileHandler("unaar.log");
                fh.setFormatter(new SimpleFormatter());
                _logger.addHandler(fh);
            } catch (Throwable e) {
                e.printStackTrace(System.err);
            }
        }
        return _logger;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private ObjectProperty<File> _macOpenFileProperty = new SimpleObjectProperty<File>();
    private Stage _stage;

    public UnAar() {
        if (PlatformUtil.isMac()) {
            _macOpenFileProperty.addListener((obs, oldValue, newValue) -> {
                if (newValue != null) {
                    new Thread(() -> {
                        while (_stage == null) {
                            try {
                                Thread.sleep(500);
                            } catch (Exception e) {
                                showError(e.getMessage(), true);
                            }
                        }
                        extractArchive(newValue, _stage);
                    }).start();
                }
            });
            com.sun.glass.ui.Application.GetApplication().setEventHandler(
                    new com.sun.glass.ui.Application.EventHandler() {
                        @Override
                        public void handleOpenFilesAction(
                                com.sun.glass.ui.Application app, long time,
                                String[] files) {
                            super.handleOpenFilesAction(app, time, files);
                            if (files != null && files.length > 0) {
                                if (files[0].indexOf(':') < 0
                                        && (files[0].endsWith(".aar")
                                                || files[0].endsWith(".AAR"))) {
                                    _macOpenFileProperty
                                            .set(new File(files[0]));
                                }
                            }

                        }
                    });
        }

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        _stage = primaryStage;
        _stage.initStyle(StageStyle.UTILITY);
        _stage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });
        List<String> params = getParameters().getUnnamed();
        if (params != null && !params.isEmpty()) {
            extractArchive(new File(params.get(0)), _stage);
        } else {
            if (PlatformUtil.isMac()) {
                // wait for 1 second
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        showError(e.getMessage(), true);
                    }
                    if (_macOpenFileProperty.getValue() == null) {
                        // no file open event received.
                        selectAndExtractArchive(_stage);
                    }
                }).start();
            } else {
                // Windows or Linux
                selectAndExtractArchive(_stage);
            }
        }
    }

    private static void selectAndExtractArchive(Stage stage) {
        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Arcitecta AAR file");
            fileChooser.getExtensionFilters()
                    .add(new ExtensionFilter("AAR Files", "*.aar", "*.AAR"));
            File af = fileChooser.showOpenDialog(stage);
            if (af != null) {
                extractArchive(af, stage);
            } else {
                Platform.exit();
            }
        });
    }

    private static void extractArchive(File af, Stage stage) {

        if (!af.exists() || !af.isFile() || !(af.getName().endsWith(".aar")
                || af.getName().endsWith(".AAR"))) {
            showError(
                    "File '" + af.getAbsolutePath() + "' is not an .aar file.",
                    true);
        } else {
            Platform.runLater(() -> {
                new UnAarGui(af).showOnStage(stage);
            });
        }
    }

    private static void showError(String msg, boolean exit) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(msg);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    Platform.exit();
                    System.exit(1);
                }
            });
        });
    }

}
