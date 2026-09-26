package com.geminiflow.app.presentation.viewmodel

import com.geminiflow.app.presentation.navigation.model.AppRoute

data class MainUiState(
    val backStack: List<AppRoute> = listOf(AppRoute.Main)
)
