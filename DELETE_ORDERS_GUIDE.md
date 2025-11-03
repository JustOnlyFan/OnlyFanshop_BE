# Hướng dẫn xóa tất cả đơn hàng mẫu

## Endpoint mới
**DELETE** `/order/deleteAllOrders`

## Yêu cầu
- Cần đăng nhập với vai trò **ADMIN**
- Cần gửi JWT token trong header `Authorization`

## Cách sử dụng

### 1. Sử dụng Postman/Thunder Client

**Request:**
```
DELETE http://localhost:8080/order/deleteAllOrders
Headers:
  Authorization: Bearer <your-admin-jwt-token>
```

### 2. Sử dụng curl

```bash
curl -X DELETE http://localhost:8080/order/deleteAllOrders \
  -H "Authorization: Bearer <your-admin-jwt-token>"
```

### 3. Sử dụng PowerShell

```powershell
$token = "<your-admin-jwt-token>"
$uri = "http://localhost:8080/order/deleteAllOrders"

Invoke-RestMethod -Uri $uri -Method Delete -Headers @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}
```

## Response

**Thành công:**
```json
{
    "statusCode": 200,
    "message": "Đã xóa tất cả đơn hàng thành công",
    "data": null
}
```

**Lỗi không phải admin:**
```json
{
    "statusCode": 403,
    "message": "Chỉ admin mới có quyền xóa tất cả đơn hàng",
    "data": null
}
```

## Lưu ý
⚠️ **CẢNH BÁO**: Endpoint này sẽ xóa **TẤT CẢ** orders và order items trong database. Hành động này không thể hoàn tác!

## Lấy admin JWT token

1. Đăng nhập với tài khoản admin
2. Copy JWT token từ response
3. Sử dụng token này trong header Authorization

## Lưu ý về database

Nếu bạn muốn xóa trực tiếp từ database (MySQL/PostgreSQL):

```sql
-- Xóa order items trước
DELETE FROM order_item;

-- Sau đó xóa orders
DELETE FROM `order`;

-- Hoặc reset auto increment
ALTER TABLE `order` AUTO_INCREMENT = 1;
ALTER TABLE order_item AUTO_INCREMENT = 1;
```


