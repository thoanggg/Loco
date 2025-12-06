package com.myapp.loco;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML private Button btnDashboard; @FXML private Button btnLogExplorer; @FXML private VBox dashboardView; @FXML private VBox logExplorerView;
    @FXML private Label lblActiveAgents; @FXML private TextField agentIpField; @FXML private Label scanStatusLabel;
    @FXML private TableView<Agent> agentTableView;
    @FXML private TableColumn<Agent, String> colAgentName; @FXML private TableColumn<Agent, String> colAgentIp; @FXML private TableColumn<Agent, String> colAgentUser;
    @FXML private TableColumn<Agent, String> colAgentStatus; @FXML private TableColumn<Agent, String> colAgentLastSeen; @FXML private TableColumn<Agent, Button> colAgentAction;
    @FXML private TextField currentTargetField; @FXML private ComboBox<String> logChannelComboBox; @FXML private Spinner<Integer> eventCountSpinner;
    @FXML private TextField xpathQueryField; @FXML private Button getLogsButton; @FXML private ToggleButton autoRefreshToggle; @FXML private ProgressIndicator loadingIndicator;
    @FXML private TableView<LogEvent> logTableView;
    @FXML private TableColumn<LogEvent, String> eventIdColumn; @FXML private TableColumn<LogEvent, String> timeColumn; @FXML private TableColumn<LogEvent, String> providerColumn;
    @FXML private TableColumn<LogEvent, String> levelColumn; @FXML private TableColumn<LogEvent, String> descriptionColumn;
    @FXML private TableColumn<LogEvent, String> userColumn; @FXML private TableColumn<LogEvent, String> hostColumn; // THÊM CỘT HOST

    private final ObservableList<Agent> agentList = FXCollections.observableArrayList();
    private Timeline autoRefreshTimeline; private Timeline agentHealthCheckTimeline;
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(java.time.Duration.ofSeconds(2)).build();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
    private final NetworkScanner networkScanner = new NetworkScanner();

    @FXML public void initialize() {
        setupDashboard(); setupLogExplorer();
        addAgentIfNotExists("127.0.0.1", "Online"); updateActiveAgentsCount();
        agentHealthCheckTimeline = new Timeline(new KeyFrame(Duration.seconds(15), e -> checkAllAgentsHealth()));
        agentHealthCheckTimeline.setCycleCount(Timeline.INDEFINITE); agentHealthCheckTimeline.play();
        PauseTransition pause = new PauseTransition(Duration.seconds(1)); pause.setOnFinished(event -> handleScanNetwork()); pause.play();
    }

    // --- LOGIC HEALTH CHECK (DASHBOARD) ---
    private void checkSingleAgentHealth(Agent agent) {
        String ip = agent.getIp();
        if ("localhost".equals(ip) || "127.0.0.1".equals(ip)) return;

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://" + ip + ":9876/ping")).GET().build();
        httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        String body = res.body();
                        if (body.startsWith("pong")) {
                            agent.setStatus("Online");
                            // Format mới từ Agent: pong | DOMAIN\User | Hostname
                            String[] parts = body.split("\\|");

                            if (parts.length > 1) {
                                String userRaw = parts[1].trim();
                                // Tách DOMAIN\User cho Dashboard
                                if (userRaw.contains("\\")) {
                                    String[] up = userRaw.split("\\\\");
                                    agent.setUser(up[1]); // Lấy User (vuduc)
                                    // Phần up[0] (DESKTOP-XXX) chính là Hostname
                                    agent.setName(up[0]);
                                } else {
                                    agent.setUser(userRaw);
                                }
                            }
                            // Nếu có phần Hostname riêng ở part 2 (fallback)
                            if (parts.length > 2 && agent.getName().equals("Checking...")) {
                                agent.setName(parts[2].trim());
                            }

                            agent.lastSeenProperty().set(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                        }
                    } else { agent.setStatus("Error"); }
                    agentTableView.refresh(); updateActiveAgentsCount();
                })).exceptionally(ex -> { Platform.runLater(() -> { agent.setStatus("Offline"); agentTableView.refresh(); updateActiveAgentsCount(); }); return null; });
    }

    private void checkAllAgentsHealth() { for (Agent agent : agentList) checkSingleAgentHealth(agent); }

    // --- LOGIC PARSING LOG (LOG EXPLORER) ---
    private List<LogEvent> parseLogEvents(String xmlOutput) throws Exception {
        List<LogEvent> events = new ArrayList<>();
        if (xmlOutput == null || xmlOutput.trim().isEmpty()) return events;
        String validXml = "<Events>" + xmlOutput + "</Events>";
        DocumentBuilder builder = xmlFactory.newDocumentBuilder(); InputSource is = new InputSource(new StringReader(validXml)); Document doc = builder.parse(is);
        NodeList eventNodes = doc.getElementsByTagName("Event");
        for (int i = 0; i < eventNodes.getLength(); i++) {
            Element eventElement = (Element) eventNodes.item(i);
            Element systemElement = (Element) eventElement.getElementsByTagName("System").item(0);
            String eventId = systemElement.getElementsByTagName("EventID").item(0).getTextContent();
            String timeCreated = ((Element) systemElement.getElementsByTagName("TimeCreated").item(0)).getAttribute("SystemTime");
            String providerName = ((Element) systemElement.getElementsByTagName("Provider").item(0)).getAttribute("Name");
            String level = systemElement.getElementsByTagName("Level").item(0).getTextContent();

            // Lấy Hostname từ thẻ <Computer> trong <System> (Chuẩn nhất)
            String computer = "Unknown";
            NodeList compNode = systemElement.getElementsByTagName("Computer");
            if (compNode.getLength() > 0) computer = compNode.item(0).getTextContent();

            Element eventDataElement = (Element) eventElement.getElementsByTagName("EventData").item(0);
            String user = "N/A"; String fullDetails = "";

            if (eventDataElement != null) {
                NodeList dataNodes = eventDataElement.getElementsByTagName("Data"); StringBuilder sb = new StringBuilder();
                for (int j = 0; j < dataNodes.getLength(); j++) {
                    Node node = dataNodes.item(j); Node nameAttr = node.getAttributes().getNamedItem("Name"); String val = node.getTextContent();
                    if (nameAttr != null) {
                        String name = nameAttr.getNodeValue();
                        sb.append(name).append(": ").append(val).append("\n");

                        // Tách User: DOMAIN\User -> User
                        if (("User".equals(name) || "TargetUserName".equals(name) || "SubjectUserName".equals(name)) && !val.equals("-")) {
                            if (val.contains("\\")) {
                                user = val.substring(val.indexOf("\\") + 1);
                            } else {
                                user = val;
                            }
                        }
                    } else sb.append(val).append("\n");
                }
                fullDetails = sb.toString().trim();
            }
            String description = fullDetails.split("\n")[0] + " [...]";
            events.add(new LogEvent(eventId, timeCreated, providerName, level, description, user, computer, fullDetails));
        }
        return events;
    }

    // ... (Giữ nguyên các phần khác) ...
    private void setupDashboard() {
        colAgentName.setCellValueFactory(new PropertyValueFactory<>("name")); colAgentIp.setCellValueFactory(new PropertyValueFactory<>("ip"));
        colAgentUser.setCellValueFactory(new PropertyValueFactory<>("user")); colAgentStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAgentLastSeen.setCellValueFactory(new PropertyValueFactory<>("lastSeen"));
        colAgentStatus.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) { super.updateItem(item, empty); if (empty || item == null) { setText(null); setGraphic(null); setStyle(""); } else { Label lbl = new Label(item); if ("Online".equals(item)) lbl.getStyleClass().add("status-badge-online"); else if ("Offline".equals(item)) lbl.getStyleClass().add("status-badge-offline"); else lbl.setStyle("-fx-text-fill: orange;"); setGraphic(lbl); } }
        });
        colAgentAction.setCellValueFactory(param -> { Button btn = new Button("Investigate"); btn.getStyleClass().add("button-primary"); btn.setOnAction(event -> switchToLogExplorer(param.getValue())); return new SimpleObjectProperty<>(btn); });
        agentTableView.setItems(agentList);
    }
    @FXML private void handleScanNetwork() {
        if (scanStatusLabel != null) scanStatusLabel.setText("Scanning all local networks...");
        Task<List<String>> scanTask = new Task<>() { @Override protected List<String> call() throws Exception { return networkScanner.scanAllNetworks(); } };
        scanTask.setOnSucceeded(e -> { List<String> foundIps = scanTask.getValue(); for (String ip : foundIps) addAgentIfNotExists(ip, "Online"); if (scanStatusLabel != null) scanStatusLabel.setText("Scan complete. Found " + foundIps.size() + " agents."); updateActiveAgentsCount(); });
        scanTask.setOnFailed(e -> { if (scanStatusLabel != null) scanStatusLabel.setText("Scan failed."); e.getSource().getException().printStackTrace(); });
        new Thread(scanTask).start();
    }
    @FXML private void handleAddAgent() { String ip = agentIpField.getText().trim(); if (!ip.isEmpty()) { addAgentIfNotExists(ip, "Unknown"); agentIpField.clear(); checkAllAgentsHealth(); } }
    private boolean addAgentIfNotExists(String ip, String initialStatus) {
        for (Agent a : agentList) if (a.getIp().equals(ip)) { a.setStatus("Online"); return false; }
        String name = ip.equals("127.0.0.1") ? "LOCAL-HOST" : "Checking..."; String user = "Checking...";
        agentList.add(new Agent(name, ip, initialStatus, user, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))); return true;
    }
    private void updateActiveAgentsCount() { lblActiveAgents.setText(String.valueOf(agentList.stream().filter(a -> "Online".equals(a.getStatus())).count())); }
    @FXML private void switchView(javafx.event.ActionEvent event) {
        if (event.getSource() == btnDashboard) { dashboardView.setVisible(true); logExplorerView.setVisible(false); btnDashboard.getStyleClass().add("active"); btnLogExplorer.getStyleClass().remove("active"); }
        else if (event.getSource() == btnLogExplorer) { dashboardView.setVisible(false); logExplorerView.setVisible(true); btnDashboard.getStyleClass().remove("active"); btnLogExplorer.getStyleClass().add("active"); }
    }
    private void switchToLogExplorer(Agent agent) {
        dashboardView.setVisible(false); logExplorerView.setVisible(true); btnDashboard.getStyleClass().remove("active"); btnLogExplorer.getStyleClass().add("active");
        if ("localhost".equals(agent.getIp()) || "127.0.0.1".equals(agent.getIp())) currentTargetField.setText("localhost"); else currentTargetField.setText(agent.getIp());
        handleGetLogs();
    }
    private void setupLogExplorer() {
        logChannelComboBox.setItems(FXCollections.observableArrayList("Microsoft-Windows-Sysmon/Operational", "Application", "Security", "System"));
        logChannelComboBox.setValue("Microsoft-Windows-Sysmon/Operational");
        eventIdColumn.setCellValueFactory(new PropertyValueFactory<>("eventId")); timeColumn.setCellValueFactory(new PropertyValueFactory<>("timeCreated"));
        providerColumn.setCellValueFactory(new PropertyValueFactory<>("providerName")); levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description")); userColumn.setCellValueFactory(new PropertyValueFactory<>("user"));
        hostColumn.setCellValueFactory(new PropertyValueFactory<>("host")); // MAP CỘT HOST
        logTableView.setRowFactory(tv -> { TableRow<LogEvent> row = new TableRow<>(); row.setOnMouseClicked(event -> { if (event.getClickCount() == 2 && (!row.isEmpty())) showLogDetails(row.getItem()); }); return row; });
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> handleGetLogs())); autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshToggle.selectedProperty().addListener((obs, oldVal, newVal) -> { if (newVal) autoRefreshTimeline.play(); else autoRefreshTimeline.stop(); });
    }
    @FXML private void handleGetLogs() {
        String target = currentTargetField.getText(); if (target == null || target.isEmpty()) { target = "localhost"; currentTargetField.setText("localhost"); }
        LogRequest request = new LogRequest(); request.setLogChannel(logChannelComboBox.getValue()); request.setEventCount(eventCountSpinner.getValue()); request.setXpathQuery(xpathQueryField.getText());
        Task<List<LogEvent>> task;
        if ("localhost".equals(target) || "127.0.0.1".equals(target)) task = createLocalTask(request);
        else { if (!target.startsWith("http")) target = "http://" + target + ":9876"; task = createRemoteTask(request, target); }
        task.setOnSucceeded(e -> { logTableView.setItems(FXCollections.observableArrayList(task.getValue())); loadingIndicator.setVisible(false); });
        task.setOnFailed(e -> { loadingIndicator.setVisible(false); showError("Fetch Error", "Failed to get logs", task.getException()); });
        loadingIndicator.setVisible(true); new Thread(task).start();
    }
    private Task<List<LogEvent>> createLocalTask(LogRequest request) {
        return new Task<>() { @Override protected List<LogEvent> call() throws Exception {
            List<String> command = new ArrayList<>(); command.add("wevtutil"); command.add("qe"); command.add(request.getLogChannel());
            command.add("/c:" + request.getEventCount()); command.add("/rd:true"); command.add("/f:xml");
            if (request.getXpathQuery() != null && !request.getXpathQuery().trim().isEmpty()) command.add("/q:" + request.getXpathQuery());
            ProcessBuilder pb = new ProcessBuilder(command); pb.redirectErrorStream(true); Process process = pb.start();
            StringBuilder xmlOutput = new StringBuilder(); try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) { String line; while ((line = reader.readLine()) != null) xmlOutput.append(line).append(System.lineSeparator()); }
            int exitCode = process.waitFor(); if (exitCode != 0) throw new RuntimeException("wevtutil error: " + xmlOutput);
            return parseLogEvents(xmlOutput.toString());
        }};
    }
    private Task<List<LogEvent>> createRemoteTask(LogRequest request, String hostUrl) {
        return new Task<>() { @Override protected List<LogEvent> call() throws Exception {
            String jsonBody = jsonMapper.writeValueAsString(request); HttpRequest req = HttpRequest.newBuilder().uri(URI.create(hostUrl + "/get-logs")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString()); if (res.statusCode() != 200) throw new RuntimeException("Agent Error: " + res.body());
            return parseLogEvents(res.body());
        }};
    }
    private void showLogDetails(LogEvent logEvent) { Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle("Details"); alert.setHeaderText(logEvent.getEventId()); TextArea area = new TextArea(logEvent.getFullDetails()); area.setEditable(false); area.setWrapText(true); GridPane.setVgrow(area, Priority.ALWAYS); GridPane.setHgrow(area, Priority.ALWAYS); GridPane content = new GridPane(); content.add(area, 0, 0); content.setMaxWidth(Double.MAX_VALUE); alert.getDialogPane().setExpandableContent(content); alert.getDialogPane().setExpanded(true); alert.showAndWait(); }
    private void showError(String t, String h, Throwable ex) { Platform.runLater(() -> { Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setHeaderText(h); a.setContentText(ex.getMessage()); a.showAndWait(); }); }
}