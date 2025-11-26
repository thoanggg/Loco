package com.myapp.locoagent;

import io.javalin.Javalin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LocoAgent {

    public static void main(String[] args) {
        System.out.println("Starting loco-agent on port 9876...");

        Javalin app = Javalin.create().start(9876);

        app.post("/get-logs", ctx -> {
            try {
                LogRequest request = ctx.bodyAsClass(LogRequest.class);
                String xmlOutput = runWevtutil(request);

                ctx.contentType("application/xml");
                ctx.result(xmlOutput);
                System.out.println("Successfully executed query for: " + request.getLogChannel());

            } catch (Exception e) {
                ctx.status(500); // 500 Internal Server Error
                // SỬA LỖI: Sửa lỗi cú pháp (typo)
                ctx.result("Error executing wevtutil: " + e.getMessage());
                System.err.println("Error executing wevtutil: " + e.getMessage());
                e.printStackTrace();
            }
        });

        System.out.println("loco-agent is running. Listening on http://localhost:9876");
    }

    private static String runWevtutil(LogRequest request) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("wevtutil");
        command.add("qe"); // qe = query-events
        command.add(request.getLogChannel());
        command.add("/c:" + request.getEventCount()); // c = count
        command.add("/rd:true"); // rd = reverse direction
        command.add("/f:xml"); // f = format (XML)

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

        return xmlOutput.toString();
    }
}