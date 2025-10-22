package com.myapp.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.shape.Circle;

public class testController {
    @FXML
    private Circle myCircle;
    private double x,y;

    public void up(ActionEvent ae) {
        System.out.println("up");
        myCircle.setCenterY(y=y-1);
    }
    public void right(ActionEvent ae) {
        System.out.println("right");
        myCircle.setCenterX(x=x+1);
    }
    public void down(ActionEvent ae) {
        System.out.println("down");
        myCircle.setCenterY(y=y+1);
    }
    public void left(ActionEvent ae) {
        System.out.println("left");
        myCircle.setCenterX(x=x-1);
    }
}
