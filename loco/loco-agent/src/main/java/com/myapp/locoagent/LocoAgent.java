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
        System.out.println("Starting loco-agent on port 9876 (HTTPS)...");

        Javalin app = Javalin.create(config -> {
            config.jetty.modifyServer(server -> {
                org.eclipse.jetty.util.ssl.SslContextFactory.Server sslContextFactory = new org.eclipse.jetty.util.ssl.SslContextFactory.Server();

                try {
                    // Load keystore from classpath (works inside JAR)
                    java.security.KeyStore ks = java.security.KeyStore.getInstance("JKS");
                    try (java.io.InputStream is = LocoAgent.class.getResourceAsStream("/keystore.jks")) {
                        if (is == null) {
                            throw new RuntimeException("Keystore file '/keystore.jks' not found in classpath.");
                        }
                        ks.load(is, "password123".toCharArray());
                    }
                    sslContextFactory.setKeyStore(ks);
                } catch (Exception e) {
                    System.err.println("CRITICAL: Failed to load keystore. " + e.getMessage());
                    e.printStackTrace();
                    // Fallback/Crash handled by caller usually, but we should exit or try file
                    sslContextFactory.setKeyStorePath("keystore.jks");
                }

                sslContextFactory.setKeyStorePassword("password123");

                org.eclipse.jetty.server.HttpConfiguration httpsConfig = new org.eclipse.jetty.server.HttpConfiguration();
                org.eclipse.jetty.server.SecureRequestCustomizer src = new org.eclipse.jetty.server.SecureRequestCustomizer();
                src.setSniHostCheck(false); // Fix for "Invalid SNI" error
                httpsConfig.addCustomizer(src);

                org.eclipse.jetty.server.HttpConnectionFactory httpConnectionFactory = new org.eclipse.jetty.server.HttpConnectionFactory(
                        httpsConfig);
                org.eclipse.jetty.server.SslConnectionFactory sslConnectionFactory = new org.eclipse.jetty.server.SslConnectionFactory(
                        sslContextFactory, org.eclipse.jetty.http.HttpVersion.HTTP_1_1.asString());

                org.eclipse.jetty.server.ServerConnector sslConnector = new org.eclipse.jetty.server.ServerConnector(
                        server, sslConnectionFactory, httpConnectionFactory);
                sslConnector.setPort(9876);
                server.setConnectors(new org.eclipse.jetty.server.Connector[] { sslConnector });
            });
        }).start(); // Port argument is ignored if server is manually configured, but kept for
                    // clarity

        app.get("/ping", ctx -> {
            String hostName = "Unknown";
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
            }
            String realUser = getActiveUser();
            if (realUser.isEmpty())
                realUser = System.getProperty("user.name");
            ctx.result("pong|" + realUser + "|" + hostName);
        });

        app.post("/get-logs", ctx -> {
            try {
                LogRequest request = ctx.bodyAsClass(LogRequest.class);
                System.out.println("Fetching ALL logs for channel: " + request.getLogChannel());
                String xmlOutput = runWevtutil(request);
                ctx.contentType("application/xml");
                ctx.result(xmlOutput);
            } catch (Exception e) {
                ctx.status(500);
                ctx.result("Error: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // KEEP ALIVE: Prevents the main thread from exiting, which might cause the
        // service wrapper to think the app stopped.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String getActiveUser() {
        try {
            // ... (rest same)
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
            return user;
        } catch (Exception e) {
            return "";
        }
    }

    private static String runWevtutil(LogRequest request) throws Exception {
        // SECURITY FIX: Validate channel name to prevent command injection
        String channel = request.getLogChannel();
        if (!isValidChannel(channel)) {
            throw new IllegalArgumentException("Invalid log channel: " + channel);
        }

        List<String> command = new ArrayList<>();
        command.add("wevtutil");
        command.add("qe");
        command.add(channel);

        // TODO: Implement incremental fetching by using /q: with TimeCreated
        // For now, allow limiting count to prevent hang
        command.add("/c:100"); // Limit to 100 recent events for performance
        command.add("/rd:true"); // Newest first
        command.add("/f:xml");

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
        if (exitCode != 0)
            throw new RuntimeException("Exit code " + exitCode + ": " + xmlOutput);

        return xmlOutput.toString();
    }

    private static boolean isValidChannel(String channel) {
        // Whitelist allowed channels
        List<String> allowed = List.of(
                "Application", "System", "Security", "Setup",
                "Microsoft-Windows-Sysmon/Operational",
                "Microsoft-Windows-PowerShell/Operational");
        return allowed.contains(channel) || channel.matches("^[a-zA-Z0-9\\-]+(/[a-zA-Z0-9\\-]+)?$");
    }
}