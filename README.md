# springboot1-9 — Shop: Register/OTP, Login, Forgot password, User & Product CRUD

Spring Boot 4.1.1 · Spring Security 7.1 · Hibernate 7 · Thymeleaf (+ Security dialect + Layout dialect) · MapStruct 1.6.3 · Lombok 1.18.48 · Spring Mail · Cloudinary · SQL Server

## Chức năng
| Nhóm | Nội dung |
|---|---|
| Authentication | Đăng ký → gửi OTP email → xác nhận OTP (có Resend OTP) · Login bằng **username hoặc email** (lưu session) · Logout |
| Quên mật khẩu | Nhập email → gửi OTP → nhập OTP + mật khẩu mới |
| User (ADMIN) | CRUD, tìm kiếm (username/email/họ tên), phân trang, role USER/ADMIN, đếm tổng user, đếm product của từng user |
| Product | CRUD, tìm kiếm (tên/mô tả), phân trang, upload ảnh Cloudinary, product thuộc user, đếm product |

Quy tắc phân quyền: `/users/**` và `/admin/**` chỉ ADMIN. `/products/**` cần đăng nhập; **chỉ chủ sản phẩm hoặc ADMIN** mới được sửa/xóa.

## Chạy nhanh (H2, không cần SQL Server / mail / Cloudinary)
    mvn spring-boot:run -Dspring-boot.run.profiles=h2
    → http://localhost:8081   (user01 / 123456  hoặc  user01@gmail.com / 123456; admin01 / 123456)

Không cấu hình SMTP thì đăng ký/quên mật khẩu sẽ báo lỗi gửi email; không cấu hình Cloudinary thì chỉ tạo/sửa product **không kèm ảnh** được.

## Chạy với SQL Server + Gmail + Cloudinary
1. Tạo database `webst9` (hoặc chạy `sql/init-sqlserver.sql`; không bắt buộc vì `ddl-auto=update` tự tạo bảng và `DataInitializer` tự thêm role + 2 tài khoản mẫu).
2. Copy `.env.example` thành `.env` (cùng thư mục `pom.xml`) và điền `DB_*`, `MAIL_*`, `CLOUDINARY_*`.
   - Gmail: bật xác minh 2 bước → tạo **App password** 16 ký tự → dán vào `MAIL_PASSWORD`.
   - Cloudinary: lấy cloud name / API key / API secret trong Dashboard.
3. `mvn spring-boot:run`

`.env` đã nằm trong `.gitignore` — không commit file này.

## Kiểm thử
    mvn test
- `LoginFlowTest`: đăng nhập, phân quyền, header.
- `OtpAndProductFlowTest`: đăng ký + OTP, quên mật khẩu + OTP (mock `EmailService`), quyền sửa/xóa product, trang `/users` chỉ ADMIN.

## Cấu trúc
    config/      SecurityConfig, CloudinaryConfig, DataInitializer
    controller/  Home, Auth, User, Product
    dto/         UserDTO, ProductDTO, RegisterDTO, VerifyOtpDTO, ForgotPasswordDTO, ResetPasswordDTO, LoginDTO
    entity/      User, Role, Product, OtpToken
    exception/   GlobalExceptionHandler
    mapper/      UserMapper, ProductMapper (MapStruct)
    repository/  User, Role, Product, OtpToken
    security/    CustomUserDetails, CustomUserDetailsService
    service/     interface + impl: Auth, Otp, Email, Cloudinary, User, Product

## Yêu cầu
JDK 17–26 (mặc định `java.version=24` trong pom.xml), Maven 3.6.3+.
