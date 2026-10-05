package com.bilal.humanactivityrecognition.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.bilal.humanactivityrecognition.HARApplication
import com.bilal.humanactivityrecognition.MainActivity
import com.bilal.humanactivityrecognition.R
import com.bilal.humanactivityrecognition.data.model.ActivityPrediction
import com.bilal.humanactivityrecognition.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Foreground service that keeps sensors + inference running while the screen is off. */
class ActivityRecognitionService : LifecycleService() {
    private val container by lazy { (application as HARApplication).container }
    private var recognitionJob: Job? = null
    private var notificationsEnabled = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> stopRecognition()
            else -> startRecognition()
        }
        return START_NOT_STICKY
    }

    private fun startRecognition() {
        createChannel()
        ServiceCompat.startForeground(
            this,
            Constants.NOTIFICATION_ID,
            buildNotification(null),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0,
        )
        if (recognitionJob?.isActive == true) return

        val repository = container.activityRepository
        val predict = container.predictActivityUseCase
        val collect = container.collectSensorDataUseCase

        recognitionJob = lifecycleScope.launch {
            launch { container.sensorManager.status.collect { repository.onSensorStatus(it) } }
            container.settingsRepository.settings.distinctUntilChanged().collectLatest { settings ->
                notificationsEnabled = settings.notificationsEnabled
                predict.reset()
                predict.setSmoothingWindow(settings.smoothingWindow)
                repository.onRecognitionStarted(settings.inputSource)
                try {
                    collect(settings, repository::onBufferProgress)
                        .flowOn(Dispatchers.Default)
                        .collect { window ->
                            val prediction = kotlinx.coroutines.withContext(Dispatchers.Default) { predict(window) }
                            repository.onPrediction(prediction, settings.frequency.stepMs, settings.inputSource)
                            updateNotification(prediction)
                        }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    repository.onError(e.message ?: e.javaClass.simpleName)
                }
            }
        }
    }

    private fun stopRecognition() {
        recognitionJob?.cancel()
        recognitionJob = null
        container.activityRepository.onRecognitionStopped()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        recognitionJob?.cancel()
        container.activityRepository.onRecognitionStopped()
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.notification_channel_description) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun updateNotification(prediction: ActivityPrediction) {
        if (!notificationsEnabled) return
        getSystemService(NotificationManager::class.java).notify(Constants.NOTIFICATION_ID, buildNotification(prediction))
    }

    private fun buildNotification(prediction: ActivityPrediction?): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, ActivityRecognitionService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val text = prediction?.let { "${it.activity.displayName} · ${(it.confidence * 100).roundToInt()}% confidence" }
            ?: getString(R.string.notification_waiting)
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(prediction?.activity?.iconRes ?: R.drawable.ic_walking)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.action_stop), stop)
            .build()
    }

    companion object {
        const val ACTION_START = "com.bilal.humanactivityrecognition.action.START"
        const val ACTION_STOP = "com.bilal.humanactivityrecognition.action.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, ActivityRecognitionService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.startService(Intent(context, ActivityRecognitionService::class.java).setAction(ACTION_STOP))
        }
    }
}
