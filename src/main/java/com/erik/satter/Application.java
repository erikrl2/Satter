package com.erik.satter;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Satter");
        stage.setMinWidth(420);
        stage.setMinHeight(400);
        stage.setOnCloseRequest(e -> ((Controller) fxmlLoader.getController()).solverThread.shutdownNow());
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}