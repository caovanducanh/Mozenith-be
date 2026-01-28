## Mobile OAuth2 (Google) — Hướng dẫn nhanh ✅

Mô tả: flow OAuth2 dành cho mobile để lấy token và trả về **deep-link** (ví dụ `bestie://login?token=...&refreshToken=...`).

---

### 1) Endpoint khởi tạo (mobile)
- **Method:** `GET`
- **Path:** `/mobi/oauth2/authorization/{provider}`
- **Auth:** public (Không cần JWT)
- **Mô tả:**
  - Server sẽ đặt cookie `oauth2_mobile=true` (thời hạn 5 phút) và redirect tới `/oauth2/authorization/{provider}?mobile=true` để bắt đầu flow OAuth chuẩn.
  - Ví dụ: `GET https://your-host/mobi/oauth2/authorization/google`

### 2) Callback và kết quả
- Sau khi Google hoàn tất, Spring Security xử lý callback (`/login/oauth2/code/google`) và server:
  - Tạo `token` và `refreshToken` (theo logic hiện tại của app)
  - Nếu phát hiện flow là mobile (session attribute, cookie `oauth2_mobile`, hoặc `state` có `::m`) => **302 redirect** về **deep-link**:
    - `bestie://login?token=<token>&refreshToken=<refreshToken>`
  - Nếu không phải mobile => redirect về web frontend (ví dụ `http://.../login?token=...`)

### 3) Config & ENV
- `application.properties`:
  - `frontend.url.mobile=${FRONTEND_MOBILE_BASE_URL:bestie://login?}`
  - `frontend.url.base=${FRONTEND_BASE_URL:http://localhost:3000}`
- `.env` (example):
  - `FRONTEND_MOBILE_BASE_URL=bestie://login?`

### 4) Debug & Kiểm tra
- Debug endpoint (public): `GET /debug/oauth2/last-redirect` trả JSON: `{ "redirectUrl": ..., "isMobile": true|false }`
- Kiểm tra Location header (nếu không có app để handle deep-link):
  - Mở Developer Tools → Network → hoàn tất flow → xem request tới `/login/oauth2/code/google` → kiểm tra `Response` 302 `Location` header có phải `bestie://...` hay không.
- cURL ví dụ (xem 302 Location):
  ```powershell
  curl -v -L -I "https://your-host/mobi/oauth2/authorization/google"
  ```

### 5) Lưu ý triển khai mobile app
- Android: thêm `intent-filter` cho scheme `bestie` hoặc dùng App Links
- iOS: đăng ký URL scheme hoặc Universal Links
- Bảo mật: token được trả qua URL — app phải xử lý an toàn (xóa URL sau khi lấy token, lưu trữ an toàn)

### 6) Vấn đề thường gặp & cách xử lý
- Nếu `redirectUrl` là `null` hoặc không phải deep-link:
  - Kiểm tra logs, tìm dòng chứa `Detected mobile OAuth via` hoặc `OAuth2 success ... redirecting to ...`
  - Kiểm tra Google console: Redirect URI phải chứa `https://<your-host>/login/oauth2/code/google`
  - Đảm bảo cookies/session được preserve trong webview nếu dùng in-app browser (nếu không, state-based detection đã được thêm)

---

Nếu muốn, tôi có thể bổ sung ví dụ code Android/iOS để xử lý deep-link và nhận token. Muốn tôi thêm vào file này luôn không? 🔧
