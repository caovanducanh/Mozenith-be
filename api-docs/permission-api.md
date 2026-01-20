# Permission API

Quản lý quyền hệ thống.

## 1. Lấy danh sách quyền
- **Endpoint:** `GET /api/admin/permissions`
- **Response:**
```json
{
  "statusCode": 200,
  "message": "Danh sách quyền",
  "data": [
    {
      "id": 1,
      "code": "PERMISSION_VIEW",
      "name": "Xem quyền"
    },
    {
      "id": 2,
      "code": "PERMISSION_UPDATE",
      "name": "Cập nhật quyền"
    }
  ]
}
```

## 2. Cập nhật tên quyền
- **Endpoint:** `PUT /api/admin/permissions/{id}`
- **Request body:**
```json
{
  "name": "Tên quyền mới",
  "reason": "Lý do cập nhật"
}
```
- **Response:**
```json
{
  "statusCode": 200,
  "message": "Cập nhật thành công",
  "data": {
    "id": 1,
    "code": "PERMISSION_VIEW",
    "name": "Tên quyền mới"
  }
}
```
