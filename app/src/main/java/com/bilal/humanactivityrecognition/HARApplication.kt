package com.bilal.humanactivityrecognition

import android.app.Application
import android.content.Context
import com.bilal.humanactivityrecognition.data.repository.ActivityRepository
import com.bilal.humanactivityrecognition.data.repository.SettingsRepository
import com.bilal.humanactivityrecognition.data.sensor.SensorManager
import com.bilal.humanactivityrecognition.database.AppDatabase
import com.bilal.humanactivityrecognition.domain.usecase.CollectSensorDataUseCase
import com.bilal.humanactivityrecognition.domain.usecase.PredictActivityUseCase
import com.bilal.humanactivityrecognition.ml.ActivityClassifier
import com.bilal.humanactivityrecognition.ml.ModelOutputProcessor
import com.bilal.humanactivityrecognition.ml.TensorFlowLiteHelper
import com.bilal.humanactivityrecognition.utils.Constants
import org.json.JSONObject

class HARApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency container shared by the UI and the foreground service. */
class AppContainer(private val context: Context) {
    val appContext: Context get() = context

    private val database by lazy { AppDatabase.create(context) }
    val activityRepository by lazy {
        ActivityRepository(database.predictionDao()).also { it.onSensorStatus(sensorManager.status.value) }
    }
    val settingsRepository by lazy { SettingsRepository(context) }
    val sensorManager by lazy { SensorManager(context) }

    private val classifier by lazy {
        val labels = context.assets.open(Constants.LABELS_PATH).bufferedReader().use { it.readText() }
        ActivityClassifier(
            TensorFlowLiteHelper(context, Constants.MODEL_PATH),
            ModelOutputProcessor(ModelOutputProcessor.parseLabels(labels)),
        )
    }
    val predictActivityUseCase by lazy { PredictActivityUseCase(classifier) }
    val collectSensorDataUseCase by lazy { CollectSensorDataUseCase(context, sensorManager) }

    val modelInfo: Map<String, String> by lazy {
        runCatching {
            val json = JSONObject(context.assets.open(Constants.MODEL_INFO_PATH).bufferedReader().use { it.readText() })
            json.keys().asSequence().associateWith { json.get(it).toString() }
        }.getOrDefault(emptyMap())
    }
}
