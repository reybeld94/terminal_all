package com.example.terminal.di

import android.content.Context
import com.example.terminal.data.local.UserPrefs
import com.example.terminal.data.repository.WorkOrdersRepository

object AppContainer {

    fun workOrdersRepository(
        context: Context,
        userPrefs: UserPrefs = UserPrefs.create(context.applicationContext)
    ): WorkOrdersRepository {
        return WorkOrdersRepository(userPrefs)
    }
}
