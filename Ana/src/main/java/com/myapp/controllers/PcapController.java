package com.myapp.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.Packet;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeoutException;

public class PcapController {

    @FXML
    private TableView<PacketWrapper> packetTable;

    @FXML
    private TextArea packetDetails;

    @FXML
    public void initialize() {
        TableColumn<PacketWrapper, String> timestampCol = new TableColumn<>("Timestamp");
        timestampCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimestamp()));

        TableColumn<PacketWrapper, String> srcAddrCol = new TableColumn<>("Source IP");
        srcAddrCol.setCellValueFactory(cellData -> {
            Packet packet = cellData.getValue().getPacket();
            if (packet.contains(IpV4Packet.class)) {
                return new SimpleStringProperty(packet.get(IpV4Packet.class).getHeader().getSrcAddr().getHostAddress());
            } else if (packet.contains(IpV6Packet.class)) {
                return new SimpleStringProperty(packet.get(IpV6Packet.class).getHeader().getSrcAddr().getHostAddress());
            }
            return new SimpleStringProperty("N/A");
        });

        TableColumn<PacketWrapper, String> dstAddrCol = new TableColumn<>("Destination IP");
        dstAddrCol.setCellValueFactory(cellData -> {
            Packet packet = cellData.getValue().getPacket();
            if (packet.contains(IpV4Packet.class)) {
                return new SimpleStringProperty(packet.get(IpV4Packet.class).getHeader().getDstAddr().getHostAddress());
            } else if (packet.contains(IpV6Packet.class)) {
                return new SimpleStringProperty(packet.get(IpV6Packet.class).getHeader().getDstAddr().getHostAddress());
            }
            return new SimpleStringProperty("N/A");
        });

        TableColumn<PacketWrapper, String> protocolCol = new TableColumn<>("Protocol");
        protocolCol.setCellValueFactory(cellData -> {
            Packet packet = cellData.getValue().getPacket();
            if (packet.contains(IpV4Packet.class)) {
                return new SimpleStringProperty(packet.get(IpV4Packet.class).getHeader().getProtocol().name());
            } else if (packet.contains(IpV6Packet.class)) {
                return new SimpleStringProperty(packet.get(IpV6Packet.class).getHeader().getNextHeader().name());
            }
            return new SimpleStringProperty("N/A");
        });


        packetTable.getColumns().addAll(timestampCol, srcAddrCol, dstAddrCol, protocolCol);

        packetTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        handlePacketSelection(newValue);
                    }
                });
    }

    public void handlePacketSelection(PacketWrapper selectedPacket) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/myapp/ana/views/packet-details-view.fxml"));
            Parent root = loader.load();

            PacketDetailsController controller = loader.getController();
            controller.setPacketDetails(selectedPacket.getPacket().toString());

            Stage stage = new Stage();
            stage.setTitle("Packet Details");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void loadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Pcap File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Pcap Files", "*.pcap", "*.cap"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            packetTable.getItems().clear();
            try (PcapHandle handle = Pcaps.openOffline(selectedFile.getAbsolutePath())) {
                while (true) {
                    try {
                        Packet packet = handle.getNextPacketEx();
                        Timestamp timestamp = new Timestamp(handle.getTimestamp().getTime());
                        packetTable.getItems().add(new PacketWrapper(packet, timestamp.toString()));
                    } catch (EOFException e) {
                        break; // End of file
                    } catch (TimeoutException e) {
                        // Ignore timeout and continue
                    }
                }
            } catch (PcapNativeException | NotOpenException e) {
                e.printStackTrace();
            }
        }
    }

    public void switchToStart(ActionEvent ae) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/myapp/ana/views/start-view.fxml"));
        Stage stage = (Stage)((Node)ae.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public static class PacketWrapper {
        private final Packet packet;
        private final String timestamp;

        public PacketWrapper(Packet packet, String timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }

        public Packet getPacket() {
            return packet;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }
}
