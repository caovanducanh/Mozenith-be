# Error Response - GlobalExceptionHandler

Tất cả các API đều trả về lỗi theo chuẩn dưới đây khi có exception hoặc lỗi nghiệp vụ. Frontend chỉ cần bắt theo cấu trúc này để xử lý.

## Cấu trúc response lỗi chung
```json
{
  "statusCode": 400,
  "message": "Thông báo lỗi chi tiết",
  "data": null
}
```

- `statusCode`: mã lỗi HTTP (400, 401, 403, 404, 409, 500...)
- `message`: mô tả lỗi chi tiết, có thể dùng để hiển thị cho user hoặc log
- `data`: luôn là null với lỗi hệ thống, có thể chứa thông tin chi tiết hơn nếu là lỗi nghiệp vụ đặc biệt

## Ví dụ các lỗi thường gặp

### 1. Lỗi xác thực (401)
```json
{
  "statusCode": 401,
  "message": "Unauthorized: Token is invalid or expired",
  "data": null
}
```

### 2. Lỗi phân quyền (403)
```json
{
  "statusCode": 403,
  "message": "Forbidden: You do not have permission to access this resource",
  "data": null
}
```

### 3. Lỗi không tìm thấy (404)
```json
{
  "statusCode": 404,
  "message": "Not Found: Resource does not exist",
  "data": null
}
```

### 4. Lỗi xung đột dữ liệu (409)
```json
{
  "statusCode": 409,
  "message": "Conflict: Username already exists",
  "data": null
}
```

### 5. Lỗi validate dữ liệu (400)
```json
{
  "statusCode": 400,
  "message": "Validation failed: Email is invalid",
  "data": null
}
```

### 6. Lỗi hệ thống (500)
```json
{
  "statusCode": 500,
  "message": "Internal Server Error: Something went wrong",
  "data": null
}
```

## Lưu ý
- Frontend chỉ cần kiểm tra `statusCode` và đọc `message` để hiển thị hoặc xử lý logic phù hợp.
- Các lỗi trả về đều thống nhất theo cấu trúc này nhờ GlobalExceptionHandler của backend.
