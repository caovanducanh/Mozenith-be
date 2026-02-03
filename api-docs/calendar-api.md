# Calendar API — Hướng dẫn chi tiết (VN)

Mô tả: tài liệu này tổng hợp cách sử dụng các endpoint liên quan tới calendar trong backend (Google Calendar integration). Tập trung vào: endpoints, body/params, header/auth, permission, ví dụ curl và lưu ý vận hành.

---

## Tổng quan
- Base path: `POST/GET/PUT/DELETE /api/calendar/events`
- Provider hiện tại: **Google Calendar** (service gọi Google APIs dưới dạng `primary` calendar nếu không truyền `calendarId`).
- Lưu credential Google (access + refresh token) trong bảng `calendar_credential` khi user hoàn tất OAuth flow.
- Token Google được refresh tự động (server dùng `refresh_token` + client id/secret).

## Xác thực & Phân quyền
- Yêu cầu header: `Authorization: Bearer <JWT_ACCESS_TOKEN>` (JWT của app).
- Mỗi endpoint được bảo vệ bằng annotation `@SecuredEndpoint` và sẽ kiểm tra permission trong JWT (qua role → permissions):
  - GET `/api/calendar/events` → **`CALENDAR_READ`**
  - POST `/api/calendar/events` → **`CALENDAR_CREATE`**
  - PUT `/api/calendar/events/{eventId}` → **`CALENDAR_UPDATE`**
  - DELETE `/api/calendar/events/{eventId}` → **`CALENDAR_DELETE`**

Nếu JWT thiếu permission => 403 Forbidden.
Nếu không có JWT hoặc token invalid/expired => 401 Unauthorized.

---

## Endpoints

### 1) Lấy events
- Method: `GET`
- Path: `/api/calendar/events`
- Query params (tùy chọn):
  - `calendarId` (string) — id calendar, mặc định `primary` nếu không có
  - `timeMin` (ISO-8601 instant, ví dụ `2026-01-29T00:00:00Z`) — lọc từ thời điểm này
  - `timeMax` (ISO-8601 instant) — lọc đến thời điểm này
- Permission: `CALENDAR_READ`
- Response: body là Map JSON (dịch nguyên từ Google Calendar API `events` response)
 - Errors:
   - If user has not linked Google account, service will return HTTP 400 with JSON error message `Google account not linked`.

Ví dụ curl:
```powershell
curl -H "Authorization: Bearer <JWT>" "https://<host>/api/calendar/events?timeMin=2026-01-01T00:00:00Z&timeMax=2026-01-31T23:59:59Z"
```

Sample response (rút gọn):
```json
{
  "kind": "calendar#events",
  "items": [ { "id": "evt1", "summary": "Meeting", "start": {"dateTime":"2026-01-29T10:00:00+07:00"}, "end": {"dateTime":"2026-01-29T11:00:00+07:00"} } ],
  "nextPageToken": "..."
}
```

---

### Credential status (new)
- Method: `GET`
- Path: `/api/calendar/credential`
- Permission: `CALENDAR_READ` (requires authenticated user)
- Response:
  - If linked: HTTP 200, body `{ "linked": true, "scopes": "<space-separated-scopes>", "expiresAt": "<ISO instant>" }`
  - If not linked: HTTP 200, body `{ "linked": false }`

Note: We standardized the missing-credential error for other endpoints (create/get/update/delete) to return **HTTP 400** with message `Google account not linked` instead of a generic 500.

### Credential token (new)
- Method: `GET`
- Path: `/api/calendar/credential/token`
- Permission: `CALENDAR_READ`
  - Note: `CALENDAR_READ` is granted to `MEMBER` role by default so members can check linking status and request a token.
- Response:
  - If linked: HTTP 200, body `{ "accessToken": "<token>", "expiresAt": "<ISO instant>" }` (server will refresh token if needed before returning)
  - If not linked: HTTP 400 with `{ "status": 400, "message": "Google account not linked", "data": null }`

⚠️ Warning: The access token is sensitive. Prefer allowing the server to perform Google API calls on behalf of the client instead of returning raw tokens to mobile clients. If you must return tokens, ensure the client is trusted and audit the issuance.

### 2) Tạo event
- Method: `POST`
- Path: `/api/calendar/events`
- Query params (tùy chọn): `calendarId` (mặc định `primary`)
- Permission: `CALENDAR_CREATE`
- Request body (JSON) -> `CalendarEventRequest`:
  - `summary` (string) — tiêu đề
  - `description` (string)
  - `startDateTime` (ISO-8601 with offset) — ví dụ: `2026-01-29T10:00:00+07:00`
  - `endDateTime` (ISO-8601 with offset)
  - `timeZone` (string) — ví dụ `Asia/Ho_Chi_Minh` (không bắt buộc, nếu bỏ sẽ dùng dateTime string có offset)
  - `location` (string)
- Response: HTTP 201 Created, body là Map JSON trả về từ Google (ví dụ có `id` của event)

Ví dụ curl:
```powershell
curl -X POST "https://<host>/api/calendar/events" -H "Authorization: Bearer <JWT>" -H "Content-Type: application/json" -d '{"summary":"Gặp team","description":"Discuss","startDateTime":"2026-01-29T10:00:00+07:00","endDateTime":"2026-01-29T11:00:00+07:00","timeZone":"Asia/Ho_Chi_Minh" }'
```

Sample response body (rút gọn):
```json
{ "id": "abcdef12345", "summary": "Gặp team", "start": {"dateTime":"2026-01-29T10:00:00+07:00"}, "end": {"dateTime":"2026-01-29T11:00:00+07:00"} }
```

---

### 3) Cập nhật event
- Method: `PUT`
- Path: `/api/calendar/events/{eventId}`
- Query params (tùy chọn): `calendarId`
- Permission: `CALENDAR_UPDATE`
- Request body: giống `CalendarEventRequest` (cập các trường cần thay đổi)
- Response: HTTP 200 OK, body là Map JSON trả về từ Google (updated event)

Ví dụ:
```powershell
curl -X PUT "https://<host>/api/calendar/events/abcdef12345" -H "Authorization: Bearer <JWT>" -H "Content-Type: application/json" -d '{"summary":"New summary"}'
```

---

### 4) Xóa event
- Method: `DELETE`
- Path: `/api/calendar/events/{eventId}`
- Query params (tùy chọn): `calendarId`
- Permission: `CALENDAR_DELETE`
- Response: HTTP 204 No Content

Ví dụ:
```powershell
curl -X DELETE "https://<host>/api/calendar/events/abcdef12345" -H "Authorization: Bearer <JWT>"
```

---

## Lưu credential & OAuth (liên kết Google Calendar)

### ⚠️ QUAN TRỌNG: Phân biệt Login và Calendar Authorization

**Google Login ≠ Google Calendar Authorization**

| Loại | Mục đích | Scopes | Refresh Token? |
|------|----------|--------|----------------|
| Google Sign-In | Xác thực user (identity) | openid, profile, email | Không bắt buộc |
| Google Calendar OAuth | Xin quyền truy cập Calendar API | calendar, calendar.events | **BẮT BUỘC** |

👉 Đây là 2 flow riêng biệt với endpoint riêng biệt.

---

### Flow mới (v2) - Dedicated Calendar OAuth

Flow này tách biệt hoàn toàn khỏi Google Login, đảm bảo:
- ✅ Luôn có calendar scope
- ✅ Luôn có refresh_token (với `access_type=offline` + `prompt=consent`)
- ✅ Token lưu riêng trong bảng `calendar_credential`

#### Endpoints mới

| Method | Path | Mô tả | Auth Required? |
|--------|------|-------|----------------|
| GET | `/oauth2/google/calendar/authorize` | Khởi tạo OAuth Calendar (web) | ✅ JWT Bearer |
| GET | `/oauth2/google/calendar/authorize/mobile?token=<jwt>` | Khởi tạo OAuth Calendar (mobile) | JWT via query |
| GET | `/oauth2/google/calendar/callback` | Callback từ Google | ❌ (validate via state) |
| GET | `/oauth2/google/calendar/status` | Kiểm tra trạng thái liên kết | ✅ JWT Bearer |
| GET | `/oauth2/google/calendar/unlink` | Hủy liên kết Calendar | ✅ JWT Bearer |

#### Flow chi tiết cho Mobile:

```
[MOBILE APP]
   |
   | 1. User đã login (có JWT token)
   |
   | 2. User bấm "Liên kết Google Calendar"
   v
GET /oauth2/google/calendar/authorize/mobile?token=<jwt_token>
   |
   | Backend validates JWT, extracts userId
   | Generates state, stores in memory
   v
[REDIRECT TO GOOGLE CONSENT SCREEN]
   |
   | Google shows calendar permission request
   | User grants/denies access
   v
GET /oauth2/google/calendar/callback?code=xxx&state=xxx
   |
   | Backend validates state
   | Exchanges code for tokens
   | Validates calendar scope exists
   | Saves to calendar_credential table
   v
[REDIRECT TO MOBILE DEEP-LINK]
bestie://calendar?linked=true
   or
bestie://calendar?linked=false&error=<message>
```

#### Ví dụ sử dụng (Mobile Kotlin):

```kotlin
// Get JWT token from local storage
val jwtToken = authRepository.getToken()
if (jwtToken.isNullOrEmpty()) {
    // User needs to login first
    return
}

// Build URL with token
val encodedToken = java.net.URLEncoder.encode(jwtToken, "UTF-8")
val url = "${BuildConfig.OAUTH_BASE_URL}/oauth2/google/calendar/authorize/mobile?token=$encodedToken"

// Open in CustomTabs/WebView
val customTabsIntent = CustomTabsIntent.Builder().build()
customTabsIntent.launchUrl(context, Uri.parse(url))
```

#### Ví dụ kiểm tra trạng thái liên kết:

```bash
curl -H "Authorization: Bearer <JWT>" "https://<host>/oauth2/google/calendar/status"
```

Response nếu đã liên kết:
```json
{
  "linked": true,
  "email": "user@example.com",
  "scopes": "https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/calendar.events",
  "hasCalendarScope": true,
  "hasRefreshToken": true,
  "expiresAt": "2026-02-03T02:30:00Z"
}
```

Response nếu chưa liên kết:
```json
{
  "linked": false,
  "message": "Google Calendar not linked. Use /oauth2/google/calendar/authorize to link."
}
```

---

### Flow cũ (deprecated) - Không nên dùng

> ⚠️ Flow cũ `/mobi/oauth2/authorization/google/calendar` vẫn hoạt động nhưng không khuyến khích.
> Lý do: nó phụ thuộc vào session/cookies có thể không reliable trên mobile WebViews.

- App hỗ trợ **mobile OAuth** (đọc `api-docs/mobile-oauth.md`).
- Flow mobile cũ:
  1. Client mở `GET /mobi/oauth2/authorization/google/calendar` → server set cookie và redirect tới Google
  2. Google redirect về `/login/oauth2/code/google` → success handler lưu credential

---

## Refresh token & vận hành
- Service tự refresh access token khi `expiresAt` đã qua (tối thiểu check `expiresAt.isBefore(now - 60s)`).
- Cần có config env:
  - `spring.security.oauth2.client.registration.google.client-id`
  - `spring.security.oauth2.client.registration.google.client-secret`
- Nếu refresh token thất bại, service log warning và sẽ giữ credential hiện có; behavior cần theo dõi logs để xử lý.

## Lỗi thường gặp & cách debug
- 401 Unauthorized: JWT bị thiếu/expired → kiểm tra header `Authorization`
- 403 Forbidden: thiếu permission (check roles/permissions của user)
- 500 Internal Server Error: thường do không có Google credential hoặc lỗi khi gọi Google API. Kiểm tra logs server cho message `No Google credential linked` hoặc `Refreshing Google access token for user=...`
- Debug OAuth mobile: dùng `GET /debug/oauth2/last-redirect` (áp dụng nếu server bật debug endpoints) để xem redirect URL

---

## Quick checklist khi triển khai client (mobile/web)
- [ ] Đăng nhập vào app, lấy JWT access token
- [ ] Nếu muốn thao tác calendar trên Google: trigger OAuth mobile flow `GET /mobi/oauth2/authorization/google` hoặc web OAuth
- [ ] Sau khi OAuth xong, server đã lưu credential (kiểm tra logs hoặc gọi API debug)
- [ ] Gọi `/api/calendar/events` với header `Authorization: Bearer <JWT>` và đúng permission

---

## Gợi ý mở rộng (noted for backend maintainers)
- Trả lỗi rõ hơn khi chưa liên kết Google (không dùng generic 500), ví dụ 400 với message `Google account not linked` hoặc 404
- Thêm endpoint `/api/calendar/credential` để client kiểm tra trạng thái linking (exists? scopes?)
- Tắt spread of raw Google error to clients; chuẩn hóa schema error

---

Nếu bạn muốn, tôi có thể: 
- Thêm ví dụ Postman collection / OpenAPI snippet dựa trên controller hiện tại, hoặc
- Tạo endpoint debug `/api/calendar/credential` để kiểm tra trạng thái liên kết. Muốn tôi làm tiếp không? ✅
