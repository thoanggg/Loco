# 🛡️ Analysis Dashboard Documentation

This document describes the **Analysis & Detection** module implemented in the LOCO Security Log Collector.

## Overview
The Analysis Dashboard provides a real-time visual overview of the security posture of your monitored infrastructure. It aggregates logs from multiple agents, applies detection rules, and presents actionable intelligence to the administrator.

## Key Features

### 1. Visualization Charts
*   **Alert Severity Distribution (Pie Chart)**:
    *   Visualizes the ratio of **High**, **Medium**, and **Low** severity alerts.
    *   Helps prioritize incident response efforts.
*   **Events per Agent (Bar Chart)**:
    *   Displays the volume of logs collected from each agent.
    *   Useful for identifying noisy agents or potential flooding attacks.

### 2. Recent Detections Table
A dynamic table listing the most recent security alerts triggered by the Rule Engine.

| Column | Description |
| :--- | :--- |
| **Severity** | Color-coded indicator (**Red/High**, **Orange/Medium**, **Yellow/Low**) for quick assessment. |
| **Detection** | The name of the specific detection rule that was triggered (e.g., "Detect Mimikatz", "Brute Force"). |
| **Date** | Timestamp of the event in a human-readable format (e.g., `Nov 27th 2025 at 21:11`). |
| **Host** | The name of the agent/computer where the event originated. |
| **Status** | Interactive toggle button. Indicates whether the alert has been **Acknowledged** by an admin. |

### 3. Interactive Triage
*   **Status Toggle**: Admins can click the **Status** button to toggle an alert between "Not Acknowledged" (Grey) and "Acknowledged" (Green). This helps track which incidents have been reviewed.
*   **Deep Dive**: Double-clicking (or single-clicking depending on config) a row opens a detailed view of the raw log data, including Event ID, User, Process, and full XML payload.

## Configuration
Detection rules are managed in the **Rules Engine** tab. The dashboard automatically subscribes to these rules. When a new log matches a rule:
1.  It is flagged as an `Alert`.
2.  Assigned a `Severity` and `Detection Name`.
3.  Immediately appears in the **Recent Detections** table and updates chart counters.

## Technical Notes
*   **Time Format**: Custom logic formats ISO timestamps into `Month Day[st/nd/rd/th] Year at HH:mm`.
*   **Style**: The dashboard uses a custom CSS theme (`style.css`) inspired by Wazuh/Elastic Security suitable for SOC environments (Dark Mode).
