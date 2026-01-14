module com.myapp.loco {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;
    requires java.net.http;

    requires com.fasterxml.jackson.databind;
    requires org.yaml.snakeyaml;
    requires java.sql;

    opens com.myapp.loco to javafx.fxml, com.fasterxml.jackson.databind;

    exports com.myapp.loco;
}