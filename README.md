# GeminiFlow Android App

GeminiFlow 是一個基於 Android 原生架構（Kotlin 2.0 / Jetpack Compose / Ktor CIO 嵌入式伺服器 / OkHttp）的本機 AI 服務端與測試沙盒應用程式。

應用程式在本機啟動非阻塞 HTTP 伺服器（預設為 `http://127.0.0.1:5000`），對外提供文字串流對話（SSE Stream）、對話歷史（Session）與本機圖片生成快取，使本機其他應用程式可直接調用 Gemini 模型。

---

## 系統架構設計

本專案採 Clean Architecture 架構分層，各層職責分明：

1. **Presentation Layer (展示層)**：
   - Jetpack Compose 開發。
   - 包含主頁面儀表板、網路流量遙測監控（Server Hub）與模型測試沙盒（Playground）。
   - 以 `MediaAsset` 狀態機驅動圖片渲染，杜絕非同步載入坍縮。
2. **Domain Layer (領域層)**：
   - 核心業務模型與 UseCase（`StreamChatUseCase`、`EnsureAuthUseCase`）。
   - 強型別狀態機定義（`MediaAsset`、`MessageDeliveryState`），消除原始型別偏執（Primitive Obsession）。
3. **Data Layer (資料層)**：
   - `GeminiApiClient`：封裝 Google Batchexecute RPC 串流解析。
   - `GeminiThoughtFilter`：純淨化輸出，過濾 XML 思維鏈、思考標題與內部佔位網址。
   - `WebkitCookieJar`：遵循 RFC 6265，委託 Android 原生 `CookieManager` 實作動態憑證注入。
   - `ImageStorageManager`：專用連線池與原子性本機檔案落地。
4. **Server Layer (本機服務層)**：
   - 基於 Ktor CIO 引擎之嵌入式 HTTP 伺服器，常駐記憶體約 25MB~35MB。
   - 支援 CORS、ContentNegotiation 與靜態圖片路由（`/images/{filename}`）。
5. **Service Layer (系統服務層)**：
   - 前景服務（Foreground Service）綁定 Partial WakeLock，確保背景待機時伺服器持續監聽。

---

## Google CDN 圖片下載機制與踩坑技術復盤

### 1. 核心驗證鏈路：Google CDN 三跳轉機制 (3-Hop Redirect Chain)

Google Gemini 所產生的圖片資源（`lh3.googleusercontent.com`）具有嚴格的跨網域安全防護機制，其完整交付流程為 3 次 HTTP 跳轉：

```
[Hop 0] https://lh3.googleusercontent.com/gg-dl/...
        │
        │ 條件：嚴禁攜帶 .google.com Cookie
        ▼
   HTTP 302 Found (轉向至認證跳板)
        │
[Hop 1] https://work.fife.usercontent.google.com/rd-gg-dl/...
        │
        │ 條件：必須注入有效 Google Session Cookie (__Secure-1PSID 等)
        ▼
   HTTP 302 Found (驗證通過，轉向至實體檔案)
        │
[Hop 2] https://lh3.googleusercontent.com/rd-gg-dl/...=s512
        │
        ▼
   HTTP 200 OK (交付 Content-Type: image/jpeg 二進位串流)
```

### 2. 歷史問題點與失敗嘗試 (Pitfalls)

#### 坑點一：Hop 0 跨域憑證洩漏導致 HTTP 400
* **現象**：發送圖片下載請求時，伺服器立即回傳 `HTTP 400 Bad Request`。
* **原因**：舊實作以字串拼接方式直接在請求 Header 加入帳號 Cookie。`lh3.googleusercontent.com` 作為公開沙盒入口，一旦在 Hop 0 接收到包含 `.google.com` 的 Session Cookie，Google 邊緣防火牆會直接判定為跨域憑證外洩並中斷請求。
* **防範原則**：Hop 0 必須維持純淨請求，不可帶入 `.google.com` 帳號 Cookie。

#### 坑點二：Hop 1 遺漏認證憑證導致 HTTP 403
* **現象**：跳轉至 `work.fife.usercontent.google.com` 時回傳 `HTTP 403 Forbidden`。
* **原因**：若客戶端完全不帶 Cookie，或重定向時未自動識別 `.google.com` 主網域，認證跳板無法驗證呼叫者身份。
* **防範原則**：轉向 Hop 1 時，客戶端必須根據目標網域自動注入本機登入憑證。

#### 坑點三：WebView 沙盒 Canvas/Fetch 跨域限制 (CORS Null Origin)
* **現象**：嘗試透過隱藏 WebView 執行 JavaScript `fetch()` 或繪製 Canvas 轉 Base64 失敗。
* **原因**：WebView 載入 `about:blank` 時，其安全來源（Origin）為 `null`。Chromium 沙盒環境嚴格阻擋 `null` 來源對 Google CDN 資源發起帶憑證的請求，導致執行時期拋出 CORS 例外。
* **防範原則**：不可依賴 WebView 前端環境繞道下載，必須在原生網路層完成下載。

#### 坑點四：下載失敗降級外洩與 UI 高度坍縮
* **現象**：生成圖片時，助理對話框高度為 0 或呈現完全空白的卡片，且無任何錯誤提示。
* **原因**：
  1. 舊系統使用 `List<String>` 混雜本地路徑與遠端網址。當本機下載失敗時，程式邏輯私自將遠端 CDN 網址推入列表作為備援。
  2. Coil 圖片庫嘗試載入遠端網址時，因無 Hop 1 跳轉憑證而載入失敗，圖片高度保持為 0。
  3. 與此同時，思考過程過濾器已將模型輸出的思考文字清除，導致整個氣泡既無文字也無有效圖片，最終坍縮為空白。
* **防範原則**：未下載完成之遠端 CDN 網址絕不可傳遞至 UI 層；任何下載異常必須轉化為明確的失敗狀態，禁止靜默吞噬。

#### 坑點五：修改已簽章網址參數導致簽章失效
* **現象**：對 `gg-dl` 圖片網址追加 `=s0-d` 或修改 Query Parameter，導致 HTTP 400/404。
* **原因**：`gg-dl` 路徑中的 Token 已內嵌 Google 的加密雜湊校驗碼，字串的任何變更都會導致伺服器端簽章校驗失敗。
* **防範原則**：Google CDN 已簽章網址必須原樣發送，禁止擅自拼接或修改後綴。

### 3. 架構解決方案

1. **RFC 6265 規範之 `WebkitCookieJar`**：
   - 實作 OkHttp `CookieJar` 介面，委託 Android 原生 `CookieManager.getInstance().getCookie(url)`。
   - 對於 Hop 0（`googleusercontent.com`），`CookieManager` 自動回傳空值，請求不帶 Cookie，順利獲取 302。
   - 轉向 Hop 1（`work.fife.usercontent.google.com`，符合 `.google.com`），`CookieManager` 自動匹配並注入登入憑證，通過認證。
2. **本機原子化檔案儲存 (`ImageStorageManager`)**：
   - 採用專用連線池與自定義標頭（模擬瀏覽器 Sec-Fetch 策略）。
   - 下載時先寫入 `.tmp` 暫存檔，確認寫入完成且位元組大於 0 後，再以原子操作 `renameTo` 目標檔案，避免 UI 讀取到半殘檔案。
3. **強型別領域狀態機 (`MediaAsset`)**：
   - 定義 `LocalReady`、`Downloading`、`Failed` 三種狀態。
   - UI 僅對 `LocalReady` 本機實體檔案調用 Coil 載入，100% 免疫網路波動與 Cookie 問題。
   - 下載中呈現進度指示，下載失敗呈現錯誤診斷資訊與重試按鈕。

---

## 本機 API 端點規範 (127.0.0.1:5000)

### 1. 系統健康檢查
```http
GET /health
```
回應：
```json
{
  "ok": true
}
```

### 2. 一般文字對話與會話維持
```http
POST /chat
Content-Type: application/json

{
  "prompt": "你好，請介紹你自己",
  "model": "gemini-3-pro",
  "session_id": "session_default"
}
```
回應：
```json
{
  "text": "你好！我是 Gemini...",
  "images": []
}
```

### 3. 文字串流對話 (SSE Stream)
```http
POST /stream
Content-Type: application/json

{
  "prompt": "請寫一首現代詩",
  "model": "gemini-3-pro"
}
```
串流事件格式：
```text
event: text
data: {"chunk":"微風"}

event: text
data: {"chunk":"輕拂街角"}

event: done
data: {}
```

### 4. 圖片生成
```http
POST /chat
Content-Type: application/json

{
  "prompt": "繪製一隻在草地上奔跑的柴犬",
  "model": "gemini-3-pro"
}
```
回應：
```json
{
  "text": "這是為您生成的柴犬圖片：",
  "images": [
    "http://127.0.0.1:5000/images/20260926_190000_gemini-3-pro_generated.png"
  ]
}
```

### 5. 靜態圖片存取
```http
GET /images/{filename}
```
回應對應二進位圖片資料（`Content-Type: image/png`）。

---

## 建置、測試與安裝

### 執行單元測試
```bash
./gradlew.bat testDebugUnitTest
```
包含以下測試驗證：
- `WebkitCookieJarTest`：驗證 Hop 0 與 Hop 1 網域 Cookie 隔離與注入。
- `ImageStorageManagerTest`：驗證 Base64 行內解碼與 HTTP 原子性下載。
- `TrafficLoggingInterceptorTest`：驗證 RFC 7230 記憶體遙測標籤相容性。
- `GeminiStreamParserTest`：驗證思考過程與思維鏈過濾。

### 編譯 Debug APK
```bash
./gradlew.bat assembleDebug
```
產出檔案位於：
```text
app/build/outputs/apk/debug/app-debug.apk
```

### 安裝至實體設備
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 使用說明

1. 開啟 GeminiFlow App。
2. 進入帳號驗證頁面，透過內建 WebView 登入 Google 帳號。
3. 登入成功後點擊儲存憑證，確認 Google 憑證狀態為「已驗證」。
4. 依系統提示設定電池最佳化為「無限制」，以防止後台被系統終止。
5. 點擊「啟動本地伺服器」，狀態列即顯示伺服器常駐通知。
6. 本機其他客戶端應用程式或腳本即可向 `http://127.0.0.1:5000` 發送 API 請求。
