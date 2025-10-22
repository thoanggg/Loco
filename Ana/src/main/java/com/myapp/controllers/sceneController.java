package com.myapp.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


public class sceneController {
    private Parent root;
    private Stage stage;
    private Scene scene;

    public void switchToStart(ActionEvent ae) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/com/myapp/ana/views/start-view.fxml"));
        stage = (Stage)((Node)ae.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void switchToAna(ActionEvent ae) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/myapp/ana/views/ana-view.fxml"));
        stage = (Stage)((Node)ae.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void switchToPcap(ActionEvent ae) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/myapp/ana/views/pcap-view.fxml"));
        stage = (Stage)((Node)ae.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
