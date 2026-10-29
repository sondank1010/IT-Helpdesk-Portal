# IT-Helpdesk-Portal
Bài tập lớn KTPM năm học 2026 - 2027, Nhóm 3 - Ca 3  
Thành viên:  
  Đặng Hoàng Sơn  
  Cao Vũ Nhật Triều - 23021740  
  Trần Trung Hậu  
  Phan Bá Thọ  

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
| POST | /api/tickets | Đã đăng nhập | 
| **GET** | /api/tickets | Đã đăng nhập; User chỉ thấy ticket của mình | 
| **GET** | /api/tickets/{id} | Xem ticket | 
| **PUT, PATCH** | /api/tickets/{id} | User khi ticket NEW; Agent được gán; Admin | 
| **PATCH** | /api/tickets/{id}/status | User chỉ đóng ticket RESOLVED; Agent được gán; Admin |
| **PATCH** | /api/tickets/{id}/assign | Admin |
| **DELETE** | /api/tickets/{id} | User khi ticket NEW; Admin |
| **GET** | /api/tickets/{id}/history | Xem ticket |  
| GET | /api/categories | Công khai |
| **POST** | /api/categories | Admin | 
| **PUT, PATCH** | /api/categories/{id} | Admin | 
| **DELETE** | /api/categories/{id} | Admin |  

## Cơ sở dữ liệu
<img width="1450" height="737" alt="image" src="https://github.com/user-attachments/assets/14609ffb-d696-4ecb-ba55-c2fa80ce441a" />


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
