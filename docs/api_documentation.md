# Internal API documentation

The app has no network API. This document covers the contracts between layers and between the Python training code and the Android app.

## 1. Model contract (Python ⇄ Android)

| Item | Value |
|---|---|
| File | `app/src/main/assets/models/har_cnn_lstm.tflite` |
| Input | `float32[1, 128, 9]`, a 2.56 s window at 50 Hz |
| Output | `float32[1, 6]`, softmax probabilities |
| Normalization | Embedded in the model (Keras `Normalization` layer). Feed raw channel values. |
| Labels | `assets/labels/labels.txt`, in UCI order (index 0 = WALKING) |
| Metadata | `assets/models/model_info.json` (accuracy, macro-F1, export date, input spec) |

Channel order (index → signal):

| # | Signal | Unit | Android source |
|---|---|---|---|
| 0–2 | `body_acc_x/y/z` | g | (accelerometer − gravity estimate) / 9.80665 |
| 3–5 | `body_gyro_x/y/z` | rad/s | `TYPE_GYROSCOPE` (zeros if the device has no gyroscope) |
| 6–8 | `total_acc_x/y/z` | g | `TYPE_ACCELEROMETER` / 9.80665 |

The gravity estimate comes from a 3rd-order Butterworth low-pass filter at 0.3 Hz (`scipy.signal.butter(3, 0.3/25)`). It starts in steady state from the first sample. Python implements it in `ml/src/preprocessing.py::split_gravity` and Kotlin in `ml/ModelInputProcessor.kt` via `utils/SensorUtils.kt::IirFilter`, using the same coefficients (`Constants.GRAVITY_FILTER_B/A`).

## 2. Android layers

```
Sensors ─► SensorManager (50 Hz resampling) ─► CollectSensorDataUseCase ─► ModelInputProcessor (windows)
                                                      │ (or bundled UCI demo windows)
                                                      ▼
ActivityRecognitionService ─► PredictActivityUseCase ─► ActivityClassifier ─► TensorFlowLiteHelper (TFLite)
            │                        └─► CalculateConfidenceUseCase (temporal smoothing)
            ▼
ActivityRepository ─► Room (PredictionDao) ─► ViewModels ─► Compose screens
SettingsRepository (DataStore) ─────────────┘
```

### ml/
- `InferenceEngine.run(input: Array<Array<FloatArray>>): FloatArray`: one inference. `TensorFlowLiteHelper` is the TFLite implementation and tests substitute a fake.
- `ActivityClassifier.classify(window)`: validates the `[1][128][9]` shape, runs inference, measures latency, returns `ActivityPrediction`.
- `ModelInputProcessor.add(sample): Array<Array<FloatArray>>?`: buffers resampled sensor samples. Returns a window once 128 samples are buffered, then again every `stepSize` samples.
- `ModelOutputProcessor.process(probabilities, timestampMs)`: argmax → `ActivityPrediction`.

### domain/usecase
- `CollectSensorDataUseCase(settings)`: `Flow<SensorWindow>` from the phone sensors or the bundled demo replay.
- `PredictActivityUseCase(window)`: classifies, then smooths over the last N predictions (N is configurable in Settings).
- `CalculateConfidenceUseCase.level(confidence)`: HIGH ≥ 0.8, MEDIUM ≥ 0.5, LOW otherwise.

### data/repository
- `ActivityRepository.live: StateFlow<LiveRecognitionState>`: running flag, current prediction, sensor status, recent predictions.
- `ActivityRepository.onPrediction(...)`: extends the latest history segment if the activity is unchanged and the gap is < 5 s, otherwise inserts a new segment.
- `recentSegments()`, `segmentsBetween(from, to)`, `dailySummaries(endDate, days)`: read models for History and Statistics.
- `SettingsRepository.settings: Flow<AppSettings>`: frequency, input source, smoothing window, notifications.

### service
- `ActivityRecognitionService.start(context)` / `.stop(context)`: foreground service (type `health`) that owns the sensor → model → repository pipeline and updates an ongoing notification.

## 3. Database (Room, `har_database`)

`predictions` table (`PredictionEntity`). Each row is one contiguous activity segment.

| Column | Type | Notes |
|---|---|---|
| `id` | Long PK | auto-generated |
| `activity` | String | UCI label, e.g. `WALKING` |
| `startTime` / `endTime` | Long | epoch ms (indexed) |
| `confidenceSum` | Float | sum of the per-prediction confidences in the segment |
| `predictionCount` | Int | average confidence = `confidenceSum / predictionCount` |
| `source` | String | `PHONE_SENSORS` or `DEMO_REPLAY` |
