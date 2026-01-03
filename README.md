Loco Security - Hệ thống Giám sát & Phân tích Log Tập trung

Loco Security là một giải pháp giám sát an ninh mạng (Mini-SIEM/EDR) dành cho môi trường Windows. Hệ thống cho phép quản trị viên thu thập log từ nhiều máy trạm, tự động phát hiện các hành vi đáng ngờ và quản lý tập trung thông qua một giao diện Dashboard hiện đại.

1. Giới thiệu chung

Trong môi trường mạng doanh nghiệp, việc theo dõi nhật ký hoạt động (Logs) của từng máy tính là cực kỳ khó khăn. Loco Security ra đời để giải quyết vấn đề này bằng mô hình Client-Server:

Loco Agent: Cài đặt trên máy trạm, chạy ngầm như một dịch vụ, chịu trách nhiệm thu thập dữ liệu thô.

Loco Host (App): Trung tâm quản lý, tự động quét mạng tìm Agent, thu thập log, phân tích và cảnh báo.

2. Quá trình Phát triển (Development Journey)

Dự án được phát triển qua nhiều giai đoạn, nâng cấp dần từ một công cụ đơn giản thành một hệ thống phân tán.

Giai đoạn 1: Proof of Concept (PoC) - Local Viewer

Mục tiêu: Xây dựng ứng dụng JavaFX đọc được log của chính máy đang chạy.

Giải pháp: Sử dụng ProcessBuilder để gọi lệnh wevtutil.exe của Windows.

Kết quả: Ứng dụng đọc được log thô dạng text, hiển thị lên TextArea.

Giai đoạn 2: Cấu trúc hóa dữ liệu & UI

Mục tiêu: Làm cho log dễ đọc và dễ tìm kiếm.

Cải tiến:

Chuyển định dạng log sang XML (/f:xml).

Sử dụng DOM Parser để tách các trường: EventID, Time, Provider, Data.

Hiển thị dữ liệu lên TableView.

Xây dựng bộ phân tích riêng cho Sysmon để hiển thị chi tiết (Image, CommandLine, IP...).

Giai đoạn 3: Kiến trúc Client-Server (Remote Collection)

Mục tiêu: Thu thập log từ máy khác trong mạng LAN.

Giải pháp:

Tách module thu thập thành Loco Agent (sử dụng thư viện Javalin Web Server).

Giao tiếp giữa App và Agent qua giao thức HTTP (REST API).

Agent nhận lệnh JSON -> Chạy wevtutil -> Trả về XML.

Giai đoạn 4: Tự động hóa & Khám phá (Discovery)

Mục tiêu: Admin không cần nhập IP thủ công.

Tính năng:

Xây dựng NetworkScanner: Quét đa luồng (Multi-threading) toàn bộ dải mạng để tìm Agent mở cổng 9876.

Cơ chế Health Check: Định kỳ Ping các Agent để cập nhật trạng thái (Online/Offline) và thông tin máy (User/Hostname).

Giai đoạn 5: Thông minh hóa (Rules Engine)

Mục tiêu: Tự động phát hiện tấn công thay vì chỉ đọc log.

Tính năng:

Xây dựng hệ thống luật (Detection Rules).

Cho phép Admin thêm/sửa/xóa luật trên giao diện.

Tự động so khớp log với luật (VD: Phát hiện mimikatz, whoami, xóa log).

Cảnh báo màu sắc theo mức độ nguy hiểm (High/Medium/Low).

Giai đoạn 6: Hoàn thiện & Đóng gói (Release)

UI/UX: Chuyển sang giao diện Dark Mode, bố cục Dashboard hiện đại.

Deployment: Đóng gói Agent thành Windows Service chạy ngầm bằng công cụ WinSW, đảm bảo tính ổn định và tự khởi động cùng Windows.

3. Kiến trúc Hệ thống

Sơ đồ luồng dữ liệu

[Admin Dashboard] <--(HTTP Request/Response)--> [Loco Agent 1] <--> [Windows Event Log]
       |                                            |
       |                                      [Loco Agent 2] <--> [Sysmon]
       |
    [Rules Engine] -> [Alert System]


Công nghệ sử dụng

Ngôn ngữ: Java 17.

Giao diện (Frontend): JavaFX, CSS (Dark Theme).

Backend (Agent): Javalin (Lightweight Web Framework).

Dữ liệu: XML (Windows Event Format), JSON (Cấu hình & Giao tiếp).

Hệ thống: Windows Command Line (wevtutil, wmic), WinSW (Service Wrapper).

Quản lý dự án: Maven.

4. Các tính năng chính

🛡️ Dashboard (Bảng điều khiển)

Auto-Discovery: Tự động quét và phát hiện các máy trạm có cài Agent.

Live Monitoring: Hiển thị trạng thái Online/Offline, Tên máy, Người dùng đang đăng nhập, Thời gian cập nhật cuối.

Thống kê: Tổng số Agent, Tổng số cảnh báo phát hiện được.

🔍 Log Explorer (Trình phân tích Log)

Đa nguồn: Xem log của máy Local hoặc bất kỳ Agent nào trong mạng.

Hỗ trợ đa kênh: Sysmon, Security, Application, System.

Smart Parsing: Tự động phân tích log Sysmon phức tạp thành thông tin dễ đọc.

Bộ lọc mạnh mẽ: Lọc theo User, Event ID, Ngày tháng.

⚡ Rules Engine (Hệ thống Luật)

Tùy biến: Admin có thể tự định nghĩa luật phát hiện tấn công.

Cảnh báo: Tự động tô màu các dòng log vi phạm luật (Đỏ, Cam, Vàng).

Luật mẫu tích hợp: Phát hiện Mimikatz, Reconnaissance (Whoami, IPConfig), Defense Evasion (Xóa log), v.v.

5. Hướng dẫn Cài đặt & Sử dụng

Yêu cầu

Máy tính chạy Windows 10/11 hoặc Windows Server.

Đã cài đặt Java 17 trở lên.

Khuyến nghị cài đặt Sysmon trên các máy trạm để tối ưu khả năng giám sát.

Bước 1: Cài đặt Agent (Trên máy bị giám sát)

Copy thư mục LocoAgent vào máy.

Chạy file install_agent.bat với quyền Administrator.

Agent sẽ tự động chạy ngầm và mở cổng Firewall 9876.

Bước 2: Chạy App Quản lý (Trên máy Admin)

Chạy file run_app.bat hoặc Loco.jar.

Tại Dashboard, nhấn "Scan Network" để tìm các máy trạm.

Nhấn "Investigate" để xem log và phát hiện tấn công.

6. Hướng phát triển (Future Roadmap)

[ ] Bảo mật: Mã hóa giao tiếp bằng HTTPS/TLS và thêm xác thực API Key.

[ ] Database: Lưu trữ log vào cơ sở dữ liệu (SQLite/Elasticsearch) để tra cứu lịch sử lâu dài.

[ ] Remote Response: Cho phép Admin gửi lệnh chặn tiến trình hoặc ngắt mạng máy trạm từ xa khi phát hiện tấn công.

[ ] Cross-Platform: Phát triển Agent cho Linux (sử dụng Auditd).

Tác giả: [Tên Của Bạn]
Dự án: Loco Security - Java Network Programming Project