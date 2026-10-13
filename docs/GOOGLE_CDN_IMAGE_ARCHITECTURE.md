# Google CDN 圖片三跳轉下載機制與踩坑技術復盤

## 1. 核心驗證鏈路：Google CDN 三跳轉機制 (3-Hop Redirect Chain)

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

---

## 2. 歷史問題點與失敗嘗試 (Pitfalls)

### 坑點一：Hop 0 跨域憑證洩漏導致 HTTP 400
* **現象**：發送圖片下載請求時，伺服器立即回傳 `HTTP 400 Bad Request`。
* **原因**：舊實作以字串拼接方式直接在請求 Header 加入帳號 Cookie。`lh3.googleusercontent.com` 作為公開沙盒入口，一旦在 Hop 0 接收到包含 `.google.com` 的 Session Cookie，Google 邊緣防火牆會直接判定為跨域憑證外洩並中斷請求。
* **防範原則**：Hop 0 必須維持純淨請求，不可帶入 `.google.com` 帳號 Cookie。

### 坑點二：Hop 1 遺漏認證憑證導致 HTTP 403
* **現象**：跳轉至 `work.fife.usercontent.google.com` 時回傳 `HTTP 403 Forbidden`。
* **原因**：若客戶端完全不帶 Cookie，或重定向時未自動識別 `.google.com` 主網域，認證跳板無法驗證呼叫者身份。
* **防範原則**：轉向 Hop 1 時，客戶端必須根據目標網域自動注入本機登入憑證。

### 坑點三：WebView 沙盒 Canvas/Fetch 跨域限制 (CORS Null Origin)
* **現象**：嘗試透過隱藏 WebView 執行 JavaScript `fetch()` 或繪製 Canvas 轉 Base64 失敗。
* **原因**：WebView 載入 `about:blank` 時，其安全來源（Origin）為 `null`。Chromium 沙盒環境嚴格阻擋 `null` 來源對 Google CDN 資源發起帶憑證的請求，導致執行時期拋出 CORS 例外。
* **防範原則**：不可依賴 WebView 前端環境繞道下載，必須在原生網路層完成下載。

### 坑點四：下載失敗降級外洩與 UI 高度坍縮
* **現象**：生成圖片時，助理對話框高度為 0 或呈現完全空白的卡片，且無任何錯誤提示。
* **原因**：
  1. 舊系統使用 `List<String>` 混雜本地路徑與遠端網址。當本機下載失敗時，程式邏輯私自將遠端 CDN 網址推入列表作為備援。
  2. Coil 圖片庫嘗試載入遠端網址時，因無 Hop 1 跳轉憑證而載入失敗，圖片高度保持為 0。
  3. 與此同時，思考過程過濾器已將模型輸出的思考文字清除，導致整個氣泡既無文字也無有效圖片，最終坍縮為空白。
* **防範原則**：未下載完成之遠端 CDN 網址絕不可傳遞至 UI 層；任何下載異常必須轉化為明確的失敗狀態，禁止靜默吞噬。

### 坑點五：修改已簽章網址參數導致簽章失效
* **現象**：對 `gg-dl` 圖片網址追加 `=s0-d` 或修改 Query Parameter，導致 HTTP 400/404。
* **原因**：`gg-dl` 路徑中的 Token 已內嵌 Google 的加密雜湊校驗碼，字串的任何變更都會導致伺服器端簽章校驗失敗。
* **防範原則**：Google CDN 已簽章網址必須原樣發送，禁止擅自拼接或修改後綴。

---

## 3. 架構解決方案

### 1. RFC 6265 規範之 WebkitCookieJar
* 實作 OkHttp `CookieJar` 介面，委託 Android 原生 `CookieManager.getInstance().getCookie(url)`。
* 對於 Hop 0（`googleusercontent.com`），`CookieManager` 自動回傳空值，請求不帶 Cookie，順利獲取 302。
* 轉向 Hop 1（`work.fife.usercontent.google.com`，符合 `.google.com`），`CookieManager` 自動匹配並注入登入憑證，通過認證。

### 2. 本機原子化檔案儲存 (ImageStorageManager)
* 採用專用連線池與自定義標頭（模擬瀏覽器 Sec-Fetch 策略）。
* 下載時先寫入 `.tmp` 暫存檔，確認寫入完成且位元組大於 0 後，再以原子操作 `renameTo` 目標檔案，避免 UI 讀取到半殘檔案。

### 3. 強型別領域狀態機 (MediaAsset)
* 定義 `LocalReady`、`Downloading`、`Failed` 三種狀態。
* UI 僅對 `LocalReady` 本機實體檔案調用 Coil 載入，100% 免疫網路波動與 Cookie 問題。
* 下載中呈現進度指示，下載失敗呈現錯誤診斷資訊與重試按鈕。

---

## 4. 維護者守則與不變量 (Invariants)

1. **嚴禁修改已簽章 CDN 網址**：`gg-dl` 路徑含有加密校驗碼，任何參數追加或修剪均會破壞簽章。
2. **嚴禁未驗證網址進入 UI 層**：未完成下載的 CDN 網址不得作為備援推至展示層，必須維持 `MediaAsset.Failed` 狀態供使用者重試。
3. **保持網路層 Cookie 隔離**：不得對非 `.google.com` 網域強制手動塞入包含帳號 Session 的 Cookie Header。
