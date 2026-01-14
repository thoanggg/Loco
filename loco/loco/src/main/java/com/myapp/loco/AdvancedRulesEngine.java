package com.myapp.loco;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdvancedRulesEngine {

    // --- METADATA FOR UI ---
    public static class RuleMetadata {
        private String id; // Added ID for deletion
        private String name;
        private String severity;
        private String mitreId;
        private String description;

        public RuleMetadata(String id, String name, String severity, String mitreId, String description) {
            this.id = id;
            this.name = name;
            this.severity = severity;
            this.mitreId = mitreId;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSeverity() {
            return severity;
        }

        public String getMitreId() {
            return mitreId;
        }

        public String getDescription() {
            return description;
        }
    }

    // --- DYNAMIC SIGMA RULE SUPPORT ---
    private static final java.util.List<com.myapp.loco.sigma.SigmaRule> dynamicRules = new java.util.ArrayList<>();

    // Load persisted rules
    static {
        try {
            java.util.List<java.util.Map<String, String>> savedRules = DatabaseManager.getInstance().getAllRules();
            for (java.util.Map<String, String> ruleData : savedRules) {
                String yaml = ruleData.get("yaml");
                if (yaml != null && !yaml.isEmpty()) {
                    try {
                        com.myapp.loco.sigma.SigmaRule rule = com.myapp.loco.sigma.SigmaParser.parse(yaml);
                        if (rule != null) {
                            dynamicRules.add(rule);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to load rule: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to init rules from DB: " + e.getMessage());
        }
    }

    // Old methods removed, see below for new impl with DB

    public static List<RuleMetadata> getRules() {
        List<RuleMetadata> rules = new ArrayList<>();

        // 1. Add Hardcoded Java Rules
        rules.add(new RuleMetadata("sys-1", "Suspicious Office Child Process", "High", "T1204",
                "Office app spawning CMD/PowerShell"));
        rules.add(new RuleMetadata("sys-2", "Unsigned Executable in Suspect Folder", "Medium", "T1204",
                "Unsigned binary running from %TEMP%"));
        rules.add(new RuleMetadata("sys-3", "LSASS Memory Access Detected", "Critical", "T1003",
                "Process accessing LSASS memory"));
        rules.add(new RuleMetadata("sys-4", "Mimikatz Activity Detected", "Critical", "T1003",
                "CommandLine contains sekurlsa/logonpasswords"));
        rules.add(new RuleMetadata("sys-5", "Sticky Keys Backdoor Attempt", "High", "T1546.008",
                "Registry change to sethc.exe"));
        rules.add(new RuleMetadata("sys-6", "New Service Installed", "Medium", "T1543.003",
                "Event 4697: Service installation"));
        rules.add(new RuleMetadata("sys-7", "New Scheduled Task Created", "Medium", "T1053.005",
                "Event 4698: Task creation"));
        rules.add(new RuleMetadata("sys-8", "Security Log Cleared", "Critical", "T1070", "Event 1102: Log cleared"));
        rules.add(
                new RuleMetadata("sys-9", "RDP Login Detected", "High", "T1021", "Remote Interactive Logon (Type 10)"));
        rules.add(new RuleMetadata("sys-10", "Suspicious Process Network Connection", "High", "T1071",
                "Notepad/Calc connecting to Internet"));

        // 2. Add Dynamic Sigma Rules
        for (com.myapp.loco.sigma.SigmaRule s : dynamicRules) {
            rules.add(new RuleMetadata(
                    s.getId(),
                    s.getTitle(),
                    s.getLevel() != null ? s.getLevel() : "Medium",
                    s.getId(), // Using ID for MITRE col if no MITRE tag, or handle differently logic later
                    s.getDescription() != null ? s.getDescription() : "Sigma Rule"));
        }
        return rules;
    }

    public static void applyRules(LogEvent log) {
        Map<String, String> data = log.getEventData();
        String eventId = log.getEventId();

        // --- HARDCODED RULES --- (Keeping existing logic)

        // Rule 1
        if ("4688".equals(eventId) || "1".equals(eventId)) {
            String parent = data.getOrDefault("ParentImage", "").toLowerCase();
            String child = data.getOrDefault("Image", "").toLowerCase();
            if (child.isEmpty())
                child = data.getOrDefault("NewProcessName", "").toLowerCase();
            if ((parent.endsWith("winword.exe") || parent.endsWith("excel.exe") || parent.endsWith("outlook.exe")) &&
                    (child.endsWith("cmd.exe") || child.endsWith("powershell.exe") || child.endsWith("wscript.exe"))) {
                triggerAlert(log, "Suspicious Office Child Process", "High", "T1204");
                return;
            }
        }

        // Rule 2
        if ("4688".equals(eventId) || "1".equals(eventId) || "7".equals(eventId)) {
            String image = data.getOrDefault("Image", "").toLowerCase();
            if (image.isEmpty())
                image = data.getOrDefault("NewProcessName", "").toLowerCase();
            if (image.isEmpty())
                image = data.getOrDefault("ImageLoaded", "").toLowerCase();
            if (image.contains("\\users\\public\\") || image.contains("\\appdata\\local\\temp\\")
                    || image.contains("\\windows\\temp\\")) {
                String signed = data.getOrDefault("Signed", "true");
                if ("false".equals(signed) || "Revoked".equals(signed)) {
                    triggerAlert(log, "Unsigned Executable in Suspect Folder", "Medium", "T1204");
                    return;
                }
            }
        }

        // Rule 3: LSASS
        if ("10".equals(eventId)) {
            String target = data.getOrDefault("TargetImage", "").toLowerCase();
            String access = data.getOrDefault("GrantedAccess", "");
            if (target.endsWith("lsass.exe") && (access.contains("1F3FFF") || access.contains("1010"))) {
                String source = data.getOrDefault("SourceImage", "").toLowerCase();
                if (!source.endsWith("svchost.exe") && !source.contains("antivirus")) {
                    triggerAlert(log, "LSASS Memory Access Detected", "Critical", "T1003");
                    return;
                }
            }
        }

        // Rule 4: Mimikatz
        if ("4688".equals(eventId) || "1".equals(eventId) || "4104".equals(eventId)) {
            String cmd = data.getOrDefault("CommandLine", "").toLowerCase();
            if (cmd.isEmpty())
                cmd = data.getOrDefault("ScriptBlockText", "").toLowerCase();
            if (cmd.contains("sekurlsa") || cmd.contains("logonpasswords") || cmd.contains("privilege::debug")
                    || cmd.contains("lsadump")) {
                triggerAlert(log, "Mimikatz Activity Detected", "Critical", "T1003");
                return;
            }
        }

        // Rule 5: Sticky Keys
        if ("4657".equals(eventId) || "12".equals(eventId) || "13".equals(eventId)) {
            String targetObj = data.getOrDefault("ObjectName", "").toLowerCase();
            if (targetObj.contains("image file execution options\\sethc.exe")) {
                triggerAlert(log, "Sticky Keys Backdoor Attempt", "High", "T1546.008");
                return;
            }
        }

        // Rule 6: Services
        if ("4697".equals(eventId)) {
            triggerAlert(log, "New Service Installed", "Medium", "T1543.003");
            return;
        }
        if ("4698".equals(eventId)) {
            triggerAlert(log, "New Scheduled Task Created", "Medium", "T1053.005");
            return;
        }

        // Rule 7: Log Clear
        if ("1102".equals(eventId)) {
            triggerAlert(log, "Security Log Cleared", "Critical", "T1070");
            return;
        }
        if ("4688".equals(eventId) || "1".equals(eventId)) {
            String cmd = data.getOrDefault("CommandLine", "").toLowerCase();
            if (cmd.contains("wevtutil") && (cmd.contains(" cl ") || cmd.contains(" clear-log"))) {
                triggerAlert(log, "Log Clearing Attempt Command", "Critical", "T1070");
                return;
            }
        }

        // Rule 8: RDP
        if ("4624".equals(eventId) && "10".equals(data.getOrDefault("LogonType", ""))) {
            triggerAlert(log, "RDP Login Detected", "High", "T1021");
            return;
        }

        // Rule 9: Beaconing
        if ("3".equals(eventId)) {
            String image = data.getOrDefault("Image", "").toLowerCase();
            if (image.endsWith("notepad.exe") || image.endsWith("calc.exe") || image.endsWith("mspaint.exe")) {
                String destPort = data.getOrDefault("DestinationPort", "");
                if ("80".equals(destPort) || "443".equals(destPort) || "8080".equals(destPort)) {
                    triggerAlert(log, "Suspicious Process Network Connection", "High", "T1071");
                    return;
                }
            }
        }

        // --- DYNAMIC RULES MANAGEMENT ---

        // --- DYNAMIC RULES EVALUATION ---
        evaluateSigmaRules(log);
    } // End of applyRules

    // --- DYNAMIC RULES MANAGEMENT ---

    public static void addSigmaRule(com.myapp.loco.sigma.SigmaRule rule) {
        addSigmaRule(rule, null); // Delegate
    }

    public static void addSigmaRule(com.myapp.loco.sigma.SigmaRule rule, String originalYaml) {
        dynamicRules.add(rule);
        if (originalYaml != null) {
            DatabaseManager.getInstance().insertRule(rule, originalYaml);
        }
    }

    public static boolean removeSigmaRule(String id) {
        boolean removed = dynamicRules.removeIf(r -> r.getId().equals(id));
        if (removed) {
            DatabaseManager.getInstance().deleteRule(id);
        }
        return removed;
    }

    private static void evaluateSigmaRules(LogEvent event) {
        for (com.myapp.loco.sigma.SigmaRule rule : dynamicRules) {
            if (matchesSigmaRule(event, rule)) {
                // Overload triggerAlert to accept description if needed, or use existing
                triggerAlert(event, rule.getTitle(), rule.getLevel() != null ? rule.getLevel() : "High", rule.getId());
            }
        }
    }

    private static boolean matchesSigmaRule(LogEvent event, com.myapp.loco.sigma.SigmaRule rule) {
        java.util.Map<String, Object> detection = rule.getDetection();
        if (detection == null)
            return false;

        // MVP: Check for "selection" key only for now
        Object selectionObj = detection.get("selection");
        if (selectionObj instanceof java.util.Map) {
            return matchesSelection(event, (java.util.Map<?, ?>) selectionObj);
        }

        // Check for specific named selections if "condition" isn't parsed yet
        // For MVP we just iterate all map entries in 'detection' that are Maps (except
        // 'condition')
        for (Map.Entry<String, Object> entry : detection.entrySet()) {
            if (!"condition".equals(entry.getKey()) && entry.getValue() instanceof Map) {
                if (matchesSelection(event, (Map<?, ?>) entry.getValue())) {
                    return true; // Simplified OR logic across named selections
                }
            }
        }

        return false;
    }

    private static boolean matchesSelection(LogEvent event, java.util.Map<?, ?> selection) {
        for (java.util.Map.Entry<?, ?> entry : selection.entrySet()) {
            String field = String.valueOf(entry.getKey());
            String value = String.valueOf(entry.getValue());

            // Resolve field value from event
            String eventValue = event.getEventData().get(field);

            // Fallbacks for common fields if not directly in map (e.g. from XML attributes)
            if (eventValue == null) {
                if ("EventID".equalsIgnoreCase(field))
                    eventValue = event.getEventId();
                else if ("Provider".equalsIgnoreCase(field))
                    eventValue = event.getProviderName();
            }

            if (eventValue == null || !eventValue.toLowerCase().contains(value.toLowerCase())) {
                return false; // AND logic: all fields in selection must match
            }
        }
        return true;
    }

    private static void triggerAlert(LogEvent log, String name, String severity, String mitre) {
        log.setDetectionName(name);
        log.setAlert(true);
        log.setAlertSeverity(severity);
        log.setMitreId(mitre);
        log.setStatus("New Alert");
        System.out.println("ALERT TRIGGERED: " + name + " [" + severity + "] MITRE: " + mitre);
    }
}
