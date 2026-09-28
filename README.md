# springboot1-9 — Custom login (username hoặc email)

Spring Boot 4.1.1 · Spring Security 7.1 · Hibernate 7 · Thymeleaf + Security dialect + Layout dialect · MapStruct 1.6.3 · Lombok 1.18.48

## Chạy nhanh (H2, không cần SQL Server)
    mvn spring-boot:run -Dspring-boot.run.profiles=h2
    → http://localhost:8081   (user01 / 123456  hoặc  user01@gmail.com / 123456; admin01 / 123456)

## Chạy với SQL Server
Tạo database `webst9`, rồi đặt biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (hoặc sửa `application.properties`):
    mvn spring-boot:run

## Kiểm thử
    mvn test        # LoginFlowTest chạy trên H2

## Yêu cầu
JDK 17–26 (mặc định `java.version=26` trong pom.xml), Maven 3.6.3+.

## Dữ liệu SQL Server
`sql/init-sqlserver.sql` tạo database `webst9`, bảng `roles`/`users` và 2 tài khoản mẫu (mật khẩu 123456, BCrypt).
Không bắt buộc: nếu bỏ qua, Hibernate (`ddl-auto=update`) tự tạo bảng và `DataInitializer` tự thêm dữ liệu khi khởi động.
