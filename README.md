# Human Activity Recognition (CNN-LSTM + Android)

Recognizes six daily activities (**walking, walking upstairs, walking downstairs, sitting, standing, laying**) from smartphone accelerometer and gyroscope data. A hybrid **CNN + LSTM** trained on the public [UCI-HAR dataset](https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones) runs on-device in a Kotlin / Jetpack Compose app via **TensorFlow Lite**.

| | |
|---|---|
| Test accuracy (UCI-HAR, unseen subjects) | **92.3%** (macro-F1 0.922) |
| Model | 103k parameters, 412 KB `.tflite`, float32 |
| Input | 2.56 s window: 128 samples × 9 channels at 50 Hz |
| App | Kotlin, Compose, Material 3, TFLite, Room, DataStore, foreground service; minSdk 26 / targetSdk 35 |

## How it works

```
Training (ml/)                                   Android (app/)
UCI-HAR raw inertial signals                     Phone accelerometer + gyroscope
        ↓                                                ↓  resampled to 50 Hz
preprocessing (gravity filter, normalization)    same gravity filter, m/s² → g
        ↓                                                ↓  128-sample sliding window
CNN-LSTM (Keras)  ──── export ────►  har_cnn_lstm.tflite  ──►  6 class probabilities
                                                         ↓  temporal smoothing
                                         live UI · Room history · statistics
```

The UCI dataset is **only used for training**. It lives in `ml/dataset/UCI_HAR_Dataset/` (not committed, see [ml/dataset/README.md](ml/dataset/README.md)). The app ships only the exported model, labels and a few test windows for demo mode.

## Repository layout

```
app/                     Android application
  src/main/java/com/bilal/humanactivityrecognition/
    data/                sensors, repositories, models
    domain/              use cases (collect sensor data, predict, confidence)
    ml/                  TFLite helper, input/output processors, classifier
    database/            Room entity, DAO, database
    service/             foreground ActivityRecognitionService
    ui/                  Compose navigation, screens, components, theme
    utils/               constants, filters/resampler, date & permission helpers
  src/main/assets/       har_cnn_lstm.tflite, model_info.json, labels.txt, demo windows
  src/test/              JVM unit tests
ml/                      training pipeline (see ml/README.md)
  dataset/               UCI_HAR_Dataset goes here (gitignored)
  notebooks/             01 exploration · 02 preprocessing · 03 model · 04 evaluation
  src/                   data_loader, preprocessing, model, train, evaluate, export_tflite
  models/  reports/      trained model, metrics, plots
docs/                    architecture / API / database documentation
```

## App features

| Screen | What it shows |
|---|---|
| **Home** | current activity and confidence, sensor status, Start/Stop recognition, today's summary |
| **Live Recognition** | predicted activity, confidence %, per-class probabilities, live sensor rates, buffer progress, recent predictions, session timer |
| **History** | detected activity segments with start/end time, duration, average confidence; clear history |
| **Statistics** | time per activity (walking, sitting, standing, upstairs, downstairs, laying) for today/week; daily and weekly charts |
| **Settings** | recognition frequency, input source, smoothing, notifications, model information, about |

Recognition runs in a foreground service, so it keeps working while the screen is off. Each prediction is smoothed over the last N windows, and consecutive predictions of the same activity are merged into one history segment.

**Input sources**
- *Phone sensors*: live accelerometer + gyroscope. If the device has no gyroscope, the gyroscope channels are zero-filled and the app shows a warning.
- *UCI demo replay*: replays bundled UCI test windows and shows the ground truth next to each prediction. Useful on emulators, which don't produce realistic motion.

> For results closest to the training data, carry the phone at the waist (belt or pocket), as the UCI volunteers did.

## Build & run

**Requirements:** Android Studio (Koala or newer) or JDK 17 + Android SDK 35.

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # JVM unit tests
./gradlew installDebug           # install on a connected device/emulator
```

**Retrain the model** (optional, a trained model is already in `app/src/main/assets`):

```bash
cd ml
pip install -r requirements.txt
# download the dataset first (ml/dataset/README.md)
python -m src.train && python -m src.evaluate && python -m src.export_tflite
```

## Applications

Elderly-care monitoring (e.g. long inactivity), fitness and exercise tracking, rehabilitation progress, and context-aware mobile apps, all using a phone and no dedicated wearable hardware.

## Limitations

- Trained on 30 subjects with a waist-mounted phone. Other phone placements (hand, bag) will lower accuracy.
- SITTING and STANDING are the most often confused pair.
- No transition classes (sit-to-stand etc.). Transitions are absorbed by smoothing.

## License

MIT, see [LICENSE](LICENSE). Dataset: Anguita et al., ESANN 2013.
