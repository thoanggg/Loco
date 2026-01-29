# Loco Agent Documentation

## 1. Giới thiệu
**Loco Agent** là một ứng dụng chạy nền (Service) trên môi trường Windows, được thiết kế để thu thập log hệ thống và cung cấp giao diện API bảo mật để hệ thống trung tâm (LoCo Analyzer) có thể truy xuất dữ liệu.

Agent hoạt động nhẹ nhàng, tự động khởi chạy cùng Windows và có khả năng tự phục hồi nếu gặp sự cố.

## 2. Tính năng chính
*   **Thu thập Windows Event Logs**: Sử dụng công cụ `wevtutil` của Windows để truy xuất log từ các kênh quan trọng như:
    *   Application
    *   System
    *   Security
    *   Microsoft-Windows-Sysmon/Operational
    *   Microsoft-Windows-PowerShell/Operational
*   **REST API qua HTTPS**: Cung cấp API trên port **9876** được mã hóa SSL/TLS để đảm bảo an toàn dữ liệu trên đường truyền.
    *   `GET /ping`: Kiểm tra trạng thái agent, trả về thông tin Hostname và User đang active.
    *   `POST /get-logs`: Nhận yêu cầu lấy log (theo channel) và trả về dữ liệu định dạng XML chuẩn của Windows.
*   **Bảo mật**:
    *   Xác thực kênh log (Input Validation) để ngăn chặn tấn công Command Injection.
    *   Chạy dưới quyền hạn được kiểm soát của Windows Service.
*   **Hiệu năng**:
    *   Giới hạn truy xuất 100 log mới nhất mỗi lần gọi để tránh treo hệ thống.
    *   Log xoay vòng (Rolling file appender) cho service wrapper.

## 3. Kiến trúc kỹ thuật
*   **Ngôn ngữ**: Java 17.
*   **Framework**: Javalin (Lightweight Web Framework).
*   **Server Core**: Jetty (Customized for HTTPS with local Keystore).
*   **Service Wrapper**: Sử dụng **WinSW (Windows Service Wrapper)** để bọc ứng dụng Java thành Native Windows Service.
    *   Hỗ trợ `Auto-Restart` khi service bị crash (Delay 10s, 20s).
    *   Quản lý vòng đời (Lifecycle) Start/Stop/Install/Uninstall.
*   **Installer**: Đóng gói bằng **Inno Setup** tạo file cài đặt `.exe` chuyên nghiệp.

## 4. Hướng dẫn cài đặt & Triển khai

### Yêu cầu hệ thống
*   Hệ điều hành: Windows 10/11 hoặc Windows Server 2016+.
*   Java Runtime Environment (JRE) hoặc JDK 17+ (Nếu không đóng gói kèm JRE, cần cài sẵn).
*   Quyền Administrator để cài đặt Service.

### Các bước cài đặt (End-User)
1.  Tải file cài đặt `LocoAgentInstaller.exe`.
2.  Chạy file cài đặt với quyền Administrator (**Run as Administrator**).
3.  Làm theo hướng dẫn trên màn hình (Setup Wizard).
4.  Sau khi cài đặt xong, Service **"Loco Agent Service"** sẽ tự động khởi chạy.

### Các bước đóng gói (Developer/DevOps)
Nếu bạn muốn build lại bộ cài đặt từ source code:

1.  **Build Source**:
    Tại thư mục gốc của dự án `loco-agent`, chạy lệnh:
    ```powershell
    ./mvnw clean package
    ```
2.  **Chuẩn bị Artifacts**:
    Copy file `target/loco-agent-1.0-SNAPSHOT.jar` vào thư mục `deploy`.
    Đảm bảo thư mục `deploy` đã có `loco-agent.exe` (WinSW) và `loco-agent.xml`.
3.  **Tạo Installer**:
    Sử dụng **Inno Setup Compiler**, mở file `deploy/loco-agent.iss` và nhấn **Build/Compile**.
    File kết quả `LocoAgentInstaller.exe` sẽ được tạo ra.

### Kiểm tra hoạt động
Sau khi cài đặt, bạn có thể kiểm tra agent đang hoạt động bằng cách truy cập trình duyệt hoặc dùng curl (lưu ý bỏ qua check SSL vì dùng self-signed cert):
```bash
curl -k https://localhost:9876/ping
```
Kết quả trả về dạng: `pong|<username>|<hostname>`
