# Security Management API

Quản lý bảo mật tài khoản, khóa/mở khóa tài khoản, thay đổi trạng thái user.

## 1. Mở khóa tài khoản
- **Endpoint:** `POST /api/admin/security/unlock-account/{userId}`
- **Response:**
```json
{
  "status": 200,
  "message": "Account unlocked successfully"
}
```

## 2. Khóa tài khoản
- **Endpoint:** `POST /api/admin/security/lock-account/{userId}`
- **Request:**
```json
{
  "reason": "Lý do khóa"
}
```
- **Response:**
```json
{
  "status": 200,
  "message": "Account locked successfully"
}
```

## 3. Thay đổi trạng thái user
- **Endpoint:** `PUT /api/admin/security/change-status/{userId}`
- **Request:**
```json
{
  "status": "ACTIVE | INACTIVE | LOCKED",
  "reason": "Lý do thay đổi"
}
```
- **Response:**
```json
{
  "status": 200,
  "message": "User status changed successfully"
}
```
