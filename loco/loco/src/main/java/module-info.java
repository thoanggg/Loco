module com.myapp.loco {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;
    requires java.net.http;

    requires com.fasterxml.jackson.databind;

    opens com.myapp.loco to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.myapp.loco;
}