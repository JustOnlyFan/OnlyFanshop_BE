# Cập nhật Bảo mật & Xác thực (2025-11)

Tài liệu tóm tắt các thay đổi đã triển khai trong Backend `OnlyFanshop_BE` để tăng cường bảo mật, tính sẵn sàng và khả năng mở rộng.

## 1) Access/Refresh Token + TTL + Refresh Endpoint
- Tách 2 loại token: `ACCESS` và `REFRESH` (bảng `Tokens`).
- Mỗi token có `expiresAt`, `expired`, `revoked` để quản lý vòng đời.
- `ACCESS` token có TTL ngắn (mặc định 30 phút), `REFRESH` token có TTL dài (mặc định 7 ngày).
- Endpoint mới: `POST /login/refresh` nhận `refreshToken` và trả về `accessToken` mới.

Cấu hình TTL trong `application.properties`:
```
jwt.access.ttlMinutes=30
jwt.refresh.ttlDays=7
```

Ảnh hưởng mã nguồn chính:
- `security/JwtTokenProvider.java`: thêm `generateAccessToken(...)`, `generateRefreshToken(...)` và expiration.
- `entity/Token.java` + `enums/TokenType.java`: mở rộng schema token.
- `service/LoginService.java`: tạo/lưu ACCESS và REFRESH khi đăng nhập; xử lý `/login/refresh`.
- `controller/LoginController.java`: thêm endpoint `/login/refresh`.

## 2) Revoke token khi đổi/reset mật khẩu
- Sau khi user đổi hoặc reset mật khẩu, tất cả token hợp lệ hiện tại bị gán `expired=true`, `revoked=true`.
- Vị trí:
  - `service/UserService.java::changePassword(...)`
  - `service/LoginService.java::resetPassword(...)`

## 3) RBAC cơ bản (ADMIN/STAFF/CUSTOMER)
- Thêm role `STAFF` bên cạnh `ADMIN`, `CUSTOMER` (`enums/Role.java`).
- Áp `@PreAuthorize` cho các endpoint quản trị sản phẩm:
  - Tạo/Cập nhật/Đổi ảnh/Kích hoạt: `ADMIN` hoặc `STAFF`.
  - Xóa: chỉ `ADMIN`.
- Vị trí: `controller/ProductController.java`.
- Lưu ý: Có thể mở rộng tương tự cho `Category`, `Brand`, `Order`, `User`, `Notification`, `FileUpload`, v.v.

## 4) Rate limiting cho đăng nhập
- Thêm `LoginRateLimitFilter` giới hạn số request tới `/login/signin` theo cửa sổ thời gian.
- Mặc định: 20 yêu cầu/60 giây theo IP (cấu hình được).
- Cấu hình:
```
rate.login.windowSeconds=60
rate.login.maxRequests=20
```
- Tích hợp filter trong `SecurityConfig` trước `JwtAuthenticationFilter`.

## 5) Thay đổi cấu trúc/DB
- Bảng `Tokens` bổ sung cột:
  - `type (ACCESS|REFRESH)`, `expiresAt (Instant)`, `expired`, `revoked`.
- `spring.jpa.hibernate.ddl-auto=update` sẽ tự động migrate trong môi trường dev. Với prod, khuyến nghị dùng migration tool (Flyway/Liquibase).

## 6) Cách client tích hợp
- Đăng nhập `POST /login/signin` → nhận `token` (access) và `refreshToken`.
- Gọi API bảo vệ bằng header: `Authorization: Bearer <accessToken>`.
- Khi `accessToken` hết hạn, gọi `POST /login/refresh` với body:
```
{
  "refreshToken": "..."
}
```
- Backend trả về `accessToken` mới; tiếp tục sử dụng mà không cần đăng nhập lại.

## 7) Ghi chú bảo mật khuyến nghị (tiếp theo)
- (Khuyến nghị) Rotate refresh token khi refresh: phát hành refresh mới và revoke refresh cũ.
- Bổ sung audit log cho hành động nhạy cảm: đăng nhập, đổi mật khẩu, thanh toán.
- Áp dụng RBAC đầy đủ cho các controller còn lại; thêm test cho policy.
- Chuyển rate limit sang store phân tán (Redis) khi scale nhiều instance.

## 8) Tham chiếu lớp & file chính
- `security/JwtTokenProvider.java`
- `security/JwtAuthenticationFilter.java`
- `config/SecurityConfig.java`
- `config/LoginRateLimitFilter.java`
- `controller/LoginController.java`
- `service/LoginService.java`, `service/UserService.java`
- `entity/Token.java`, `enums/TokenType.java`, `enums/Role.java`
- `repository/TokenRepository.java`

---
Nếu cần, mình có thể tiếp tục: mở rộng RBAC cho các controller khác, bật audit log, và triển khai refresh token rotation.

