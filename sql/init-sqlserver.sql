/* =====================================================================
   springboot1-9 - Tạo database, bảng và dữ liệu mẫu cho SQL Server
   Mật khẩu của cả 2 tài khoản: 123456 (đã mã hóa BCrypt)
   Chạy trong SSMS / Azure Data Studio. Có thể chạy lại nhiều lần.
   ===================================================================== */
IF DB_ID(N'webst9') IS NULL CREATE DATABASE webst9;
GO
USE webst9;
GO

/* ---------- Bảng (khớp với entity Role / User / Product / OtpToken) ---------- */
IF OBJECT_ID(N'dbo.roles', N'U') IS NULL
CREATE TABLE dbo.roles (
    id    BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name  VARCHAR(50) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
);
GO

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
CREATE TABLE dbo.users (
    id        BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    username  VARCHAR(50)   NOT NULL,
    email     VARCHAR(150)  NOT NULL,
    password  VARCHAR(255)  NOT NULL,
    full_name NVARCHAR(200) NULL,
    images    VARCHAR(500)  NULL,
    enabled   BIT           NOT NULL DEFAULT 1,
    role_id   BIGINT        NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id)
);
GO

IF OBJECT_ID(N'dbo.products', N'U') IS NULL
CREATE TABLE dbo.products (
    id              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name            NVARCHAR(500)  NOT NULL,
    description     NVARCHAR(2000) NULL,
    price           DECIMAL(18,2)  NOT NULL,
    image_url       VARCHAR(1000)  NULL,      -- secure_url Cloudinary
    image_public_id VARCHAR(255)   NULL,      -- public_id Cloudinary (để xóa/thay ảnh)
    user_id         BIGINT         NOT NULL,  -- 1 user - n product
    created_at      DATETIME2(6)   NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_products_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);
GO

IF OBJECT_ID(N'dbo.otp_tokens', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.otp_tokens (
        id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        email      VARCHAR(150) NOT NULL,
        otp_hash   VARCHAR(100) NOT NULL,      -- BCrypt hash của OTP
        type       VARCHAR(30)  NOT NULL,      -- REGISTER | RESET_PASSWORD
        expires_at DATETIME2(6) NOT NULL,
        attempts   INT          NOT NULL DEFAULT 0,
        used       BIT          NOT NULL DEFAULT 0,
        created_at DATETIME2(6) NOT NULL
    );
    CREATE INDEX idx_otp_email_type ON dbo.otp_tokens(email, type);
END
GO

/* ---------- Dữ liệu mẫu ---------- */
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = 'ROLE_USER')  INSERT INTO dbo.roles(name) VALUES ('ROLE_USER');
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = 'ROLE_ADMIN') INSERT INTO dbo.roles(name) VALUES ('ROLE_ADMIN');
GO

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE username = 'user01')
INSERT INTO dbo.users(username, email, password, full_name, images, enabled, role_id)
VALUES ('user01', 'user01@gmail.com',
        '$2a$10$lp08aWkh7bNIw0YDMF5xl.aaZM3zcD.srotPgY/CdpT.D/sc6Z6ke',
        N'Nguyễn Hữu Trung', '/images/user.png', 1,
        (SELECT id FROM dbo.roles WHERE name = 'ROLE_USER'));

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE username = 'admin01')
INSERT INTO dbo.users(username, email, password, full_name, images, enabled, role_id)
VALUES ('admin01', 'admin01@gmail.com',
        '$2a$10$lp08aWkh7bNIw0YDMF5xl.aaZM3zcD.srotPgY/CdpT.D/sc6Z6ke',
        N'Quản trị viên', '/images/user.png', 1,
        (SELECT id FROM dbo.roles WHERE name = 'ROLE_ADMIN'));
GO

SELECT u.id, u.username, u.email, u.full_name, r.name AS role, u.enabled
FROM dbo.users u JOIN dbo.roles r ON r.id = u.role_id;
GO
