package org.example.patientmanagementsystem;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        // Ekran boyutu bilgisini sildik, setMaximized(true) ile tam ekran açılacak.
        Scene scene = new Scene(fxmlLoader.load());

        stage.setTitle("Hastalık Seyir Takip Sistemi");
        stage.setScene(scene);
        stage.setMaximized(true); // Uygulamayı tam ekran başlat
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}