package com.bilal.humanactivityrecognition.utils

object Constants {
    const val MODEL_PATH = "models/har_cnn_lstm.tflite"
    const val MODEL_INFO_PATH = "models/model_info.json"
    const val LABELS_PATH = "labels/labels.txt"
    const val DEMO_WINDOWS_PATH = "demo/uci_test_windows.json"

    /** UCI-HAR contract: 50 Hz, 2.56 s windows of 9 channels. */
    const val SAMPLING_RATE_HZ = 50
    const val SAMPLING_PERIOD_US = 1_000_000 / SAMPLING_RATE_HZ
    const val SAMPLING_PERIOD_NS = 1_000_000_000L / SAMPLING_RATE_HZ
    const val WINDOW_SIZE = 128
    const val NUM_CHANNELS = 9
    const val NUM_CLASSES = 6
    const val STANDARD_GRAVITY = 9.80665f

    /** scipy.signal.butter(3, 0.3 / 25.0) — gravity estimate used by UCI-HAR. */
    val GRAVITY_FILTER_B = doubleArrayOf(6.45184913e-06, 1.93555474e-05, 1.93555474e-05, 6.45184913e-06)
    val GRAVITY_FILTER_A = doubleArrayOf(1.0, -2.92460624, 2.85202782, -0.92736997)

    /** Consecutive predictions of the same activity closer than this are merged into one history segment. */
    const val SEGMENT_MERGE_GAP_MS = 5_000L

    /** Resampler resets if sensor events stop for longer than this. */
    const val MAX_SENSOR_GAP_NS = 1_000_000_000L

    const val RECENT_PREDICTIONS_LIMIT = 20

    const val NOTIFICATION_CHANNEL_ID = "activity_recognition"
    const val NOTIFICATION_ID = 1001
}
