# IT-Helpdesk-Portal
Bài tập lớn KTPM năm học 2025 2026

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
