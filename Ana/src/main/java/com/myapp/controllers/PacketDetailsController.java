package com.myapp.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class PacketDetailsController {

    @FXML
    private TextArea packetDetailsArea;

    public void setPacketDetails(String details) {
        packetDetailsArea.setText(details);
    }
}
