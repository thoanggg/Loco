package com.myapp.loco;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.net.ssl.SSLContext;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javafx.beans.property.SimpleStringProperty;

public class MainController {

    static {
        // Disable hostname verification globally for the HttpClient
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
    }

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
            .getLogger(MainController.class.getName());

    // --- Constants ---
    private static final String STATUS_ONLINE = "Online";
    private static final String STATUS_OFFLINE = "Offline";
    private static final String STATUS_CHECKING = "Checking...";
    private static final String STATUS_UNKNOWN = "Unknown";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // --- Sidebar Buttons ---
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnLogExplorer;
    @FXML
    private Button btnRules;
    @FXML
    private Button btnAnalyze; // New Button

    // --- Views ---
    @FXML
    private VBox dashboardView;
    @FXML
    private VBox logExplorerView;
    @FXML
    private VBox rulesView;
    @FXML
    private VBox analyzeView; // New View
    @FXML
    private PieChart alertSeverityChart;
    @FXML
    private BarChart<String, Number> eventsByAgentChart;
    @FXML
    private TableView<LogEvent> alertTableView;
    @FXML
    private TableColumn<LogEvent, String> colAlertSeverity;
    @FXML
    private TableColumn<LogEvent, String> colAlertDetection;
    @FXML
    private TableColumn<LogEvent, String> colAlertDate;
    @FXML
    private TableColumn<LogEvent, String> colAlertHost;
    @FXML
    private TableColumn<LogEvent, Button> colAlertStatus;

    // --- Dashboard UI ---
    @FXML
    private Label lblActiveAgents;
    @FXML
    private Label lblTotalAlerts;
    @FXML
    private TextField agentIpField;
    @FXML
    private Label scanStatusLabel;
    @FXML
    private TableView<Agent> agentTableView;
    @FXML
    private TableColumn<Agent, String> colAgentName;
    @FXML
    private TableColumn<Agent, String> colAgentIp;
    @FXML
    private TableColumn<Agent, String> colAgentUser;
    @FXML
    private TableColumn<Agent, String> colAgentStatus;
    @FXML
    private TableColumn<Agent, String> colAgentLastSeen;
    @FXML
    private TableColumn<Agent, Button> colAgentAction;

    // --- Log Explorer UI ---
    @FXML
    private ComboBox<Agent> targetAgentCombo;
    @FXML
    private ComboBox<String> logChannelComboBox;
    @FXML
    private Button getLogsButton;
    @FXML
    private ToggleButton autoRefreshToggle;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private TableView<LogEvent> logTableView;
    @FXML
    private TableColumn<LogEvent, String> eventIdColumn;
    @FXML
    private TableColumn<LogEvent, String> timeColumn;
    @FXML
    private TableColumn<LogEvent, String> providerColumn;
    @FXML
    private TableColumn<LogEvent, String> levelColumn;
    @FXML
    private TableColumn<LogEvent, String> descriptionColumn;
    @FXML
    private TableColumn<LogEvent, String> userColumn;
    @FXML
    private TableColumn<LogEvent, String> hostColumn;

    @FXML
    private TextField filterUserField;
    @FXML
    private TextField filterEventIdField;
    @FXML
    private DatePicker filterStartDate;
    @FXML
    private DatePicker filterEndDate;

    // --- Rules Engine UI (Mới) ---
    @FXML
    private TableView<AdvancedRulesEngine.RuleMetadata> rulesTableView;
    @FXML
    private TableColumn<AdvancedRulesEngine.RuleMetadata, String> ruleNameCol;
    @FXML
    private TableColumn<AdvancedRulesEngine.RuleMetadata, String> ruleFieldCol;
    @FXML
    private TableColumn<AdvancedRulesEngine.RuleMetadata, String> ruleConditionCol;
    @FXML
    private TableColumn<AdvancedRulesEngine.RuleMetadata, String> ruleValueCol;
    @FXML
    private TableColumn<AdvancedRulesEngine.RuleMetadata, String> ruleSeverityCol;

    // --- Data & Helpers ---
    private final ObservableList<Agent> agentList = FXCollections.observableArrayList();
    private final ObservableList<LogEvent> masterLogList = FXCollections.observableArrayList();
    private FilteredList<LogEvent> filteredLogList;
    private final ObservableList<AdvancedRulesEngine.RuleMetadata> metadataList = FXCollections.observableArrayList(); // New
    // List

    private final Agent allAgents = new Agent("All Agents", "ALL", "Virtual", "", "");

    private final HttpClient httpClient = createInsecureHttpClient();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
    private final NetworkScanner networkScanner = new NetworkScanner();

    @FXML
    public void initialize() {
        setupDashboard();
        // GLOBAL FIX: Disable strict hostname verification for the internal HttpClient
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
        setupLogExplorer();
        setupRulesEngine(); // Init Rules
        setupAlertTable(); // Init Alert Table

        // Load Agents from DB
        agentList.setAll(DatabaseManager.getInstance().getAllAgents());
        // Load Alerts from DB
        masterLogList.setAll(DatabaseManager.getInstance().getAllAlerts());

        updateActiveAgentsCount();
        updateTargetCombo();
        agentList.addListener((ListChangeListener<Agent>) c -> updateTargetCombo());

        Timeline agentHealthCheckTimeline = new Timeline(
                new KeyFrame(Duration.seconds(15), e -> checkAllAgentsHealth()));
        agentHealthCheckTimeline.setCycleCount(Timeline.INDEFINITE);
        agentHealthCheckTimeline.play();

        // Auto-save alerts to DB when added
        masterLogList.addListener((ListChangeListener<LogEvent>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (LogEvent log : c.getAddedSubList()) {
                        if (log.isAlert()) {
                            DatabaseManager.getInstance().insertAlert(log);
                        }
                    }
                }
            }
        });

        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(event -> handleScanNetwork());
        pause.play();
    }

    // --- RULES ENGINE LOGIC ---
    private void setupRulesEngine() {
        // Configure Columns for Read-Only Rules View
        ruleNameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        ruleFieldCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMitreId()));
        ruleConditionCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription()));
        ruleSeverityCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSeverity()));
        ruleValueCol.setVisible(false); // Hidden column

        // Populate Table with Expert Rules
        metadataList.setAll(AdvancedRulesEngine.getRules());
        rulesTableView.setItems(metadataList);

        // Context Menu for Deletion
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete Rule");
        deleteItem.setOnAction(e -> handleDeleteRule());
        contextMenu.getItems().add(deleteItem);
        rulesTableView.setContextMenu(contextMenu);
    }

    private void handleDeleteRule() {
        AdvancedRulesEngine.RuleMetadata selected = rulesTableView.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        String ruleId = selected.getId();
        if (ruleId != null && ruleId.startsWith("sys-")) {
            showAlert("Action Denied", "System Rules cannot be deleted.");
            return;
        }

        boolean removed = AdvancedRulesEngine.removeSigmaRule(ruleId);
        if (removed) {
            showAlert("Success", "Rule deleted: " + selected.getName());
            setupRulesEngine(); // Refresh
        } else {
            showAlert("Error", "Could not delete rule.");
        }
    }

    @FXML
    private void handleAddRule() {
        // Legacy method - functionality removed in favor of AdvancedRulesEngine
    }

    private void applyRules(LogEvent event) {
        // Apply Advanced MITRE/Sigma Rules ONLY
        AdvancedRulesEngine.applyRules(event);
    }

    @FXML
    private TextArea sigmaRuleEditor;

    @FXML
    private void handleImportRule() {
        String yamlContent = sigmaRuleEditor.getText();
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            showAlert("Error", "Please paste a YAML rule first.");
            return;
        }
        doImportRule(yamlContent);
    }

    @FXML
    private void handleLoadRuleFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Sigma Rule File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Sigma YAML", "*.yaml", "*.yml"));
        File selectedFile = fileChooser.showOpenDialog(sigmaRuleEditor.getScene().getWindow());
        if (selectedFile != null) {
            try {
                String content = java.nio.file.Files.readString(selectedFile.toPath());
                doImportRule(content);
            } catch (Exception e) {
                showAlert("File Error", "Could not read file: " + e.getMessage());
            }
        }
    }

    private void doImportRule(String yamlContent) {
        try {
            com.myapp.loco.sigma.SigmaRule rule = com.myapp.loco.sigma.SigmaParser.parse(yamlContent);
            AdvancedRulesEngine.addSigmaRule(rule, yamlContent); // Pass YAML for persistence

            // Refresh Table
            setupRulesEngine(); // Re-populate

            sigmaRuleEditor.clear();
            showAlert("Success", "Rule imported: " + rule.getTitle());
        } catch (Exception e) {
            showAlert("Import Failed", "Invalid Sigma YAML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- NAVIGATION UPDATE ---
    @FXML
    private void switchView(javafx.event.ActionEvent event) {
        dashboardView.setVisible(false);
        logExplorerView.setVisible(false);
        rulesView.setVisible(false);
        if (analyzeView != null)
            analyzeView.setVisible(false); // Fix overlay bug

        btnDashboard.getStyleClass().remove("active");
        btnLogExplorer.getStyleClass().remove("active");
        btnRules.getStyleClass().remove("active");
        if (btnAnalyze != null)
            btnAnalyze.getStyleClass().remove("active");

        if (event.getSource() == btnDashboard) {
            dashboardView.setVisible(true);
            btnDashboard.getStyleClass().add("active");
        } else if (event.getSource() == btnLogExplorer) {
            logExplorerView.setVisible(true);
            btnLogExplorer.getStyleClass().add("active");
        } else if (event.getSource() == btnRules) {
            rulesView.setVisible(true);
            btnRules.getStyleClass().add("active");
        } else if (event.getSource() == btnAnalyze) {
            if (analyzeView != null)
                analyzeView.setVisible(true);
            if (btnAnalyze != null)
                btnAnalyze.getStyleClass().add("active");
            updateCharts();
        }
    }

    // --- LOG EXPLORER (CẬP NHẬT TÔ MÀU) ---
    private void setupLogExplorer() {
        setupLogControls();
        setupLogColumns();
        setupLogTableRows();
        setupLogFilters();
        setupAutoRefresh();
    }

    private void setupLogControls() {
        logChannelComboBox.setItems(FXCollections.observableArrayList(
                "Microsoft-Windows-Sysmon/Operational", "Application", "Security", "System"));
        logChannelComboBox.setValue("Microsoft-Windows-Sysmon/Operational");

        targetAgentCombo.setConverter(new StringConverter<Agent>() {
            @Override
            public String toString(Agent a) {
                if (a == null)
                    return "";
                if ("ALL".equals(a.getIp()))
                    return "All Agents";
                return a.getName() + " (" + a.getIp() + ")";
            }

            @Override
            public Agent fromString(String s) {
                return null;
            }
        });
    }

    private void setupLogColumns() {
        eventIdColumn.setCellValueFactory(new PropertyValueFactory<>("eventId"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timeCreated"));
        providerColumn.setCellValueFactory(new PropertyValueFactory<>("providerName"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        userColumn.setCellValueFactory(new PropertyValueFactory<>("user"));
        hostColumn.setCellValueFactory(new PropertyValueFactory<>("host"));
    }

    private void setupLogTableRows() {
        logTableView.setRowFactory(tv -> {
            TableRow<LogEvent> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty()))
                    showLogDetails(row.getItem());
            });

            row.itemProperty().addListener((obs, oldVal, newVal) -> updateRowColor(row, newVal));
            return row;
        });
    }

    private void updateRowColor(TableRow<LogEvent> row, LogEvent event) {
        if (event != null && event.isAlert()) {
            if ("High".equals(event.getAlertSeverity())) {
                row.setStyle("-fx-background-color: rgba(239, 83, 80, 0.3);"); // Red
            } else if ("Medium".equals(event.getAlertSeverity())) {
                row.setStyle("-fx-background-color: rgba(255, 167, 38, 0.3);"); // Orange
            } else {
                row.setStyle("-fx-background-color: rgba(255, 238, 88, 0.2);"); // Yellow
            }
        } else {
            row.setStyle("");
        }
    }

    private void setupLogFilters() {
        filteredLogList = new FilteredList<>(masterLogList, p -> true);
        SortedList<LogEvent> sortedData = new SortedList<>(filteredLogList);
        sortedData.comparatorProperty().bind(logTableView.comparatorProperty());
        logTableView.setItems(sortedData);

        filterUserField.textProperty().addListener((o, old, val) -> updateFilters());
        filterEventIdField.textProperty().addListener((o, old, val) -> updateFilters());
        filterStartDate.valueProperty().addListener((o, old, val) -> updateFilters());
        filterEndDate.valueProperty().addListener((o, old, val) -> updateFilters());
    }

    private void setupAutoRefresh() {
        Timeline autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> handleGetLogs()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal))
                autoRefreshTimeline.play();
            else
                autoRefreshTimeline.stop();
        });
    }

    // --- CÁC HÀM CÒN LẠI (GIỮ NGUYÊN) ---
    // --- Legacy methods removed ---

    // --- Persistence Handled via DatabaseManager ---

    private void setupDashboard() {
        colAgentName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAgentIp.setCellValueFactory(new PropertyValueFactory<>("ip"));
        colAgentUser.setCellValueFactory(new PropertyValueFactory<>("user"));
        colAgentStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAgentLastSeen.setCellValueFactory(new PropertyValueFactory<>("lastSeen"));
        colAgentStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    Label lbl = new Label(item);
                    if (STATUS_ONLINE.equals(item))
                        lbl.getStyleClass().add("status-badge-online");
                    else if (STATUS_OFFLINE.equals(item))
                        lbl.getStyleClass().add("status-badge-offline");
                    else
                        lbl.setStyle("-fx-text-fill: orange;");
                    setGraphic(lbl);
                }
            }
        });
        colAgentAction.setCellValueFactory(param -> {
            Button btn = new Button("Investigate");
            btn.getStyleClass().add("button-primary");
            btn.setOnAction(event -> switchToLogExplorer(param.getValue()));
            return new SimpleObjectProperty<>(btn);
        });
        agentTableView.setItems(agentList);

        // Context Menu for Deleting Agent
        ContextMenu ctx = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Remove Agent");
        deleteItem.setOnAction(e -> handleRemoveAgent());
        ctx.getItems().add(deleteItem);
        agentTableView.setContextMenu(ctx);
    }

    private void handleRemoveAgent() {
        Agent selected = agentTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            DatabaseManager.getInstance().removeAgent(selected.getIp()); // DB Remove
            agentList.remove(selected);
            updateActiveAgentsCount();
            checkAllAgentsHealth(); // Refresh scan logic
        }
    }

    private void updateTargetCombo() {
        Agent selected = targetAgentCombo.getValue();
        ObservableList<Agent> comboItems = FXCollections.observableArrayList();
        comboItems.add(allAgents);
        comboItems.addAll(agentList);
        targetAgentCombo.setItems(comboItems);
        if (selected != null && comboItems.contains(selected))
            targetAgentCombo.setValue(selected);
        else
            targetAgentCombo.setValue(allAgents);
    }

    private void checkSingleAgentHealth(Agent agent) {
        String ip = agent.getIp();
        if ("localhost".equals(ip) || "127.0.0.1".equals(ip))
            return;

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create("https://" + ip + ":9876/ping")).GET().build();
        httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> Platform.runLater(() -> handlePingResponse(agent, res)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> handlePingFailure(agent, ex));
                    return null;
                });
    }

    private void handlePingResponse(Agent agent, HttpResponse<String> res) {
        if (res.statusCode() == 200) {
            processAgentPong(agent, res.body());
        } else {
            agent.setStatus("Error: " + res.statusCode());
        }
        agentTableView.refresh();
        updateActiveAgentsCount();
    }

    private void handlePingFailure(Agent agent, Throwable ex) {
        LOGGER.warning("Ping failed for agent " + agent.getIp() + ": " + ex.getMessage());
        agent.setStatus(STATUS_OFFLINE);
        agentTableView.refresh();
        updateActiveAgentsCount();
    }

    private void processAgentPong(Agent agent, String body) {
        if (body.startsWith("pong")) {
            agent.setStatus(STATUS_ONLINE);
            String[] parts = body.split("\\|");
            boolean infoChanged = updateAgentDetails(agent, parts);

            agent.lastSeenProperty().set(LocalDateTime.now().format(TIME_FORMATTER));

            // Always update DB on success to capture LastSeen or Name/User changes
            // Or stick to original logic if performance concern, but SQLite is fast.
            if (infoChanged) {
                DatabaseManager.getInstance().upsertAgent(agent);
            }
        }
    }

    private boolean updateAgentDetails(Agent agent, String[] parts) {
        boolean infoChanged = false;
        if (parts.length > 1) {
            String userRaw = parts[1].trim();
            if (!agent.getUser().equals(userRaw)) {
                agent.setUser(userRaw);
                infoChanged = true;
            }
        }
        if (parts.length > 2 && STATUS_CHECKING.equals(agent.getName())) {
            agent.setName(parts[2].trim());
            infoChanged = true;
        }
        return infoChanged;
    }

    private void checkAllAgentsHealth() {
        for (Agent agent : agentList)
            checkSingleAgentHealth(agent);
    }

    @FXML
    private void handleAddAgent() {
        String ip = agentIpField.getText().trim();
        if (!ip.isEmpty()) {
            if (addAgentIfNotExists(ip, STATUS_UNKNOWN)) {
                // Find and save
                agentList.stream().filter(a -> a.getIp().equals(ip)).findFirst()
                        .ifPresent(a -> DatabaseManager.getInstance().upsertAgent(a));
            }
            agentIpField.clear();
            checkAllAgentsHealth();
        }
    }

    // ...

    // Note: handleScanNetwork calls uses inline task, we need to update that too.
    @FXML
    private void handleScanNetwork() {
        if (scanStatusLabel != null)
            scanStatusLabel.setText("Scanning all local networks...");
        Task<List<String>> scanTask = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                return networkScanner.scanAllNetworks();
            }
        };
        scanTask.setOnSucceeded(e -> {
            List<String> foundIps = scanTask.getValue();
            for (String ip : foundIps) {
                if (addAgentIfNotExists(ip, STATUS_ONLINE)) {
                    agentList.stream().filter(a -> a.getIp().equals(ip)).findFirst()
                            .ifPresent(agent -> DatabaseManager.getInstance().upsertAgent(agent));
                }
            }
            if (scanStatusLabel != null)
                scanStatusLabel.setText("Scan complete. Found " + foundIps.size() + " agents.");
            updateActiveAgentsCount();
        });
        scanTask.setOnFailed(e -> {
            if (scanStatusLabel != null)
                scanStatusLabel.setText("Scan failed.");
        });
        new Thread(scanTask).start();
    }

    private boolean addAgentIfNotExists(String ip, String initialStatus) {
        if ("127.0.0.1".equals(ip) || "localhost".equals(ip))
            return false; // Hide localhost
        for (Agent a : agentList)
            if (a.getIp().equals(ip)) {
                if (STATUS_ONLINE.equals(initialStatus))
                    a.setStatus(STATUS_ONLINE);
                return false;
            }
        String name = STATUS_CHECKING;
        String user = STATUS_CHECKING;
        agentList.add(new Agent(name, ip, initialStatus, user,
                LocalDateTime.now().format(TIME_FORMATTER)));
        return true;
    }

    private void updateActiveAgentsCount() {
        lblActiveAgents
                .setText(String.valueOf(agentList.stream().filter(a -> STATUS_ONLINE.equals(a.getStatus())).count()));
    }

    private void switchToLogExplorer(Agent agent) {
        dashboardView.setVisible(false);
        logExplorerView.setVisible(true);
        rulesView.setVisible(false);
        btnDashboard.getStyleClass().remove("active");
        btnLogExplorer.getStyleClass().add("active");
        btnRules.getStyleClass().remove("active");
        targetAgentCombo.setValue(agent);
        handleGetLogs();
    }

    private void updateFilters() {
        String userFilter = filterUserField.getText().toLowerCase();
        String eventIdFilter = filterEventIdField.getText().toLowerCase();
        LocalDate startDate = filterStartDate.getValue();
        LocalDate endDate = filterEndDate.getValue();
        filteredLogList.setPredicate(log -> {
            if (!userFilter.isEmpty() && (log.getUser() == null || !log.getUser().toLowerCase().contains(userFilter)))
                return false;
            if (!eventIdFilter.isEmpty() && !log.getEventId().equals(eventIdFilter))
                return false;
            if (startDate != null || endDate != null) {
                try {
                    String logDateStr = log.getTimeCreated().substring(0, 10);
                    LocalDate logDate = LocalDate.parse(logDateStr);
                    if (startDate != null && logDate.isBefore(startDate))
                        return false;
                    if (endDate != null && logDate.isAfter(endDate))
                        return false;
                } catch (Exception e) {
                    LOGGER.warning("Invalid date format in log: " + log.getTimeCreated());
                }
            }
            return true;
        });
    }

    @FXML
    private void handleResetFilters() {
        filterUserField.clear();
        filterEventIdField.clear();
        filterStartDate.setValue(null);
        filterEndDate.setValue(null);
    }

    @FXML
    private void handleClearTarget() {
        targetAgentCombo.setValue(allAgents);
    }

    private void updateCharts() {
        if (analyzeView == null || !analyzeView.isVisible())
            return;

        // 1. Severity Distribution
        Map<String, Integer> severityCounts = new HashMap<>();
        // 2. Events per Agent
        Map<String, Integer> agentCounts = new HashMap<>();

        for (LogEvent log : masterLogList) {
            // Severity
            String sev = "Info";
            if (log.isAlert()) {
                sev = log.getAlertSeverity() != null ? log.getAlertSeverity() : "Alert";
            } else if (log.getLevel() != null && !log.getLevel().isEmpty()) {
                sev = log.getLevel();
            }
            severityCounts.put(sev, severityCounts.getOrDefault(sev, 0) + 1);

            // Agent
            String agent = log.getHost() != null ? log.getHost() : "Unknown";
            agentCounts.put(agent, agentCounts.getOrDefault(agent, 0) + 1);
        }

        // Update PieChart
        if (alertSeverityChart != null) {
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : severityCounts.entrySet()) {
                pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }
            alertSeverityChart.setData(pieData);
        }

        // Update BarChart
        if (eventsByAgentChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Events");
            for (Map.Entry<String, Integer> entry : agentCounts.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            eventsByAgentChart.getData().clear();
            eventsByAgentChart.getData().add(series);
        }

        // Update Alert Table
        if (alertTableView != null) {
            ObservableList<LogEvent> alerts = FXCollections.observableArrayList();
            for (LogEvent log : masterLogList) {
                if (log.isAlert())
                    alerts.add(log);
            }
            alertTableView.setItems(alerts);
        }
    }

    private void setupAlertTable() {
        if (alertTableView == null)
            return;

        setupAlertColumns();
        setupAlertRowFactory();
    }

    private void setupAlertColumns() {
        colAlertSeverity.setCellValueFactory(new PropertyValueFactory<>("alertSeverity"));
        colAlertSeverity.setCellFactory(column -> createSeverityCell());

        colAlertDetection.setCellValueFactory(new PropertyValueFactory<>("detectionName"));
        colAlertHost.setCellValueFactory(new PropertyValueFactory<>("host"));

        colAlertDate.setCellValueFactory(new PropertyValueFactory<>("timeCreated"));
        colAlertDate.setCellFactory(column -> createDateCell());

        colAlertStatus.setCellValueFactory(param -> createStatusButtonProperty(param.getValue()));
    }

    private void setupAlertRowFactory() {
        alertTableView.setRowFactory(tv -> {
            TableRow<LogEvent> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && (!row.isEmpty()))
                    showLogDetails(row.getItem());
            });
            return row;
        });
    }

    private TableCell<LogEvent, String> createSeverityCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("● " + item);
                    updateSeverityStyle(this, item);
                }
            }
        };
    }

    private void updateSeverityStyle(TableCell<?, ?> cell, String severity) {
        if ("High".equals(severity))
            cell.setStyle("-fx-text-fill: #ef5350; -fx-font-weight: bold;");
        else if ("Medium".equals(severity))
            cell.setStyle("-fx-text-fill: #ffa726; -fx-font-weight: bold;");
        else if ("Low".equals(severity))
            cell.setStyle("-fx-text-fill: #ffee58; -fx-font-weight: bold;");
        else
            cell.setStyle("-fx-text-fill: white;");
    }

    private TableCell<LogEvent, String> createDateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setText(null);
                else
                    setText(formatAlertDate(item));
            }
        };
    }

    private String formatAlertDate(String rawDate) {
        try {
            LocalDateTime dt = LocalDateTime.parse(rawDate.substring(0, 19));
            String month = dt.getMonth().name().substring(0, 1).toUpperCase()
                    + dt.getMonth().name().substring(1, 3).toLowerCase();
            int day = dt.getDayOfMonth();
            String suffix = getDaySuffix(day);
            return String.format("%s %d%s %d at %02d:%02d",
                    month, day, suffix, dt.getYear(), dt.getHour(), dt.getMinute());
        } catch (Exception e) {
            return rawDate;
        }
    }

    private String getDaySuffix(int day) {
        if (day % 10 == 1 && day != 11)
            return "st";
        if (day % 10 == 2 && day != 12)
            return "nd";
        if (day % 10 == 3 && day != 13)
            return "rd";
        return "th";
    }

    private SimpleObjectProperty<Button> createStatusButtonProperty(LogEvent log) {
        Button btn = new Button(log.getStatus());
        btn.getStyleClass().add("status-badge-offline"); // Default base class
        updateStatusButtonStyle(btn, log.getStatus());

        btn.setOnAction(event -> toggleAlertStatus(log, btn));
        return new SimpleObjectProperty<>(btn);
    }

    private void updateStatusButtonStyle(Button btn, String status) {
        if ("Acknowledged".equals(status)) {
            btn.setStyle("-fx-background-color: rgba(76, 175, 80, 0.2); -fx-text-fill: #66bb6a;");
        } else {
            btn.setStyle("-fx-background-color: rgba(150, 150, 150, 0.2); -fx-text-fill: #cfd8dc;");
        }
    }

    private void toggleAlertStatus(LogEvent log, Button btn) {
        String newStatus = "Acknowledged".equals(log.getStatus()) ? "Not Acknowledged" : "Acknowledged";
        log.setStatus(newStatus);
        btn.setText(newStatus);
        updateStatusButtonStyle(btn, newStatus);
        DatabaseManager.getInstance().updateAlertStatus(log);
    }

    @FXML
    private void handleGetLogs() {
        Agent selectedTarget = targetAgentCombo.getValue();
        final boolean fetchAll;
        final String targetIp;
        if (selectedTarget == null || "ALL".equals(selectedTarget.getIp())) {
            fetchAll = true;
            targetIp = "ALL";
        } else {
            fetchAll = false;
            targetIp = selectedTarget.getIp();
        }
        String logChannel = logChannelComboBox.getValue();
        LogRequest request = new LogRequest();
        request.setLogChannel(logChannel);
        Task<List<LogEvent>> task = new Task<>() {
            @Override
            protected List<LogEvent> call() throws Exception {
                List<LogEvent> allLogs = new ArrayList<>();
                ExecutorService executor = Executors.newFixedThreadPool(10);
                List<Future<List<LogEvent>>> futures = new ArrayList<>();
                for (Agent agent : agentList) {
                    if ("Offline".equals(agent.getStatus()) || (!fetchAll && !agent.getIp().equals(targetIp)))
                        continue;
                    Callable<List<LogEvent>> job = () -> {
                        try {
                            if ("127.0.0.1".equals(agent.getIp()))
                                return fetchLocalLogs();
                            else
                                return fetchRemoteLogs(request, "https://" + agent.getIp() + ":9876");
                        } catch (Exception e) {
                            return new ArrayList<>();
                        }
                    };
                    futures.add(executor.submit(job));
                }
                for (Future<List<LogEvent>> f : futures) {
                    try {
                        allLogs.addAll(f.get());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                executor.shutdown();
                allLogs.sort((o1, o2) -> o2.getTimeCreated().compareTo(o1.getTimeCreated()));
                return allLogs;
            }
        };
        task.setOnSucceeded(e -> {
            List<LogEvent> fetchedLogs = task.getValue();
            // Preserve historical alerts from DB
            List<LogEvent> dbAlerts = DatabaseManager.getInstance().getAllAlerts();

            Map<String, LogEvent> uniqueEvents = new java.util.HashMap<>();

            // Prioritize fetched logs (might be fresher?)
            for (LogEvent log : fetchedLogs) {
                // Key: Host + EventID + Time (Composite Key)
                String key = log.getHost() + "_" + log.getEventId() + "_" + log.getTimeCreated();
                uniqueEvents.put(key, log);
            }

            for (LogEvent alert : dbAlerts) {
                String key = alert.getHost() + "_" + alert.getEventId() + "_" + alert.getTimeCreated();
                uniqueEvents.putIfAbsent(key, alert);
            }

            masterLogList.setAll(new ArrayList<>(uniqueEvents.values()));

            loadingIndicator.setVisible(false);
            updateCharts(); // Refresh charts when logs arrive
        });
        task.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            showError("Fetch Error", "Error collecting logs", task.getException());
        });
        loadingIndicator.setVisible(true);
        new Thread(task).start();
    }

    private List<LogEvent> fetchLocalLogs() {
        // Admin is running on Linux/Ubuntu, so we cannot Use wevtutil (Windows).
        // For now, we return an empty list or a system notification event.
        // In the future, we could map this to /var/log/syslog if desired.
        LOGGER.info("Admin is on Linux. Skipping local Windows log fetch.");
        return new ArrayList<>();
    }

    private List<LogEvent> fetchRemoteLogs(LogRequest request, String hostUrl) throws Exception {
        String jsonBody = jsonMapper.writeValueAsString(request);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(hostUrl + "/get-logs"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new java.io.IOException("Remote Error: " + res.body());
        return parseLogEvents(res.body());
    }

    private List<LogEvent> parseLogEvents(String xmlOutput) throws Exception {
        List<LogEvent> events = new ArrayList<>();
        if (xmlOutput == null || xmlOutput.trim().isEmpty())
            return events;
        String validXml = "<Events>" + xmlOutput + "</Events>";
        DocumentBuilder builder = xmlFactory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(validXml));
        Document doc = builder.parse(is);
        NodeList eventNodes = doc.getElementsByTagName("Event");
        for (int i = 0; i < eventNodes.getLength(); i++) {
            Element eventElement = (Element) eventNodes.item(i);
            Element systemElement = (Element) eventElement.getElementsByTagName("System").item(0);
            String eventId = systemElement.getElementsByTagName("EventID").item(0).getTextContent();
            String timeCreated = ((Element) systemElement.getElementsByTagName("TimeCreated").item(0))
                    .getAttribute("SystemTime");
            String providerName = ((Element) systemElement.getElementsByTagName("Provider").item(0))
                    .getAttribute("Name");
            String level = systemElement.getElementsByTagName("Level").item(0).getTextContent();
            String computer = "Unknown";
            NodeList compNode = systemElement.getElementsByTagName("Computer");
            if (compNode.getLength() > 0)
                computer = compNode.item(0).getTextContent();
            Element eventDataElement = (Element) eventElement.getElementsByTagName("EventData").item(0);
            String user = "N/A";
            String fullDetails = "";
            java.util.Map<String, String> dataMap = new java.util.HashMap<>();

            if (eventDataElement != null) {
                NodeList dataNodes = eventDataElement.getElementsByTagName("Data");
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < dataNodes.getLength(); j++) {
                    Node node = dataNodes.item(j);
                    Node nameAttr = node.getAttributes().getNamedItem("Name");
                    String val = node.getTextContent();
                    if (nameAttr != null) {
                        String name = nameAttr.getNodeValue();
                        sb.append(name).append(": ").append(val).append("\n");
                        dataMap.put(name, val); // Store for Rules Engine

                        if (("User".equals(name) || "TargetUserName".equals(name) || "SubjectUserName".equals(name))
                                && !val.equals("-")) {
                            if (val.contains("\\"))
                                user = val.substring(val.indexOf("\\") + 1);
                            else
                                user = val;
                        }
                    } else {
                        sb.append(val).append("\n");
                        // For data without Name attribute (rare in structured logs but possible)
                        dataMap.put("UnknownData" + j, val);
                    }
                }
                fullDetails = sb.toString().trim();
            }
            String description = fullDetails.split("\n")[0] + " [...]";
            LogEvent log = new LogEvent(eventId, timeCreated, providerName, level, description, user, computer,
                    fullDetails, dataMap);
            applyRules(log);
            events.add(log);
        }
        return events;
    }

    private void showLogDetails(LogEvent logEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Details");
        alert.setHeaderText("Event " + logEvent.getEventId() + " - " + logEvent.getDetectionName()); // Show Detection
                                                                                                     // Name
        TextArea area = new TextArea(logEvent.getFullDetails());
        area.setEditable(false);
        area.setWrapText(true);
        GridPane.setVgrow(area, Priority.ALWAYS);
        GridPane.setHgrow(area, Priority.ALWAYS);
        GridPane content = new GridPane();
        content.add(area, 0, 0);
        content.setMaxWidth(Double.MAX_VALUE);
        alert.getDialogPane().setExpandableContent(content);
        alert.getDialogPane().setExpanded(true);
        alert.showAndWait();
    }

    private void showError(String t, String h, Throwable ex) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(t);
            a.setHeaderText(h);
            a.setContentText(ex.getMessage());
            a.showAndWait();
        });
    }

    private static HttpClient createInsecureHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());

            return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .sslContext(sc)
                    .build();
        } catch (Exception e) {
            // Log error instead of printStackTrace
            java.util.logging.Logger.getLogger(MainController.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Failed to initialize insecure HttpClient", e);
            return HttpClient.newHttpClient();
        }
    }
}