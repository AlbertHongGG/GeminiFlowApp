package com.geminiflow.app.presentation.navigation.contract

import com.geminiflow.app.presentation.navigation.model.AppRoute

interface CupertinoNavigator {
    fun push(route: AppRoute)
    fun pop()
}
