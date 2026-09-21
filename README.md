# GeminiFlow Android App (Local Server)

GeminiFlow Android 是一個基於純 Android 原生架構（Kotlin 2.0+ / Jetpack Compose / Ktor CIO 嵌入式伺服器 / OkHttp）的本地 AI 服務端 App。

它直接在 Android 手機本機啟動一個高效、低資源消耗的 HTTP 伺服器（預設為 `http://127.0.0.1:5000`），對內提供文字串流對話（SSE Stream）、對話歷史（Session）、圖片多模態理解與本地圖片生成快取，讓手機上的其他應用程式無需架設外部雲端伺服器即可直接調用 Gemini。

---

## 🌟 核心特點

1. **純 Android 原生新架構**：
   - 採用 **Clean Architecture** 分層（`domain`, `data`, `server`, `service`, `presentation`），完全消除歷史修補包袱。
   - 採用 **Ktor CIO 嵌入式伺服器**，純協程非阻塞，常駐記憶體僅約 25MB~35MB，大幅降低遭系統 LMK（Low Memory Killer）終止的風險。
2. **前景服務 (Foreground Service) 與電池無限制防殺**：
   - 內建常駐前台通知與 `Partial WakeLock`，確保螢幕關閉時伺服器依然持續監聽。
   - 內建一鍵申請系統「電池最佳化無限制 (Unrestricted)」流程。
   - 提供針對小米 (HyperOS/MIUI)、三星 (OneUI)、華為 (HarmonyOS)、OPPO/vivo 等主流機型的自啟動跳轉與防殺指引。
3. **原生 Google 登入整合**：
   - 內建 Material 3 封裝的 Android WebView，支援安全的 Google 帳號登入與雙重驗證。
   - 自動透過 Android `CookieManager` 擷取憑證，並自動向 Gemini 首頁請求解析 `SNlM0e` 與 `FdrFJe` 安全權杖。
4. **本機圖床與靜態資源託管**：
   - 當 Gemini 生成圖片時，App 會自動透過 Google 認證連線下載圖片至私有目錄，並轉換為 `http://127.0.0.1:5000/images/xxx.png` 供其他 App 讀取。
5. **內建對話與生圖測試盒**：
   - 在 App 主儀表板即可直接進行文字提問與圖片生成測試。

---

## 📡 API 端點規範 (127.0.0.1:5000)

### 1. 健康檢查
```http
GET /health
```
**回應範例：**
```json
{"ok": true}
```

### 2. 一般文字對話 (包含 Session)
```http
POST /chat
Content-Type: application/json

{
  "prompt": "記住我的名字是小明",
  "model": "gemini-3-pro",
  "session_id": "session_001"
}
```
**回應範例：**
```json
{
  "text": "你好小明！很高興認識你，請問今天有什麼我可以協助你的嗎？",
  "images": []
}
```

### 3. 文字串流對話 (SSE Stream)
```http
POST /stream
Content-Type: application/json

{
  "prompt": "寫一首關於人工智慧的現代詩",
  "model": "gemini-3-pro"
}
```
**串流事件格式 (Server-Sent Events)：**
```
event: text
data: {"chunk": "晶片在"}

event: text
data: {"chunk": "寂靜中低鳴"}

event: done
data: {}
```

### 4. 圖片生成測試
```http
POST /chat
Content-Type: application/json

{
  "prompt": "畫一隻戴著太陽眼鏡的柯基犬",
  "model": "gemini-3-pro-image-preview"
}
```
**回應範例：**
```json
{
  "text": "這是一隻戴著太陽眼鏡的可愛柯基犬：",
  "images": ["http://127.0.0.1:5000/images/20260921_220500_gemini-3-pro-image-preview_generated.png"]
}
```

### 5. 攜帶圖片提問 (多模態)
在 `images` 陣列傳入 Base64 字串（支援 `data:image/png;base64,...` 或純 Base64 字串）：
```json
{
  "prompt": "請描述這張圖片的內容",
  "model": "gemini-3-pro",
  "images": ["data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ..."]
}
```

---

## 🛠️ 建置與安裝

### 透過終端機建置 Debug APK：
```bash
./gradlew.bat assembleDebug
```
產生的 APK 位於：
```
app/build/outputs/apk/debug/app-debug.apk
```

### 透過 ADB 直接安裝至手機：
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 使用步驟

1. 安裝並開啟 **GeminiFlow** App。
2. 點擊右上角 **帳號圖示**，在內建瀏覽器中登入您的 Google 帳號。
3. 登入完成後點擊「立即儲存 Cookie」，返回主畫面可見 Google 憑證顯示為「已驗證」。
4. 點擊主畫面中的 **電池防殺** 卡片，點擊「一鍵設置為無限制」，依據自身手機廠牌完成自啟動與後台鎖定。
5. 點擊綠色的 **「啟動本地伺服器」** 大按鈕。
6. 通知欄將常駐顯示 `GeminiFlow 服務運行中 (http://127.0.0.1:5000)`，此時您手機上的所有其他 App 皆可自由發送 HTTP 請求！
