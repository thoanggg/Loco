package com.myapp.loco;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DetectionRule {
    private final StringProperty name;      // Tên luật (VD: Detect Mimikatz)
    private final StringProperty field;     // Trường (VD: Description, EventID)
    private final StringProperty condition; // Điều kiện (Contains, Equals)
    private final StringProperty value;     // Giá trị (VD: mimikatz)
    private final StringProperty severity;  // Mức độ (High, Medium, Low)

    public DetectionRule(String name, String field, String condition, String value, String severity) {
        this.name = new SimpleStringProperty(name);
        this.field = new SimpleStringProperty(field);
        this.condition = new SimpleStringProperty(condition);
        this.value = new SimpleStringProperty(value);
        this.severity = new SimpleStringProperty(severity);
    }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getField() { return field.get(); }
    public StringProperty fieldProperty() { return field; }

    public String getCondition() { return condition.get(); }
    public StringProperty conditionProperty() { return condition; }

    public String getValue() { return value.get(); }
    public StringProperty valueProperty() { return value; }

    public String getSeverity() { return severity.get(); }
    public StringProperty severityProperty() { return severity; }
}