package com.myapp.loco;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
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
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    // --- FXML Control Fields ---
    @FXML private ToggleGroup modeToggleGroup;
    @FXML private RadioButton localModeRadio;
    @FXML private RadioButton remoteModeRadio;
    @FXML private TextField remoteHostField;
    @FXML private ComboBox<String> logChannelComboBox;
    @FXML private Spinner<Integer> eventCountSpinner;
    @FXML private TextField xpathQueryField;
    @FXML private Button getLogsButton;
    @FXML private ToggleButton autoRefreshToggle;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private TableView<LogEvent> logTableView;
    @FXML private TableColumn<LogEvent, String> eventIdColumn;
    @FXML private TableColumn<LogEvent, String> timeColumn;
    @FXML private TableColumn<LogEvent, String> providerColumn;
    @FXML private TableColumn<LogEvent, String> levelColumn;
    @FXML private TableColumn<LogEvent, String> descriptionColumn;

    // --- Class Fields ---
    private Timeline autoRefreshTimeline;
    private final DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
    // Khởi tạo HttpClient và ObjectMapper một lần để tái sử dụng
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @FXML
    public void initialize() {
        // Cấu hình ComboBox
        logChannelComboBox.setItems(FXCollections.observableArrayList(
                "Application",
                "Security",
                "System",
                "Microsoft-Windows-Sysmon/Operational"
        ));
        logChannelComboBox.setValue("Application");

        // Cấu hình TableView Columns
        eventIdColumn.setCellValueFactory(new PropertyValueFactory<>("eventId"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timeCreated"));
        providerColumn.setCellValueFactory(new PropertyValueFactory<>("providerName"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Sửa lỗi: Cài đặt co dãn cột bằng code Java (thay vì FXML)
        logTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Xử lý Double-click trên hàng của TableView
        logTableView.setRowFactory(tv -> {
            TableRow<LogEvent> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    LogEvent rowData = row.getItem();
                    showLogDetails(rowData);
                }
            });
            return row;
        });

        // Cấu hình Auto-Refresh
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> handleGetLogs()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);

        autoRefreshToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            setControlsDisabled(newVal); // Tắt/bật các nút
            if (newVal) {
                autoRefreshTimeline.play();
            } else {
                autoRefreshTimeline.stop();
            }
        });

        // Cấu hình chuyển đổi Mode Local/Remote
        modeToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == remoteModeRadio) {
                remoteHostField.setDisable(false);
            } else {
                remoteHostField.setDisable(true);
            }
        });
    }

    /**
     * Tắt/bật các điều khiển khi đang auto-refresh hoặc đang tải
     */
    private void setControlsDisabled(boolean isDisabled) {
        getLogsButton.setDisable(isDisabled);
        logChannelComboBox.setDisable(isDisabled);
        eventCountSpinner.setDisable(isDisabled);
        xpathQueryField.setDisable(isDisabled);
        // Vô hiệu hóa trường host nếu đang ở local mode HOẶC nếu auto-refresh đang bật
        remoteHostField.setDisable(isDisabled || localModeRadio.isSelected());
        localModeRadio.setDisable(isDisabled);
        remoteModeRadio.setDisable(isDisabled);
    }

    @FXML
    private void handleGetLogs() {
        String logChannel = logChannelComboBox.getValue();
        int eventCount = eventCountSpinner.getValue();
        String xpathQuery = xpathQueryField.getText();

        // Tạo một đối tượng Request (POJO)
        LogRequest logRequest = new LogRequest();
        logRequest.setLogChannel(logChannel);
        logRequest.setEventCount(eventCount);
        logRequest.setXpathQuery(xpathQuery);

        // Tạo Task (luồng nền)
        Task<List<LogEvent>> task;
        if (remoteModeRadio.isSelected()) {
            String host = remoteHostField.getText();
            task = createRemoteTask(logRequest, host); // Lấy log từ xa
        } else {
            task = createLocalTask(logRequest); // Lấy log nội bộ
        }

        // Xử lý khi Task thành công
        task.setOnSucceeded(e -> {
            logTableView.setItems(FXCollections.observableArrayList(task.getValue()));
            loadingIndicator.setVisible(false);
        });

        // Xử lý khi Task thất bại
        task.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            showError("Lỗi Lấy Logs", "Không thể lấy dữ liệu.", task.getException());
        });

        // Chạy Task
        loadingIndicator.setVisible(true);
        new Thread(task).start();
    }

    /**
     * Tạo Task để chạy wevtutil nội bộ (Local Mode)
     */
    private Task<List<LogEvent>> createLocalTask(LogRequest request) {
        return new Task<>() {
            @Override
            protected List<LogEvent> call() throws Exception {
                List<String> command = new ArrayList<>();
                command.add("wevtutil");
                command.add("qe"); // query-events
                command.add(request.getLogChannel());
                command.add("/c:" + request.getEventCount());
                command.add("/rd:true"); // reverse direction
                command.add("/f:xml"); // format

                // Thêm lọc XPath nếu có
                if (request.getXpathQuery() != null && !request.getXpathQuery().trim().isEmpty()) {
                    command.add("/q:" + request.getXpathQuery());
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                StringBuilder xmlOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        xmlOutput.append(line).append(System.lineSeparator());
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("wevtutil exited with code: " + exitCode + "\nOutput: " + xmlOutput);
                }

                return parseLogEvents(xmlOutput.toString());
            }
        };
    }

    /**
     * Tạo Task để gửi yêu cầu đến Agent từ xa (Remote Mode)
     */
    private Task<List<LogEvent>> createRemoteTask(LogRequest request, String hostUrl) {
        return new Task<>() {
            @Override
            protected List<LogEvent> call() throws Exception {
                if (hostUrl == null || hostUrl.trim().isEmpty() || !hostUrl.startsWith("http")) {
                    throw new IllegalArgumentException("Host URL của Agent không hợp lệ. Phải bắt đầu bằng http:// hoặc https://");
                }

                // 1. Chuyển request object thành JSON
                String jsonBody = jsonMapper.writeValueAsString(request);

                // 2. Tạo HTTP Request
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(hostUrl + "/get-logs")) // Endpoint là /get-logs
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                // 3. Gửi request và nhận response
                // Lưu ý: httpClient đã được khởi tạo 1 lần
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                // 4. Xử lý response
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Agent trả về lỗi (Code: " + response.statusCode() + "): " + response.body());
                }

                // Agent trả về XML, chúng ta phân tích nó
                String xmlOutput = response.body();
                return parseLogEvents(xmlOutput);
            }
        };
    }


    /**
     * Phân tích chuỗi XML thành danh sách các LogEvent
     * Dùng chung cho cả Local và Remote
     */
    private List<LogEvent> parseLogEvents(String xmlOutput) throws Exception {
        List<LogEvent> events = new ArrayList<>();
        // Bao bọc output trong một thẻ root để đảm bảo XML hợp lệ
        String validXml = "<Events>" + xmlOutput + "</Events>";

        DocumentBuilder builder = xmlFactory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(validXml));
        Document doc = builder.parse(is);

        NodeList eventNodes = doc.getElementsByTagName("Event");
        for (int i = 0; i < eventNodes.getLength(); i++) {
            Node eventNode = eventNodes.item(i);
            if (eventNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eventElement = (Element) eventNode;

                // Trích xuất từ <System>
                Element systemElement = (Element) eventElement.getElementsByTagName("System").item(0);
                String eventId = systemElement.getElementsByTagName("EventID").item(0).getTextContent();
                String timeCreated = ((Element) systemElement.getElementsByTagName("TimeCreated").item(0)).getAttribute("SystemTime");
                String providerName = ((Element) systemElement.getElementsByTagName("Provider").item(0)).getAttribute("Name");
                String level = systemElement.getElementsByTagName("Level").item(0).getTextContent();

                // Trích xuất từ <EventData>
                Element eventDataElement = (Element) eventElement.getElementsByTagName("EventData").item(0);
                String fullDetails;
                String description;

                if ("Microsoft-Windows-Sysmon".equals(providerName)) {
                    fullDetails = parseSysmonEventData(eventDataElement, eventId);
                } else {
                    fullDetails = parseGenericEventData(eventDataElement);
                }

                // Lấy dòng đầu tiên làm mô tả tóm tắt
                description = fullDetails.split("\n")[0] + " [...]";
                events.add(new LogEvent(eventId, timeCreated, providerName, level, description, fullDetails));
            }
        }
        return events;
    }

    /**
     * Phân tích EventData cho log Sysmon (có định dạng "Name")
     */
    private String parseSysmonEventData(Element eventDataElement, String eventId) {
        if (eventDataElement == null) return "No EventData";
        StringBuilder details = new StringBuilder();

        // Thêm tiêu đề dựa trên Event ID
        switch (eventId) {
            case "1": details.append("[Process Create]\n"); break;
            case "3": details.append("[Network Connect]\n"); break;
            case "7": details.append("[Image Load]\n"); break;
            case "8": details.append("[Create Remote Thread]\n"); break;
            case "10": details.append("[Process Access]\n"); break;
            case "11": details.append("[File Create]\n"); break;
            case "22": details.append("[DNS Query]\n"); break;
            default: details.append("[Sysmon Event ID: ").append(eventId).append("]\n");
        }

        NodeList dataNodes = eventDataElement.getElementsByTagName("Data");
        for (int j = 0; j < dataNodes.getLength(); j++) {
            Node dataNode = dataNodes.item(j);
            Node nameAttr = dataNode.getAttributes().getNamedItem("Name");
            if (nameAttr != null) {
                details.append(nameAttr.getNodeValue()).append(": ").append(dataNode.getTextContent()).append("\n");
            }
        }
        return details.toString().trim();
    }

    /**
     * Phân tích EventData cho log chung (Application, System)
     */
    private String parseGenericEventData(Element eventDataElement) {
        if (eventDataElement == null) return "No EventData";
        StringBuilder details = new StringBuilder();
        NodeList dataNodes = eventDataElement.getElementsByTagName("Data");
        for (int j = 0; j < dataNodes.getLength(); j++) {
            Node dataNode = dataNodes.item(j);
            Node nameAttr = dataNode.getAttributes().getNamedItem("Name");
            if (nameAttr != null) {
                // Định dạng có tên (Ví dụ: một số log PowerShell)
                details.append(nameAttr.getNodeValue()).append(": ").append(dataNode.getTextContent()).append("\n");
            } else {
                // Định dạng không tên (Ví dụ: Application)
                details.append(dataNode.getTextContent()).append("\n");
            }
        }
        // Nếu không có thẻ <Data> nào, trả về chuỗi rỗng thay vì "No EventData"
        if (details.length() == 0) return "No specific data";
        return details.toString().trim();
    }

    /**
     * Hiển thị cửa sổ pop-up chi tiết
     */
    private void showLogDetails(LogEvent logEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết Log Event: " + logEvent.getEventId());
        alert.setHeaderText("Provider: " + logEvent.getProviderName() + "\nTime: " + logEvent.getTimeCreated());

        TextArea textArea = new TextArea(logEvent.getFullDetails());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.getDialogPane().setExpanded(true); // Tự động mở rộng
        alert.getDialogPane().setPrefWidth(600); // Đặt chiều rộng
        alert.getDialogPane().setPrefHeight(400); // Đặt chiều cao

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);
        alert.showAndWait();
    }

    /**
     * Hiển thị cửa sổ pop-up lỗi
     */
    private void showError(String title, String header, Throwable ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText("Lỗi: " + ex.getMessage());

            // Thêm stack trace vào expandable content
            TextArea textArea = new TextArea(ex.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);
            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(textArea, 0, 0);
            alert.getDialogPane().setExpandableContent(expContent);

            alert.showAndWait();
        });
    }
}