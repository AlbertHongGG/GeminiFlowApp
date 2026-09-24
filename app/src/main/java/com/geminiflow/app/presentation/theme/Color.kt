package com.geminiflow.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Canvas & Surfaces (Pure Light Mode)
val BgCanvas = Color(0xFFF8FAFC)        // Slate-50 冷白背景
val SurfaceCard = Color(0xFFFFFFFF)     // 純白陶瓷卡片表面
val SurfaceElevated = Color(0xFFF1F5F9) // Slate-100 輸入框與輔助底色
val SurfaceHighlight = Color(0xFFE2E8F0)// Slate-200

// Borders
val BorderLight = Color(0xFFE2E8F0)     // Slate-200 1dp 精緻微邊框
val BorderFocused = Color(0xFFCBD5E1)   // Slate-300

// Typography
val TextPrimary = Color(0xFF0F172A)     // Slate-900 高對比深邃主文字
val TextSecondary = Color(0xFF64748B)   // Slate-500 中灰副文字
val TextMuted = Color(0xFF94A3B8)       // Slate-400 淺灰輔助文字

// Semantic Accents (Flat, No Solid Gradients)
val AccentEmerald = Color(0xFF059669)       // 翡翠綠 (Online / 200 OK / 成功)
val AccentEmeraldLight = Color(0xFFECFDF5)  // 翡翠綠淺底
val AccentBlue = Color(0xFF2563EB)          // 皇家藍 (Brand / Action / Highlight)
val AccentBlueLight = Color(0xFFEFF6FF)     // 皇家藍淺底
val AccentAmber = Color(0xFFD97706)         // 琥珀暖橙 (Warning / 待設定)
val AccentAmberLight = Color(0xFFFFFBEB)    // 琥珀淺底
val AccentRose = Color(0xFFDC2626)          // 石榴紅 (Offline / 錯誤 / 危險操作)
val AccentRoseLight = Color(0xFFFEF2F2)     // 石榴紅淺底

// Backward Compatibility Aliases
val BluePrimary = AccentBlue
val BlueSecondary = Color(0xFF3B82F6)
val BlueTertiary = Color(0xFF60A5FA)
val GreenSuccess = AccentEmerald
val RedError = AccentRose
val OrangeWarning = AccentAmber
