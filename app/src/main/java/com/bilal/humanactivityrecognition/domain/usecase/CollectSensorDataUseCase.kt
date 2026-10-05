package com.bilal.humanactivityrecognition.domain.usecase

import android.content.Context
import com.bilal.humanactivityrecognition.data.model.ActivityClass
import com.bilal.humanactivityrecognition.data.sensor.SensorManager
import com.bilal.humanactivityrecognition.domain.model.AppSettings
import com.bilal.humanactivityrecognition.domain.model.InputSource
import com.bilal.humanactivityrecognition.ml.ModelInputProcessor
import com.bilal.humanactivityrecognition.utils.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import org.json.JSONObject

/** A model-ready [1][128][9] window, optionally with a known label (demo replay). */
class SensorWindow(val data: Array<Array<FloatArray>>, val groundTruth: ActivityClass? = null)

/** Produces model windows either from live phone sensors or from bundled UCI-HAR test windows. */
class CollectSensorDataUseCase(
    private val context: Context,
    private val sensorManager: SensorManager,
) {
    private var demoWindows: List<SensorWindow>? = null

    operator fun invoke(settings: AppSettings, onBufferProgress: (Int) -> Unit = {}): Flow<SensorWindow> =
        when (settings.inputSource) {
            InputSource.PHONE_SENSORS -> phoneWindows(settings.frequency.stepSamples, onBufferProgress)
            InputSource.DEMO_REPLAY -> demoReplay(settings.frequency.stepMs, onBufferProgress)
        }

    private fun phoneWindows(step: Int, onBufferProgress: (Int) -> Unit): Flow<SensorWindow> {
        val processor = ModelInputProcessor(stepSize = step)
        return sensorManager.samples().mapNotNull { sample ->
            val window = processor.add(sample)
            onBufferProgress(processor.bufferedSamples)
            window?.let { SensorWindow(it) }
        }
    }

    private fun demoReplay(intervalMs: Long, onBufferProgress: (Int) -> Unit): Flow<SensorWindow> = flow {
        val windows = loadDemoWindows()
        onBufferProgress(Constants.WINDOW_SIZE)
        var i = 0
        while (true) {
            delay(intervalMs)
            emit(windows[i % windows.size])
            i++
        }
    }

    private fun loadDemoWindows(): List<SensorWindow> = demoWindows ?: run {
        val json = context.assets.open(Constants.DEMO_WINDOWS_PATH).bufferedReader().use { it.readText() }
        parseDemoWindows(json).also { demoWindows = it }
    }

    companion object {
        fun parseDemoWindows(json: String): List<SensorWindow> {
            val root = JSONObject(json)
            val windows = root.getJSONArray("windows")
            return List(windows.length()) { w ->
                val obj = windows.getJSONObject(w)
                val rows = obj.getJSONArray("data")
                val data = Array(rows.length()) { t ->
                    val row = rows.getJSONArray(t)
                    FloatArray(row.length()) { c -> row.getDouble(c).toFloat() }
                }
                SensorWindow(arrayOf(data), ActivityClass.fromLabel(obj.getString("label")))
            }
        }
    }
}
