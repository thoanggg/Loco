module com.myapp.ana {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires org.pcap4j.core;
    /* requires org.pcap4j.packetfactory.static; */

    opens com.myapp.ana to javafx.fxml;
    exports com.myapp.ana;
    exports com.myapp.controllers;
    opens com.myapp.controllers to javafx.fxml;
}
