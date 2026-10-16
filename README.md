# GeminiFlow 本機服務對接指南

GeminiFlow 在 Android 裝置本機運行嵌入式 Ktor HTTP 伺服器，預設監聽端口為 `http://127.0.0.1:5000`（亦可透過區域網路 IP 連線）。本機其他應用程式、前端網頁或腳本可透過標準 HTTP/SSE 請求直接對接 Gemini 最新官方協議模型。

---

## 伺服器端口與連線資訊

* **基礎位址 (Base URL)**：`http://127.0.0.1:5000`（或 `http://<裝置區域網路IP>:5000`）
* **跨域資源共享 (CORS)**：已全域啟用，支援任何 Origin 跨域呼叫（允許 GET, POST, OPTIONS 方法及 Content-Type 標頭）。
* **請求格式**：`Content-Type: application/json; charset=utf-8`
* **支援模型 (model)**：
  * `gemini-3.8-flash`：極速回應，適合一般問答、資訊萃取與日常編程。
  * `gemini-3-pro`：旗艦推理，支援高難度邏輯分析、多模態圖文理解與原生圖像生成。
* **思考強度 (thinking)**：
  * `low`：等級 1，極速回應（預設值）。
  * `medium`：等級 4，深度推理，平衡思考時間與輸出品質。
  * `high`：等級 2，極致推理，複雜問題深度思維鏈。

---

## API 端點一覽

| HTTP 方法 | 端點路徑 | 功能說明 | 回應格式 |
| :--- | :--- | :--- | :--- |
| `GET` | `/health` | 檢查本機伺服器運行狀態 | `application/json` |
| `POST` | `/chat` | 一般文字對話、多模態圖文對話與生圖 (非串流) | `application/json` |
| `POST` | `/stream` | 即時打字機串流對話 (SSE Stream) | `text/event-stream` |
| `GET` | `/images/{filename}` | 讀取本機快取的生成圖片實體二進位檔案 | `image/png` 或 `image/jpeg` |

---

## 請求參數規格 (`POST /chat` 與 `POST /stream`)

兩個對話端點共用相同的 JSON 請求體結構：

| 欄位名稱 | 型別 | 必填/選填 | 預設值 | 說明 |
| :--- | :--- | :--- | :--- | :--- |
| `prompt` | String | 必填 | 無 | 輸入之提示詞或任務內容。 |
| `model` | String | 必填 | 無 | 指定調用之模型：`gemini-3.8-flash` 或 `gemini-3-pro`。 |
| `thinking` | String | 選填 | `low` | 思考模式強度：`low`、`medium`、`high`。 |
| `system_prompt` | String | 選填 | `null` | 系統人設指令與全域角色約束。 |
| `session_id` | String | 選填 | `null` | 會話識別碼。傳入相同值即可自動串接多輪上下文記憶。 |
| `images` | List<String> | 選填 | `[]` | 傳入圖片列表，支援純 Base64 或 Data URI 字串。 |
| `language` | String | 選填 | `zh-TW` | 回應偏好語系（如 `zh-TW`, `en-US`, `ja-JP`）。 |

---

## 常用組合範例

### 1. 極速日常問答 (Flash + 預設低思考)
適用場景：一般知識查詢、快速摘要、文字潤飾。

**請求：**
```bash
curl -X POST http://127.0.0.1:5000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemini-3.8-flash",
    "prompt": "請用繁體中文列出太陽系前五大行星。"
  }'
```
**回應 (HTTP 200)：**
```json
{
  "text": "太陽系體積最大的前五大行星依序為：\n1. 木星 (Jupiter)\n2. 土星 (Saturn)\n3. 天王星 (Uranus)\n4. 海王星 (Neptune)\n5. 地球 (Earth)",
  "images": []
}
```

---

### 2. 深度邏輯推理與代碼除錯 (Flash/Pro + 高思考)
適用場景：演算法解題、架構重構、複雜邏輯論證。

**請求：**
```bash
curl -X POST http://127.0.0.1:5000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemini-3.8-flash",
    "thinking": "high",
    "prompt": "請分析快速排序法在最差情況下的時間複雜度，並說明如何避免退化至 O(n^2)。"
  }'
```
**回應 (HTTP 200)：**
```json
{
  "text": "快速排序法 (Quick Sort) 在最差情況下的時間複雜度為 O(n^2)...（省略深度解析）",
  "images": []
}
```

---

### 3. 指定系統人設與格式約束 (System Prompt)
適用場景：約束輸出必須為純 JSON、角色扮演、API 資料清洗。

**請求：**
```bash
curl -X POST http://127.0.0.1:5000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemini-3.8-flash",
    "system_prompt": "你是一個專業的資料清洗程式。請只輸出符合 JSON 陣列格式的結果，不要帶任何 Markdown 或解說。",
    "prompt": "台北, 台中, 高雄, 台南"
  }'
```
**回應 (HTTP 200)：**
```json
{
  "text": "[\"台北\", \"台中\", \"高雄\", \"台南\"]",
  "images": []
}
```

---

### 4. 連續多輪對話上下文維持 (Session Tracking)
適用場景：聊天機器人、步驟引導式任務。只要攜帶相同的 `session_id`，模型即具備前文記憶。

**第一輪請求：**
```bash
curl -X POST http://127.0.0.1:5000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemini-3.8-flash",
    "session_id": "user_session_1001",
    "prompt": "我的幸運數字是 7，請記住它。"
  }'
```

**第二輪延續請求：**
```bash
curl -X POST http://127.0.0.1:5000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemini-3.8-flash",
    "session_id": "user_session_1001",
    "prompt": "我剛才說我的幸運數字是多少？"
  }'
```
**第二輪回應 (HTTP 200)：**
```json
{
  "text": "您剛才說您的幸運數字是 7。",
  "images": []
}
```

---

### 5. 圖像輸入多模態分析 (Multimodal Vision)
適用場景：看圖辨物、圖表數據分析、光學文字辨識 (OCR)。將圖片轉為 Base64 字串傳入 `images` 陣列。

**請求：**
```bash
curl -X POST http://127.0.0.1:5000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemini-3-pro",
    "prompt": "這張圖片裡有什麼？請詳細描述細節。",
    "images": [
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
    ]
  }'
```
**回應 (HTTP 200)：**
```json
{
  "text": "這是一張尺寸為 1x1 像素的純紅色影像...",
  "images": []
}
```

---

### 6. 旗艦繪圖創作與本機快取 (Pro Image Generation)
適用場景：依提示詞生成圖像。系統會自動完成 Google 內部三跳轉鑑權下載並快取至本機，直接回傳可存取的本機 HTTP 圖片網址。

**請求：**
```bash
curl -X POST http://127.0.0.1:5000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemini-3-pro",
    "prompt": "請畫一隻戴著護目鏡在火星漫步的柴犬，賽博朋克風格"
  }'
```
**回應 (HTTP 200)：**
```json
{
  "text": "這是為您生成的柴犬圖片：",
  "images": [
    "http://127.0.0.1:5000/images/20261014_141000_gemini-3-pro_generated.png"
  ]
}
```
**客戶端取得實體圖片：**
```bash
curl -O http://127.0.0.1:5000/images/20261014_141000_gemini-3-pro_generated.png
```

---

### 7. SSE 即時打字機串流對話 (`POST /stream`)
適用場景：降低首字延遲、前端即時打字機文字呈現。

**請求：**
```bash
curl -N -X POST http://127.0.0.1:5000/stream \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemini-3.8-flash",
    "thinking": "low",
    "prompt": "請用繁體中文寫一首關於秋天的三行俳句。"
  }'
```
**串流事件輸出 (text/event-stream)：**
```text
event: text
data: {"chunk":"落葉飄幽徑，"}

event: text
data: {"chunk":"秋風拂過晚霞紅，"}

event: text
data: {"chunk":"寒蟬咽殘更。"}

event: done
data: {}
```

---

## HTTP 狀態碼與錯誤處理

| HTTP 狀態碼 | 錯誤原因 | 解決方式 |
| :--- | :--- | :--- |
| `200 OK` | 請求成功完成 | 正常解析 JSON 或串流事件。 |
| `400 Bad Request` | 缺少必要欄位（例如未提供 `model` 或 `prompt`） | 檢查請求體 JSON，確保包含 `model` 與 `prompt`。 |
| `401 Unauthorized` | Google 帳號憑證過期或未完成登入授權 | 開啟手機 GeminiFlow App 重新點擊 Google 登入並儲存憑證。 |
| `422 Unprocessable Entity` | 傳入無效的 Base64 圖片資料或參數損毀 | 確認 `images` 陣列中的字串為有效 Base64 格式。 |
| `500 Internal Server Error`| 伺服器內部非預期例外 | 查看手機端「日誌紀錄」獲取具體 StackTrace。 |
| `502 Bad Gateway` | 本機與 Google 端點網路連線失敗或超時 | 檢查裝置網路連線是否暢通。 |

---

## 開發與測試建置命令

* 執行全套單元測試：
  ```bash
  ./gradlew.bat testDebugUnitTest
  ```
* 編譯 Debug APK：
  ```bash
  ./gradlew.bat assembleDebug
  ```
* 透過 ADB 安裝至手機：
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

---

## 相關技術文檔

* [Google CDN 圖片三跳轉下載機制與踩坑技術復盤](docs/GOOGLE_CDN_IMAGE_ARCHITECTURE.md)