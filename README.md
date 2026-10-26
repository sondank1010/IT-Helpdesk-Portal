# IT-Helpdesk-Portal
Bài tập lớn KTPM năm học 2025 2026

## CI: đóng gói và xuất bản image lên GHCR

Luồng: push code lên `main` → Maven `clean verify` → build JAR → Docker build → push GHCR.
Không có bước deploy hoặc chạy ứng dụng trong workflow.

Các file cấu hình:

- `backend/Dockerfile`: đóng JAR đã build vào image có Java 21 JRE.
- `backend/.dockerignore`: chỉ gửi JAR vào Docker build context.
- `.github/workflows/ci.yml`: build và publish tự động bằng GitHub Actions.
- `docker-compose.yaml`: cấu hình chạy ứng dụng và MySQL riêng; không tham gia luồng CI.

### Đưa cấu hình lên GitHub

Từ thư mục gốc repository:

```powershell
git add .github/workflows/ci.yml backend/Dockerfile backend/.dockerignore docker-compose.yaml README.md
git commit -m "Add Docker packaging and GHCR CI"
git push origin main
```

Vào repository trên GitHub → **Actions** → **Build and publish to GHCR** để xem kết quả từng bước.
Nếu repository chưa bật Actions, bật trong **Settings → Actions → General**.
Workflow khai báo `contents: read` và `packages: write`; GitHub tự cấp `GITHUB_TOKEN`.
Không cần tạo PAT, thêm secret hoặc tạo file `.env` để publish.

Khi thành công, package được liên kết với repository và có image:

```text
ghcr.io/sondank1010/it-helpdesk-portal:latest
ghcr.io/sondank1010/it-helpdesk-portal:sha-<full-commit-sha>
```

- Push lên `main`: build và publish cả hai tag.
- Pull request vào `main`: build để kiểm tra, không đăng nhập/publish GHCR.
- Có thể chạy thủ công bằng **Run workflow**; chỉ chọn nhánh `main` mới publish.
- `verify` chạy các test nếu có. Hiện repository chưa có test nên build thành công chưa chứng minh ứng dụng kết nối database hay hoạt động đúng.
- Package mới mặc định private. Không cần đổi public để CI publish.

### Vai trò của Compose và MySQL

Compose tham chiếu image ứng dụng trên GHCR và image `mysql:8.4` riêng.
Image ứng dụng chỉ chứa Java runtime và JAR, không chứa MySQL server hoặc dữ liệu database.
File `helpdesk_mysql.sql` được mount vào MySQL để tạo schema khi volume dữ liệu còn mới;
sửa file SQL không tự cập nhật database trong volume đã được khởi tạo.
Volume `mysql-data` giữ dữ liệu MySQL qua các lần tạo lại container.

Compose dùng thông tin đăng nhập mẫu cho môi trường local; không cần `.env` cho ví dụ này.
Ứng dụng kết nối hostname `mysql` trong mạng Compose, không dùng `localhost`.
CI không chạy Compose, không pull image về máy bạn, và không triển khai hệ thống.
