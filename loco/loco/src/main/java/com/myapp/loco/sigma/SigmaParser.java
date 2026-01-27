package com.myapp.loco.sigma;

import org.yaml.snakeyaml.Yaml;

import java.util.Map;

public class SigmaParser {

    private SigmaParser() {
        throw new IllegalStateException("Utility class");
    }

    public static SigmaRule parse(String yamlContent) {
        Yaml yaml = new Yaml();
        try {
            // Load as simple Map to avoid custom Constructor issues initially
            Map<String, Object> data = yaml.load(yamlContent);
            return mapToRule(data);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Sigma YAML: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static SigmaRule mapToRule(Map<String, Object> data) {
        SigmaRule rule = new SigmaRule();
        rule.setTitle((String) data.getOrDefault("title", "Unknown Rule"));
        Object idObj = data.get("id");
        rule.setId(idObj != null ? String.valueOf(idObj) : null);
        rule.setStatus((String) data.get("status"));
        rule.setDescription((String) data.get("description"));
        rule.setAuthor((String) data.get("author"));
        rule.setLevel((String) data.get("level"));

        rule.setLogsource((Map<String, String>) data.get("logsource"));
        rule.setDetection((Map<String, Object>) data.get("detection"));
        rule.setTags((java.util.List<String>) data.get("tags"));

        return rule;
    }
}
