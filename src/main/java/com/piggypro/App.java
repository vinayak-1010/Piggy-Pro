package com.piggypro;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * App.java
 * ─────────────────────────────────────────────────────
 * JavaFX Application entry point.
 *
 * Responsibilities:
 *   - Create the primary Stage
 *   - Hand it to SceneManager
 *   - Navigate to the Login screen
 *
 * Window settings:
 *   - Title  : Piggy Pro
 *   - Size   : 1020 × 660  (login)  →  1100 × 680  (dashboard+)
 *   - Style  : DECORATED (native title bar)
 *   - Resize : false (fixed-size desktop app)
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Hand the stage to the scene manager before doing anything else
        SceneManager.init(primaryStage);

        // Window chrome
        primaryStage.setTitle("Piggy Pro");
        primaryStage.setResizable(false);
        primaryStage.initStyle(StageStyle.DECORATED);

        // Navigate to login — SceneManager will set the scene and show the stage
        SceneManager.navigateTo(SceneManager.Screen.LOGIN);

        primaryStage.show();
    }

    @Override
    public void stop() {
        // Called when the window is closed
        // TODO: flush any pending DB writes, close DB connection
        // DBConnection.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
