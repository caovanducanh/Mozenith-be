# Tài khoản mặc định

Hệ thống khởi tạo sẵn 2 tài khoản mẫu:

- **Admin**
  - username: `admin`
  - password: `admin123`
  - role: `ADMIN`
- **Member**
  - username: `member`
  - password: `member123`
  - role: `MEMBER`

Bạn có thể dùng các tài khoản này để đăng nhập và test các API phân quyền.

# Authentication API

Các API xác thực, đăng nhập, đăng ký, làm mới token, đăng nhập Google/Facebook, OAuth2.

## 1. Đăng ký tài khoản
- **Endpoint:** `POST /api/register`
- **Request body:**
```json
{
  "username": "string",
  "password": "string",
  "confirmPassword": "string",
  "fullName": "string",
  "dateOfBirth": "yyyy-MM-dd",
  "gender": "MALE | FEMALE | OTHER",
  "email": "string",
  "identityCard": "05088997658",
  "phone": "+84987654321",
  "address": "string"
}
```
- **Response:**
```json
{
  "statusCode": 200,
  "message": "Đăng ký thành công",
  "data": {
    "userId": 1,
    "username": "string",
    "fullName": "string",
    "email": "string",
    "phone": "+84987654321",
    "address": "string",
    "dateOfBirth": "yyyy-MM-dd",
    "identityCard": "05088997658",
    "gender": "MALE | FEMALE | OTHER",
    "status": "ACTIVE",
    "createdDate": "2025-08-20T10:00:00",
    "token": "jwt-access-token",
    "refreshToken": "jwt-refresh-token"
  }
}
```

## 2. Đăng nhập
- **Endpoint:** `POST /api/login`
- **Request:**
```json
{
  "username": "string",
  "password": "string"
}
```
- **Response:**
```json
{
  "status": 200,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "...",
    "refreshToken": "..."
  }
}
```

## 3. Làm mới token
- **Endpoint:** `POST /api/refresh-token`
- **Request:**
```json
{
  "refreshToken": "string"
}
```
- **Response:**
```json
{
  "status": 200,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "...",
    "refreshToken": "..."
  }
}
```

## 4. Đăng nhập Google
- **Endpoint:** `POST /api/google-login`
- **Request:**
```json
{
  "token": "google_id_token"
}
```
- **Response:**
```json
{
  "status": 200,
  "message": "Google login successful",
  "data": { ... }
}
```

## 5. OAuth2 login success
- **Endpoint:** `GET /api/oauth2/success`
- **Response:**
```json
{
  "status": 200,
  "message": "OAuth2 login successful",
  "data": { ... }
}
```

## 6. OAuth2 login failure
- **Endpoint:** `GET /api/oauth2/failure`
- **Response:**
```json
{
  "status": 401,
  "message": "OAuth2 login failed"
}
```

## 7. Đăng nhập Facebook
- **Endpoint:** `POST /api/facebook-login`
- **Request:**
```json
{
  "token": "facebook_access_token"
}
```
- **Response:**
```json
{
  "status": 200,
  "message": "Facebook login successful",
  "data": { ... }
}
```
