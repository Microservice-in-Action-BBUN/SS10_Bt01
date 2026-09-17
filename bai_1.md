# BÀI 1: Phân Tích & Lựa Chọn Giải Pháp Triển Khai Hạ Tầng LLMOps Langfuse

---

## 1. Giới Thiệu Hệ Sinh Thái & Bối Cảnh Nghiệp Vụ

Trong hệ sinh thái **Rikkei Intelligent Banking & Assistant Suite (RikkeiPay)**, phân hệ trợ lý ảo ngân hàng thông minh **RikkeiPay Assistant** đóng vai trò xử lý trực tiếp các yêu cầu tài chính, truy vấn số dư, hướng dẫn giao dịch và hỗ trợ khách hàng tự động thông qua mô hình ngôn ngữ lớn (LLM).

Khi hệ thống vận hành thực tế phục vụ hàng ngàn đến hàng triệu lượt gọi mỗi ngày, việc thiết lập hạ tầng **LLMOps (Large Language Model Operations)** tập trung bằng **Langfuse** là điều kiện tiên quyết nhằm:
- **Truy vết toàn diện (Full-stack LLM Tracing):** Theo dõi chuỗi thực thi (Trace Tree), từng bước Tool Calling, Input/Output payload, và đo lường độ trễ (Latency).
- **Kiểm soát chi phí & Token (Cost & Token Management):** Đo lường chính xác lượng token tiêu thụ theo từng model/user/session để tối ưu hóa ngân sách.
- **Quản lý Prompt tập trung (Centralized Prompt Management):** Quản lý phiên bản Prompt template độc lập với mã nguồn backend, hỗ trợ A/B Testing và cập nhật realtime.
- **Đánh giá chất lượng & Phản hồi (Evaluations & Scores):** Thu thập điểm đánh giá của khách hàng và chạy các bộ thẩm định tự động (LLM-as-a-judge).

Tuy nhiên, đối với một hệ thống tài chính - ngân hàng, bài toán lựa chọn kiến trúc triển khai Langfuse Self-Host phải thỏa mãn 3 yêu cầu cốt lõi:
1. **Tuyệt đối an toàn thông tin & Tuân thủ bảo mật:** Bảo vệ dữ liệu nhạy cảm (PII, số tài khoản, thông tin giao dịch) theo chuẩn PCI-DSS, ISO 27001 và quy định của Ngân hàng Nhà nước.
2. **Kiểm soát tài nguyên & Chi phí vận hành:** Tối ưu hóa năng lực tính toán và lưu trữ, tránh lãng phí hạ tầng nhưng vẫn đảm bảo khả năng mở rộng.
3. **Tính sẵn sàng cao & Giảm thiểu tối đa thời gian gián đoạn (High Availability & Zero Downtime):** Đảm bảo cơ chế sao lưu liên tục, phục hồi thảm họa tức thì (RPO $\approx 0$, RTO $< 5$ phút).

---

## 2. Bảng So Sánh Chi Tiết 3 Phương Án Triển Khai Langfuse

| Tiêu Chí Đánh Giá | Phương án A: Langfuse Self-Host Tối Giản (PostgreSQL Docker Container) | Phương án B: Langfuse Self-Host Đầy Đủ (PostgreSQL + ClickHouse Docker Compose) | Phương án C: Langfuse Self-Host Doanh Nghiệp (Docker Compose + PostgreSQL External/RDS) |
| :--- | :--- | :--- | :--- |
| **Kiến Trúc Tổng Thể** | 1 Container Web/API + 1 Container PostgreSQL cục bộ (Local Docker Network). | 1 Container Web + 1 Container PostgreSQL (Metadata) + 1 Container ClickHouse (Analytics Traces) trên cùng 1 Docker Host. | Các Stateless Container Web/API (Docker/K8s) kết nối tới Cụm PostgreSQL External chuyên dụng (AWS RDS / On-Premise HA Cluster). |
| **Bảo Mật Dữ Liệu (Data Privacy & Compliance)** | 🔴 **Thấp**<br>- Dữ liệu lưu trong Local Docker Volume.<br>- Không có phân tầng mạng (Network Segmentation).<br>- Không có mã hóa ổ đĩa cấp doanh nghiệp (KMS/TDE).<br>- Khó đáp ứng chuẩn PCI-DSS/ISO 27001 của ngân hàng. | 🟡 **Trung Bình - Thấp**<br>- Cả hai CSDL đều nằm trong Docker host nội bộ.<br>- ClickHouse mở thêm cổng mạng nội bộ, tăng diện tích tấn công (Attack Surface).<br>- Không có IAM Role hoặc Audit Log chuyên dụng của hệ thống DB Enterprise. | 🟢 **Xuất Sắc (Bank-grade)**<br>- Database đặt trong **Private Subnet** cô lập, kiểm soát bằng Security Groups/NACLs.<br>- Bắt buộc mã hóa at-rest (AWS KMS AES-256) và in-transit (`sslmode=require`).<br>- Kiểm soát quyền truy cập chặt chẽ qua IAM/LDAP, hỗ trợ Database Activity Streams / Audit Trail. |
| **Chi Phí & Tài Nguyên (CPU / RAM / Storage)** | 🟢 **Rất Thấp (Tiết kiệm nhất)**<br>- Yêu cầu: ~1 - 2 GB RAM, 1-2 vCPU.<br>- Phù hợp máy tính cá nhân của lập trình viên (Dev Local). | 🔴 **Rất Cao & Dễ Tranh Chấp**<br>- Yêu cầu: Tối thiểu 6 - 8 GB RAM, 4 vCPU.<br>- ClickHouse tiêu thụ RAM rất lớn khi ingest batch và aggregate queries.<br>- Nguy cơ tranh chấp tài nguyên giữa Web và DB gây sập hệ thống (OOM). | 🟢 **Tối Ưu & Tách Biệt Độc Lập**<br>- App Container chỉ cần 1 - 2 GB RAM (Stateless).<br>- DB scale độc lập theo dung lượng giao dịch thực tế (Auto-scaling Storage).<br>- Tận dụng hạ tầng RDS/DB Cluster có sẵn của ngân hàng, tối ưu TCO (Total Cost of Ownership). |
| **Độ Phức Tạp Triển Khai & Vận Hành (DevOps Complexity)** | 🟢 **Rất Đơn Giản**<br>- Chỉ cần 1 file `docker-compose.yml` cơ bản.<br>- Khởi chạy nhanh chóng bằng 1 câu lệnh `docker-compose up -d`. | 🔴 **Cực Kỳ Phức Tạp**<br>- Cần cấu hình và đồng bộ đồng thời 2 CSDL khác nhau (Postgres + ClickHouse).<br>- Phải tự quản lý phân vùng dữ liệu (Partition Pruning), Retention Policy và cấu hình bộ nhớ của ClickHouse.<br>- Nâng cấp phiên bản dễ gặp lỗi Schema Migration. | 🟢 **Đơn Giản & Tối Giản Gánh Nặng**<br>- Cấu hình Docker cực kỳ gọn nhẹ qua biến môi trường `DATABASE_URL`.<br>- Toàn bộ việc vá lỗi OS, nâng cấp bản vá DB, tối ưu Index do Cloud Provider hoặc đội ngũ DBA chuyên nghiệp phụ trách. |
| **Khả Năng Sao Lưu & Phục Hồi (Backup & Disaster Recovery)** | 🔴 **Kém & Rủi Ro Cao**<br>- Phụ thuộc vào sao lưu thủ công (`pg_dump`) hoặc copy volume.<br>- Không có tính năng Point-In-Time Recovery (PITR).<br>- Lệnh `docker-compose down -v` hoặc lỗi ổ đĩa sẽ gây **mất dữ liệu vĩnh viễn**. | 🔴 **Rất Kém & Rủi Ro Đồng Bộ**<br>- Cực kỳ khó khăn khi tạo bản sao lưu nhất quán (Consistent Snapshot) giữa 2 CSDL khác nhau cùng lúc.<br>- Khi gặp sự cố thảm họa, việc phục hồi dữ liệu ClickHouse từ docker volume rất chậm và dễ lỗi. | 🟢 **Tối Đa (Enterprise Standard)**<br>- Tự động sao lưu liên tục (Automated Backups).<br>- **Point-In-Time Recovery (PITR)** khôi phục chính xác tới từng giây trước sự cố.<br>- Hỗ trợ Multi-AZ Failover tự động trong $< 60$ giây, **RPO $\approx 0$, RTO $< 5$ phút**. |
| **Khả Năng Mở Rộng (Scalability)** | 🔴 **Hạn Chế (Vertical Only)**<br>- PostgreSQL trong Docker bị nghẽn I/O khi số lượng trace tăng lên hàng triệu bản ghi.<br>- Không thể scale ngang (Horizontal Scale) cho App. | 🟡 **Khá (Scale Data)**<br>- ClickHouse xử lý truy vấn phân tích tốt hơn, nhưng cả cụm bị giới hạn bởi phần cứng của một Single Docker Host duy nhất. | 🟢 **Vượt Trội (Stateless Horizontal Scale)**<br>- Ứng dụng Langfuse Web/API có thể scale thành nhiều Replicas (trên ECS / EKS / K8s) phía sau Load Balancer.<br>- Database hỗ trợ Read Replicas để phân tải truy vấn báo cáo. |
| **Môi Trường Khuyến Nghị** | Môi trường **Local Development**, thử nghiệm tính năng nhanh trên máy cá nhân của Developer. | Môi trường **Staging / Sandbox** có khối lượng log lớn để benchmark hiệu năng phân tích. | **Production Môi Trường Ngân Hàng (RikkeiPay)** bắt buộc áp dụng. |

---

## 3. Quyết Định Lựa Chọn Giải Pháp Tối Ưu Cho RikkeiPay

### 🏆 ĐÁP ÁN LỰA CHỌN: **PHƯƠNG ÁN C**
> **Triển khai Langfuse Self-Host sử dụng Docker Compose kết nối tới một PostgreSQL External Database (AWS RDS PostgreSQL hoặc On-Premises Managed Postgres Cluster của Doanh nghiệp).**

```
+====================================================================================================+
|                     KIẾN TRÚC TRIỂN KHAI LLMOPS LANGFUSE DOANH NGHIỆP CHO RIKKEIPAY                |
+====================================================================================================+
|                                                                                                    |
|   [ CLIENT / USERS ]             [ RIKKEIPAY BACKEND ]                                             |
|          │                                │ (Spring Boot 3 + Spring AI)                            |
|          │                                │                                                        |
|          ▼                                ▼  (OTLP / REST Ingestion)                               |
|   ┌───────────────┐               ┌────────────────────────────────────────┐                       |
|   │ Web Dashboard │               │   Langfuse Observability Client        │                       |
|   └───────┬───────┘               └───────────────────┬────────────────────┘                       |
|           │                                           │                                            |
|           └───────────────────┬───────────────────────┘                                            |
|                               ▼ (Port 3000 / HTTPS)                                                |
|   ============================│=================================================================  |
|   PUBLIC / DMZ SUBNET         │                                                                    |
|                               ▼                                                                    |
|                  ┌─────────────────────────┐                                                       |
|                  │  Application Load       │                                                       |
|                  │  Balancer (ALB / NGINX) │                                                       |
|                  └────────────┬────────────┘                                                       |
|                               │ (Reverse Proxy & SSL Termination)                                  |
|   ============================│=================================================================  |
|   APP TIER (Private Subnet)   ▼                                                                    |
|                  ┌─────────────────────────┐                                                       |
|                  │   Langfuse Web / API    │ ◄── Stateless Docker Container                        |
|                  │   (Docker Compose / K8s)│     (Hỗ trợ Auto-scale ngang)                         |
|                  └────────────┬────────────┘                                                       |
|                               │                                                                    |
|                               │  DATABASE_URL: postgresql://admin:***@rds-db:5432/langfuse         |
|                               │  (Encrypted TLS: sslmode=require)                                  |
|   ============================│=================================================================  |
|   DATA TIER (Isolated Subnet) ▼                                                                    |
|        ┌─────────────────────────────────────────────────────────────┐                             |
|        │            PostgreSQL External / AWS RDS Cluster            │                             |
|        │                                                             │                             |
|        │   ┌─────────────────────┐         ┌─────────────────────┐   │                             |
|        │   │   Primary Instance  │ ──────► │ Standby (Multi-AZ)  │   │                             |
|        │   │   (Read/Write)      │ Sync    │ (Auto Failover)     │   │                             |
|        │   └──────────┬──────────┘ Repl    └─────────────────────┘   │                             |
|        │              │                                              │                             |
|        │              ▼                                              │                             |
|        │   ┌─────────────────────────────────────────────────────┐   │                             |
|        │   │  - Storage Auto-Scaling & EBS Encryption (KMS)      │   │                             |
|        │   │  - Continuous Backup & Point-In-Time Recovery (PITR)│   │                             |
|        │   │  - Automated Patching & Maintenance Windows         │   │                             |
|        │   └─────────────────────────────────────────────────────┘   │                             |
|        └─────────────────────────────────────────────────────────────┘                             |
+====================================================================================================+
```

---

### 3.1. Phân Tích Lập Luận Chi Tiết (Tại Sao Chọn Phương Án C?)

#### 1. Tuân thủ Tiêu chuẩn Bảo mật Ngân hàng Khắt khe (Bank-Grade Security & Zero Trust Architecture):
- **Phân tách tầng mạng (Network Segmentation):** Trong kiến trúc tài chính, tầng dữ liệu (Data Tier) không bao giờ được phép đặt chung môi trường chia sẻ với tầng ứng dụng (App Tier). Phương án C cho phép cô lập hoàn toàn PostgreSQL trong **Isolated Database Subnet**, ngăn chặn tuyệt đối truy cập từ Internet công cộng.
- **Mã hóa toàn diện (End-to-End Encryption):** Dữ liệu được mã hóa khi lưu trữ (**Encryption At-Rest** sử dụng thuật toán AES-256 qua AWS KMS) và mã hóa trên đường truyền (**Encryption In-Transit** thông qua giao thức TLS với cờ `sslmode=require`).
- **Kiểm soát truy cập & Nhật ký kiểm toán (Access Control & Auditing):** Tận dụng hệ thống phân quyền IAM, Role-Based Access Control (RBAC) và Database Audit Trail để giám sát mọi truy vấn đọc/ghi tới dữ liệu Prompt và Trace của người dùng.

#### 2. Đảm bảo Tính liên tục Nghiệp vụ & Khắc phục Thảm họa (Business Continuity, High Availability & PITR):
- **Multi-AZ Replication & Auto-Failover:** Trong trường hợp Data Center chính gặp sự cố vật lý, hệ thống Managed Database tự động chuyển hướng kết nối sang Standby Instance trong vòng dưới 60 giây mà không cần can thiệp thủ công, đảm bảo hệ thống RikkeiPay Assistant không bị gián đoạn.
- **Point-In-Time Recovery (PITR):** Cơ sở dữ liệu tài chính yêu cầu khả năng phục hồi chính xác đến từng mili-giây trước khi xảy ra lỗi (ví dụ: thao tác xóa nhầm dữ liệu của quản trị viên). RDS/Managed Postgres lưu trữ transaction log (WAL) liên tục, cho phép khôi phục trạng thái database tại bất kỳ thời điểm nào trong vòng 35 ngày.

#### 3. Tách biệt Trạng thái (Stateless vs Stateful) Giúp Hệ thống Dễ dàng Mở rộng:
- Bằng cách đẩy toàn bộ trạng thái dữ liệu (Stateful) sang PostgreSQL External, các container Langfuse Web/API trở thành các dịch vụ **hoàn toàn không lưu trạng thái (Stateless)**.
- Khi lưu lượng giao dịch ngân hàng tăng đột biến trong các khung giờ cao điểm (ví dụ: ngày nhận lương, lễ tết), đội ngũ kỹ sư có thể dễ dàng nhân bản (Scale-out) số lượng container Langfuse Web phía sau Load Balancer mà không cần lo lắng về việc đồng bộ hóa dữ liệu đĩa.

#### 4. Tối ưu Hóa Chi Phí Vận Hành (TCO) & Giảm Thiểu Gánh Nặng Bảo Trì (Operational Excellence):
- Thay vì đội ngũ kỹ sư AI/LLMOps phải kiêm nhiệm công việc quản trị database (sửa lỗi phân mảnh đĩa, cấu hình vacuum, nâng cấp bản vá bảo mật hệ điều hành), các công việc này được tự động hóa 100% bởi Managed Service hoặc đội ngũ DBA chuyên trách của ngân hàng.
- Toàn bộ thời gian và nguồn lực của đội ngũ AI được tập trung vào việc tối ưu chất lượng Prompt, đo lường chi phí Token và cải thiện độ chính xác của RikkeiPay Assistant.

---

## 4. Phân Tích Chuyên Sâu Nhược Điểm & Rủi Ro Của Các Phương Án Bị Loại Bỏ

### 4.1. Nhược Điểm & Rủi Ro Cốt Tử Của Phương Án A (PostgreSQL Local Docker)

```
RỦI RO CỦA PHƯƠNG ÁN A:
┌─────────────────────────────────────────────────────────────┐
│  SINGLE DOCKER HOST (Máy chủ duy nhất)                      │
│                                                             │
│  ┌───────────────────────┐       ┌───────────────────────┐  │
│  │     Langfuse Web      │ ────► │     PostgreSQL        │  │
│  │      Container        │       │     Container         │  │
│  └───────────────────────┘       └───────────┬───────────┘  │
│                                              │              │
│                                              ▼              │
│                                  Local Docker Volume        │
│                                  (/var/lib/postgresql/data) │
│                                                             │
│  [X] docker-compose down -v      ──► MẤT SẠCH DỮ LIỆU!      │
│  [X] Hỏng ổ cứng vật lý          ──► DOWNTIME DÀI HẠN!      │
│  [X] Tăng trưởng hàng triệu log  ──► POSTGRES BỊ OOM/CRASH! │
└─────────────────────────────────────────────────────────────┘
```

1. **Nguy cơ Mất Dữ Liệu Vĩnh Viễn (Catastrophic Data Loss):**
   - Dữ liệu được ánh xạ vào Docker Volume cục bộ trên host. Chỉ cần một câu lệnh thao tác nhầm của kỹ sư vận hành (`docker-compose down -v`) hoặc lỗi hỏng ổ cứng vật lý của máy chủ, toàn bộ dữ liệu lịch sử giao dịch, prompt templates và traces sẽ bị xóa sạch hoàn toàn mà không có cách nào khôi phục.
2. **Không có Khả Năng Khắc Phục Thảm Họa (Zero Disaster Recovery):**
   - Phương án A hoàn toàn không có cơ chế Replicate tự động, không có Multi-AZ và không có Point-In-Time Recovery. Chỉ số **RPO (Recovery Point Objective)** và **RTO (Recovery Time Objective)** có thể kéo dài hàng ngày hoặc hàng tuần, vi phạm nghiêm trọng cam kết SLA của ngân hàng RikkeiPay.
3. **Nghẽn Cổ Chai Hiệu Năng (Performance Degradation & Database Bloat):**
   - PostgreSQL chạy bên trong container bị giới hạn bởi tài nguyên của máy chủ ảo. Khi hệ thống RikkeiPay ghi nhận hàng ngàn trace mỗi phút, bảng `traces` và `generations` sẽ phình to nhanh chóng (bloat table), làm cạn kiệt I/O đĩa (IOPS bottleneck) và gây treo toàn bộ ứng dụng Langfuse.
4. **Vi Phạm Quy Định Tuân Thủ Ngân Hàng (Non-Compliance):**
   - Lưu trữ dữ liệu tài chính nhạy cảm trong một Docker container đơn lẻ không có cơ chế mã hóa KMS và phân quyền đa lớp là hành vi vi phạm trực tiếp các tiêu chuẩn an toàn thông tin bắt buộc trong ngành ngân hàng.

---

### 4.2. Nhược Điểm & Rủi Ro Cốt Tử Của Phương Án B (PostgreSQL + ClickHouse trên Docker Compose)

```
RỦI RO CỦA PHƯƠNG ÁN B:
┌────────────────────────────────────────────────────────────────────────┐
│  SINGLE DOCKER HOST (Máy chủ duy nhất)                                 │
│                                                                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │
│  │   Langfuse Web   │  │    PostgreSQL    │  │      ClickHouse      │  │
│  │   (Next.js App)  │  │    (Metadata)    │  │   (Trace Analytics)  │  │
│  └────────┬─────────┘  └────────┬─────────┘  └──────────┬───────────┘  │
│           │                     │                       │              │
│           └─────────────────────┴───────────────────────┘              │
│                                 │                                      │
│                                 ▼                                      │
│           ┌──────────────────────────────────────────┐                 │
│           │       TRANH CHẤP TÀI NGUYÊN GAY GẮT      │                 │
│           │   (CPU & RAM Contention -> OOM Killer)   │                 │
│           └──────────────────────────────────────────┘                 │
│                                                                        │
│  [X] ClickHouse ngốn RAM đột biến ──► OS OOM Killer tự động tắt App!   │
│  [X] Sự cố máy chủ duy nhất (SPoF)──► TOÀN BỘ 3 SERVICES SẬP CÙNG LÚC! │
│  [X] Rủi ro lệch dữ liệu           ──► Postgres & ClickHouse lệch sync!│
└────────────────────────────────────────────────────────────────────────┘
```

1. **Hiểm Họa Điểm Lỗi Đơn Lẻ (Single Point of Failure - SPoF):**
   - Dù sử dụng 2 cơ sở dữ liệu chuyên biệt hóa (Postgres cho metadata, ClickHouse cho telemetry), nhưng việc gom chung tất cả các container lên cùng một Docker Host duy nhất khiến hệ thống dễ bị sập toàn diện khi máy chủ gặp sự cố phần cứng hoặc mạng.
2. **Hiện Tượng Tranh Chấp Tài Nguyên & Sập Ứng Dụng do OOM (Resource Contention):**
   - ClickHouse là hệ quản trị CSDL cột (Column-Oriented DBMS) có cơ chế ngốn RAM rất lớn khi xử lý các truy vấn tổng hợp phức tạp (Aggregation) hoặc nạp dữ liệu theo lô lớn (Bulk Ingestion).
   - Khi lượng truy cập tăng vọt, ClickHouse sẽ chiếm dụng toàn bộ bộ nhớ RAM của Host, kích hoạt cơ chế **Linux OOM Killer (Out Of Memory Killer)** tự động tắt container Langfuse Web hoặc PostgreSQL, khiến hệ thống giám sát bị tê liệt đột ngột.
3. **Gánh Nặng Vận Hành & Khó Khăn Khi Đồng Bộ Sao Lưu:**
   - Việc sao lưu đồng thời 2 CSDL khác nhau để đạt được tính toàn vẹn dữ liệu (Consistent Backup) tại cùng một thời điểm logic là cực kỳ phức tạp trên Docker Compose.
   - Nếu xảy ra sự cố, việc phục hồi dữ liệu ClickHouse bị lỗi hỏng (corrupted partition) trên Docker Volume đòi hỏi kỹ năng chuyên sâu về ClickHouse internals, làm gia tăng đáng kể thời gian downtime (RTO tăng cao).

---

## 5. Bản Thiết Kế File Triển Khai Thực Tế Cho Phương Án C

Dưới đây là file cấu hình `docker-compose.yml` chuẩn hóa dành cho môi trường doanh nghiệp RikkeiPay kết nối tới Cụm PostgreSQL External an toàn:

```yaml
version: '3.8'

services:
  langfuse-server:
    image: langfuse/langfuse:2
    container_name: rikkeipay-langfuse-server
    restart: always
    ports:
      - "3000:3000"
    environment:
      # Kết nối tới Cụm PostgreSQL External (AWS RDS / On-Premise Enterprise Postgres)
      # Bắt buộc kích hoạt SSL Encryption (sslmode=require)
      DATABASE_URL: "postgresql://${DB_USER}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT:-5432}/${DB_NAME}?sslmode=require&connection_limit=20"
      
      # Khóa bí mật bảo mật hệ thống (Được nạp từ Secret Manager / Vault)
      NEXTAUTH_SECRET: "${NEXTAUTH_SECRET}"
      SALT: "${SALT}"
      NEXTAUTH_URL: "https://langfuse.rikkeipay.internal"
      
      # Tối ưu hóa bảo mật & Tuân thủ ngân hàng
      TELEMETRY_ENABLED: "false"
      LANGFUSE_ENABLE_EXPERIMENTAL_FEATURES: "false"
      
      # Cấu hình bộ nhớ Node.js tối ưu cho container
      NODE_OPTIONS: "--max-old-space-size=2048"
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:3000/api/public/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 30s
    logging:
      driver: "json-file"
      options:
        max-size: "50m"
        max-file: "5"
```

---

## 6. Tổng Kết Khuyến Nghị Kiến Trúc Cho RikkeiPay

1. **Phân Định Môi Trường Rõ Ràng:**
   - **Local Dev Máy Kỹ Sư:** Sử dụng **Phương án A** (`postgres:alpine` trong docker-compose) để dev offline nhanh, nhẹ máy.
   - **Production Ngân Hàng:** Bắt buộc áp dụng **Phương án C** kết hợp **AWS RDS PostgreSQL (Multi-AZ)** hoặc **Enterprise Postgres Cluster nội bộ** được bảo vệ trong Private Subnet.
2. **Kế Hoạch Mở Rộng Tương Lai (Roadmap to ClickHouse Managed):**
   - Khi quy mô RikkeiPay vượt mốc **10.000.000 traces/ngày**, tiếp tục mở rộng kiến trúc Phương án C bằng cách tích hợp thêm **Managed ClickHouse Cluster riêng biệt** (như ClickHouse Cloud hoặc Cụm ClickHouse On-Premise độc lập có giám sát chuyên dụng) mà không đặt chung container vào cùng host với Web App.
