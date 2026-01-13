# 🛡️ LOCO - Secured Log Collector

Loco is a cross-platform security log collection and analysis system designed for threat hunting and incident response. It consists of a headless **Admin Dashboard** (running on Linux/Ubuntu) and lightweight **Agents** (running on Windows).

## 🚀 Key Features
*   **Cross-Platform Architecture**: Admin runs on Linux, Agents run on Windows.
*   **Real-time Analysis**: Dashboard with charts for Alert Severity Distribution and Event Volume.
*   **Advanced Detection Rules**:
    *   Built-in Rule Engine supporting "Contains", "Equals", "Starts With".
    *   Detects Brute Force, Mimikatz, PowerShell Persistence, and more.
*   **Secure Communication (HTTPS)**: All traffic between Admin and Agents is encrypted.
*   **Wazuh-Inspired UI**: Dark mode, cybersecurity-themed interface for optimal SOC visibility.

## 🔒 Security Architecture (HTTPS Implementation)
Loco uses a robust **Self-Signed Certificate** model to ensure encrypted communication without the complexity of a public PKI infrastructure.

### 1. Agent-Side Encryption
The Windows Agent (`loco-agent`) runs a **Javalin** web server powered by **Jetty**.
*   **Keystore**: A self-signed RSA-3072 certificate is generated and stored in a JKS keystore (`keystore.jks`). This keystore is embedded directly inside the Agent's shaded JAR file.
*   **Configuration**: When the Agent starts, it extracts or reads the keystore from the classpath and configures the Jetty SSL Context to serve traffic on port `9876` using TLS.

```java
// Snippet: Agent SSL Config
sslContextFactory.setKeyStorePath(LocoAgent.class.getResource("/keystore.jks").toExternalForm());
sslContextFactory.setKeyStorePassword("password123");
```

### 2. Admin-Side Trust
The Admin UI (`loco`) acts as the HTTPS Client.
*   **Custom Trust Manager**: Since the Agent uses a self-signed certificate, the Admin's `HttpClient` is configured with a custom `TrustManager` that explicitly trusts the internal certificate (or trusts all certs in this private environment).
*   **Hostname Verification**: Hostname verification is disabled to allow the Admin to connect to Agents by IP address or varying hostnames without certificate errors.

```java
// Snippet: Admin Secure Client
SSLContext sc = SSLContext.getInstance("SSL");
sc.init(null, trustAllCerts, new SecureRandom());
// Connects via https://<AGENT_IP>:9876/
```

This ensures that network capturing tools (like Wireshark) cannot inspect the log data or commands in transit.

## 🛠️ Build & Run

### Prerequisites
*   Java JDK 17+
*   Maven

### 1. Build Both Modules
```bash
mvn clean package -DskipTests
```

### 2. Run Admin (Linux)
```bash
cd loco
./mvnw javafx:run
```

### 3. Run Agent (Windows)
Copy the shaded JAR to the target Windows machine:
```cmd
copy loco-agent/target/loco-agent-1.0-SNAPSHOT.jar C:\Tools\
java -jar C:\Tools\loco-agent-1.0-SNAPSHOT.jar

### 4. Run as Windows Service (Optional but Recommended)
To run the agent in the background and auto-start with Windows:
1.  Download **WinSW (Windows Service Wrapper)** from [WinSW Releases](https://github.com/winsw/winsw/releases) (e.g., `WinSW-x64.exe`).
2.  Rename the downloaded file to **`loco-agent.exe`**.
3.  Place `loco-agent.exe`, `loco-agent.xml`, and `install_service.bat` in the same folder as your JAR (`C:\Tools\`).
4.  Right-click `install_service.bat` and **Run as Administrator**.
    *   *To uninstall: Run `uninstall_service.bat` as Administrator.*