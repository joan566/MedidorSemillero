package com.playground.fondoahorro;

import com.playground.fondoahorro.infrastructure.database.DatabaseManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Exception startupError;

    @Override
    public void init() {
        try {
            DatabaseManager.initialize();
        } catch (Exception e) {
            // Caught here (rather than left to propagate) so start() can still
            // open a window and show a human message instead of the app dying
            // silently before any UI exists.
            startupError = e;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        if (startupError != null) {
            log.error("Error crítico al iniciar la aplicación", startupError);
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "No fue posible iniciar la aplicación porque hubo un problema con la base de datos.\n"
                            + "Por favor cierra el programa e inténtalo de nuevo. Si el problema continúa, "
                            + "contacta a soporte técnico.",
                    ButtonType.OK);
            alert.setTitle("No fue posible iniciar");
            alert.setHeaderText(null);
            alert.showAndWait();
            Platform.exit();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/shell.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

            primaryStage.setTitle("Fondo de Ahorro");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(650);
            primaryStage.show();

            log.info("Aplicación iniciada.");
        } catch (Exception e) {
            log.error("Error crítico al iniciar la aplicación", e);
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "No fue posible abrir la ventana principal de la aplicación.\n"
                            + "Por favor cierra el programa e inténtalo de nuevo.",
                    ButtonType.OK);
            alert.setTitle("No fue posible iniciar");
            alert.setHeaderText(null);
            alert.showAndWait();
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
