# Role API

Quản lý vai trò hệ thống.

## 1. Lấy danh sách vai trò
- **Endpoint:** `GET /api/admin/roles`
- **Response:**
```json
{
  "status": 200,
  "message": "Danh sách vai trò",
  "data": [
    { "id": 1, "name": "ADMIN" },
    ...
  ]
}
```

## 2. Tạo vai trò mới
- **Endpoint:** `POST /api/admin/roles`
- **Request:**
```json
{
  "name": "Tên vai trò",
  "permissions": ["PERMISSION_VIEW", ...]
}
```
- **Response:**
```json
{
  "status": 200,
  "message": "Tạo vai trò thành công"
}
```

## 3. Cập nhật vai trò
- **Endpoint:** `PUT /api/admin/roles/{id}`
- **Request:**
```json
{
  "name": "Tên mới",
  "permissions": ["PERMISSION_VIEW", ...]
}
```
- **Response:**
```json
{
  "status": 200,
  "message": "Cập nhật thành công"
}
```

## 4. Xóa vai trò
- **Endpoint:** `DELETE /api/admin/roles/{id}`
- **Request:**
```json
{
  "reason": "Lý do xóa"
}
```
- **Response:**
```json
{
  "status": 200,
  "message": "Xóa thành công"
}
```

## 5. Cập nhật quyền cho vai trò
- **Endpoint:** `PUT /api/admin/roles/{id}/permissions`
- **Request:**
```json
{
  "permissions": ["PERMISSION_VIEW", ...]
}
```
- **Response:**
```json
{
  "status": 200,
  "message": "Cập nhật quyền thành công"
}
```
