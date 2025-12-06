package com.myapp.locoagent;

import io.javalin.Javalin;
import com.myapp.locoagent.LogRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class LocoAgent {

    public static void main(String[] args) {
        System.out.println("Starting loco-agent on port 9876...");

        Javalin app = Javalin.create().start(9876);

        app.get("/ping", ctx -> {
            // 1. Lấy Hostname
            String hostName = "Unknown";
            try { hostName = InetAddress.getLocalHost().getHostName(); } catch (Exception e) {}

            // 2. Lấy User thật đang login (thay vì User chạy service)
            String realUser = getActiveUser();
            if (realUser.isEmpty()) {
                realUser = System.getProperty("user.name"); // Fallback
            }

            // Trả về: pong | DOMAIN\User | Hostname
            ctx.result("pong|" + realUser + "|" + hostName);
        });

        app.post("/get-logs", ctx -> {
            try {
                LogRequest request = ctx.bodyAsClass(LogRequest.class);
                String xmlOutput = runWevtutil(request);
                ctx.contentType("application/xml");
                ctx.result(xmlOutput);
                System.out.println("Query executed for: " + request.getLogChannel());
            } catch (Exception e) {
                ctx.status(500);
                ctx.result("Error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // Hàm lấy User đang đăng nhập bằng lệnh WMIC
    private static String getActiveUser() {
        try {
            Process p = Runtime.getRuntime().exec("wmic computersystem get username");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            String user = "";
            while ((line = input.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.equalsIgnoreCase("UserName")) {
                    user = line;
                    break;
                }
            }
            input.close();
            return user; // Kết quả thường là: DESKTOP-XXX\vuduc
        } catch (Exception e) {
            return "";
        }
    }

    private static String runWevtutil(LogRequest request) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("wevtutil"); command.add("qe"); command.add(request.getLogChannel());
        command.add("/c:" + request.getEventCount()); command.add("/rd:true"); command.add("/f:xml");
        if (request.getXpathQuery() != null && !request.getXpathQuery().trim().isEmpty()) {
            command.add("/q:" + request.getXpathQuery());
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder xmlOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) xmlOutput.append(line).append(System.lineSeparator());
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new RuntimeException("Exit code " + exitCode + ": " + xmlOutput);
        return xmlOutput.toString();
    }
}