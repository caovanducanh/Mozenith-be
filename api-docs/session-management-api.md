# Session Management API

Quản lý phiên đăng nhập, đăng xuất thiết bị.

## 1. Đăng xuất thiết bị hiện tại
- **Endpoint:** `POST /api/session/logout`
- **Response:**
```json
{
  "status": 200,
  "message": "Logged out successfully"
}
```

## 2. Đăng xuất tất cả thiết bị
- **Endpoint:** `POST /api/session/logout-all`
- **Request:**
```json
{
  "reason": "Lý do đăng xuất tất cả"
}
```
- **Response:**
```json
{
  "status": 200,
  "message": "Logged out from all devices successfully"
}
```

## 3. Admin force logout user
- **Endpoint:** `POST /api/session/force-logout/{userId}`
- **Request:**
```json
{
  "reason": "Lý do bắt buộc đăng xuất"
}
```
- **Response:**
```json
{
  "status": 200,
  "message": "User logged out from all devices successfully"
}
```

## 4. Đếm số phiên hoạt động
- **Endpoint:** `GET /api/session/active-count`
- **Response:**
```json
{
  "status": 200,
  "message": "Số phiên hoạt động",
  "data": 3
}
```
