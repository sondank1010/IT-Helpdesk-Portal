# IT-Helpdesk-Portal
Bài tập lớn KTPM năm học 2026 - 2027, Nhóm 3 - Ca 3  
Thành viên:  
| Thành viên | Mã sinh viên |
| :---- | :---- |
| Đặng Hoàng Sơn | 23021680  |
| Cao Vũ Nhật Triều | 23021740 |
| Trần Trung Hậu | 23020061 |
| Phan Bá Thọ | 23020163 |

## Tổng quan Bài toán
* **Helpdesk Management System** là một hệ thống quản lý và xử lý yêu cầu hỗ trợ (support ticket) giữa người dùng và bộ phận hỗ trợ kỹ thuật. Hệ thống cho phép người dùng tạo các yêu cầu hỗ trợ, theo dõi tiến trình xử lý, trao đổi thông tin với nhân viên hỗ trợ, trong khi quản trị viên có thể quản lý người dùng, phân công ticket, theo dõi trạng thái và đánh giá hiệu quả xử lý.
* **Mục tiêu:** quản lý yêu cầu hỗ trợ IT từ lúc tiếp nhận, phân công, xử lý đến khi đóng ticket, có lưu vết toàn bộ thay đổi.  
* **Vai trò:** User tạo và theo dõi ticket của mình. Agent xử lý ticket được giao. Admin phân công quản lý. 
* **Dữ liệu chính:** tài khoản và vai trò, phiên đăng nhập, danh mục, ticket, bình luận, tệp đính kèm, lịch sử ticket, thông báo.

## Nghiệp vụ chính

* **Tạo ticket:** User đăng nhập, tạo ticket với tiêu đề, mô tả, danh mục và mức ưu tiên, sau đó có thể tải tệp minh chứng.  
* **Xử lý:** Agent chuyển ticket sang IN\_PROGRESS, trao đổi với User qua bình luận, xong thì chuyển RESOLVED.  
* **Đóng hoặc mở lại:** User xác nhận và đóng ticket RESOLVED. Agent hoặc Admin cũng có thể đóng. Nếu chưa xong, ticket được chuyển lại IN\_PROGRESS.  
* **Luồng trạng thái:** NEW → IN\_PROGRESS → RESOLVED → CLOSED, cho phép RESOLVED → IN\_PROGRESS. CLOSED là trạng thái cuối, không bình luận được nữa.  
* **Lưu vết và thông báo:** các thao tác tạo, sửa, đổi trạng thái, gán và bình luận đều ghi lịch sử. Người tạo và Agent được gán nhận thông báo trong ứng dụng khi có thay đổi.

## Công nghệ - Phạm vi

| Thành phần | Công nghệ |
| :---- | :---- |
| Ngôn ngữ, framework | Spring Boot 3 Java 21 |
| Cơ sở dữ liệu | Spring Data JPA, MySQL 8 |
| REST API | dùng JSON |
| Xác thực | JWT - Access Token, Refresh Token |
| Đóng gói | Chạy bằng Docker Compose. Không có giao diện người dùng. |

## Danh sách tóm tắt API Endpoints

| Method | Endpoint | Quyền |
| :---- | :---- | :---- |
| **POST** | /api/auth/register | Công khai |
| **POST** | /api/auth/login | Công khai | 
| **POST** | /api/auth/refresh-token | Công khai | 
| **POST** | /api/auth/logout | Đã đăng nhập | 
| **GET** | /api/auth/me | Đã đăng nhập | Không có |
| **PUT** | /api/users/profile | Đã đăng nhập | 
| **POST** | /api/tickets | Đã đăng nhập | 
| **GET** | /api/tickets | Đã đăng nhập; User chỉ thấy ticket của mình | 
| **GET** | /api/tickets/{id} | Xem ticket | 
| **PUT, PATCH** | /api/tickets/{id} | User khi ticket NEW; Agent được gán; Admin | 
| **PATCH** | /api/tickets/{id}/status | User chỉ đóng ticket RESOLVED; Agent được gán; Admin |
| **PATCH** | /api/tickets/{id}/assign | Admin |
| **DELETE** | /api/tickets/{id} | User khi ticket NEW; Admin |
| **GET** | /api/tickets/{id}/history | Xem ticket |  
| **GET** | /api/categories | Công khai |
| **POST** | /api/categories | Admin | 
| **PUT, PATCH** | /api/categories/{id} | Admin | 
| **DELETE** | /api/categories/{id} | Admin |  

## Kiến trúc tổng quan
* Kiến trúc phân tầng cho backend Spring Boot, kết hợp REST API xử lý yêu cầu, Spring Data JPA truy cập MySQL-JWT xác thực, Docker Compose triển khai. 

Client gọi API

   │ HTTP, REST, JSON
   ▼
Spring Security (JWT, session: thiết lập quy tắc truy cập; kiểm tra JWT và thiết lập thông tin                                người dùng đã xác thực.)
   │ 
   ▼
  API         (Auth, Ticket, request-response: tiếp nhận request, kiểm tra dữ liệu đầu vào và chuyển xử lý)
   │ 
   ▼   
Service       (Xử lý nghiệp vụ, trạng thái: Phân công, lịch sử, quyền)
   │ 
   ▼   
Repository    (Spring Data JPA: Truy vấn, lưu dữ liệu)
   │ 
   ▼   
 Model        (Entity Biểu diễn các đối tượng nghiệp vụ, ánh xạ đối tượng với bảng dữ liệu bằng JPA)
   │ 
   ▼   
MySQL 8 (Lưu trữ)



* Tổ chức dự án:

IT-Helpdesk-Portal/
|
|-- .github/
|   `-- workflows/
|       `-- ci.yml  -> GitHub Actions: kiểm thử, đóng gói và xuất bản Docker image
|           
|           
|
|-- .idea/  
|      
|
|-- backend/
|   |
|   |-- src/
|   |   `-- main/
|   |       `-- java/com/example/backend/
|   |           |
|   |           |-- BackendApplication.java
|   |           |      
|   |           |
|   |           |-- api/   -> Controller xử lý REST API: Auth, Ticket, Category
|   |           |      
|   |           |
|   |           |-- config/  -> Cấu hình Spring Security
|   |           |      
|   |           |
|   |           |-- security/  => JWT, bộ lọc xác thực, xử lý mật khẩu
|   |           |
|   |           |-- service/   => Nghiệp vụ xác thực, ticket và danh mục
|   |           |
|   |           |-- repository => Truy cập dữ liệu bằng JPA
|   |           |
|   |           `-- model/  -> Entity, enum và các đối tượng dữ liệu
|   |                  
|   |
|   |-- pom.xml  -> Thư viện và cấu hình Maven
|   |      
|   |
|   |-- mvnw  -> Maven Wrapper build dự án
|   |      
|   |
|   |-- Dockerfile -> Đóng gói ứng dụng thành Docker image
|   |      
|   |
|   `-- .dockerignore  ->  Quy định các tệp loại trừ khi build image
|         
|
|-- helpdesk_mysql.sql  -> Khai báo cấu trúc cơ sở dữ liệu
|       
|
|-- docker-compose.yaml  -> Khởi chạy Backend và MySQL
|       
|
-- README.md    -> Mô tả dự án, nghiệp vụ và API


## Cơ sở dữ liệu
<img width="1450" height="737" alt="image" src="https://github.com/user-attachments/assets/14609ffb-d696-4ecb-ba55-c2fa80ce441a" />

- app_users: Tài khoản người dùng, với role là 1 thuộc User, Admin, Agent.
- login_sessions: Phiên đăng nhập, 1 id người dùng có thể có nhiều phiên đăng nhập cùng trên các thiết bị khác nhau.
- tickets, ticket_history, categories: Lưu mỗi yêu cầu (1 ticket) của người dùng lên hệ thống IT Support với các thông tin đi kèm (tình trạng, ngày, mô tả...), và lịch sử ticket, phân loại.
- attachments, ticket_comments: Bình luận trao đổi và cho phép đính kèm file trong trao đổi.
- notifications: Mục thông báo gắn liền với mỗi người dùng và quá trình 1 ticket của người dùng được xử lý.
-> Trong quá trình Phase 1, một số trường có thể chưa cần sử dụng để đơn giản hoá nghiệp vụ.

## Kiểm thử Tải Kaggle
Các kết quả sau chạy đo đạc thu được:
<img width="1131" height="105" alt="image" src="https://github.com/user-attachments/assets/f5e62187-550b-4210-a3f2-c50b67c7a785" />
<img width="958" height="832" alt="image" src="https://github.com/user-attachments/assets/91243c09-7ba5-4b88-a1d7-05ad3e0b3a2e" />
<img width="766" height="823" alt="image" src="https://github.com/user-attachments/assets/ec4b2799-b15d-476e-8842-cb0cebfb8eb1" />

Chạy tại: [KaggleTest](https://www.kaggle.com/code/haaju20/notebook67708c4843)
Kết luận chính:
* 10 → 50 user: số user tăng 5 lần, throughput tăng 4 lần. 
* 50 → 100 user: số user tăng 2 lần, throughput tăng 2 lần, p95 từ 25 ms lên 29 ms 
* Throughput cao nhất: 78 req/s tại 100 user.
* Ứng dụng và MySQL chạy chung một máy nên tranh chấp CPU. số liệu dùng để so sánh tương đối giữa các mức tải. Dữ liệu ban đầu ít, nếu là với dữ liệu lớn, truy vấn danh sách có thể chậm hơn.

## Luồng CI đóng gói docker iamge và đẩy lên container registry của github

Luồng: push code lên `main` → Maven `clean verify` → build JAR → Docker build → push GHCR. (file CI.yml trong folder .github)

Config gồm:
- `maven` : Build jar từ app
- `backend/Dockerfile`: đóng JAR đã build vào image có Java 21 JRE.
- `backend/.dockerignore`: chỉ gửi JAR vào Docker build context.
- `.github/workflows/ci.yml`: build và publish tự động bằng GitHub Actions.
  
** `docker-compose.yaml`: sẽ pull image từ ghcr về kèm với image mysql, ko liên quan đến luồng CI.

### Đưa cấu hình lên GitHub
cd vào thư mục repository
```powershell
git add .github/workflows/ci.yml backend/Dockerfile backend/.dockerignore docker-compose.yaml README.md
git commit -m "Add Docker packaging and GHCR CI"
git push origin main
```

Khi thành công, package được liên kết với repository và có image:

```text
ghcr.io/sondank1010/it-helpdesk-portal:latest
ghcr.io/sondank1010/it-helpdesk-portal:sha-<full-commit-sha>
```
### Pull về chạy compose không cần môi trường + mysql-server:
cd vào thư mục -> docker compose up -d -> chạy file compose.yml
