package com.example.digital_obd_ii.presentation.triplog

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.example.digital_obd_ii.domain.logger.TripLogManager
import com.example.digital_obd_ii.domain.model.TripLogSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TripLogViewModel @Inject constructor(
    private val tripLogManager: TripLogManager
) : ViewModel() {

    val logSummary: StateFlow<TripLogSummary> = tripLogManager.logSummary
    val isFeatureEnabled: StateFlow<Boolean> = tripLogManager.isFeatureEnabled
    val isAutoStartEnabled: StateFlow<Boolean> = tripLogManager.isAutoStartEnabled

    fun setFeatureEnabled(enabled: Boolean) {
        tripLogManager.setFeatureEnabled(enabled)
    }

    fun setAutoStartEnabled(enabled: Boolean) {
        tripLogManager.setAutoStartEnabled(enabled)
    }

    fun startLogging() {
        tripLogManager.startLogging()
    }

    fun stopLogging() {
        tripLogManager.stopLogging()
    }

    fun resetLog() {
        tripLogManager.resetLog()
    }

    fun copyReportToClipboard(): Boolean {
        return tripLogManager.copyReportToClipboard()
    }

    fun shareReport(context: Context, vehicleName: String = "Honda Civic LXL 1.8 Manual") {
        val report = logSummary.value.toFormattedReport(vehicleName)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, report)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Compartilhar LOG Resumido da Viagem")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
