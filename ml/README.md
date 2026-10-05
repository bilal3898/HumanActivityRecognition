# ML pipeline: CNN-LSTM on UCI-HAR

Trains a hybrid CNN + LSTM on the raw UCI-HAR inertial signals, then exports it to TensorFlow Lite for the Android app.

## Setup

```bash
cd ml
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
# download the dataset into ml/dataset/UCI_HAR_Dataset (see dataset/README.md)
```

## Run

```bash
python -m src.train           # trains, saves models/cnn_lstm.keras, normalization.json, training_history.json
python -m src.evaluate        # test-set metrics → models/metrics.json, reports/*.png
python -m src.export_tflite   # → models/har_cnn_lstm.tflite and copies assets into app/src/main/assets
```

`export_tflite` copies four files into the Android app:

| File | Purpose |
|---|---|
| `models/har_cnn_lstm.tflite` | model (~410 KB, float32) |
| `models/model_info.json` | metadata shown on the Settings → Model information screen |
| `labels/labels.txt` | class order |
| `demo/uci_test_windows.json` | 72 UCI test windows (12 per class) for the app's demo replay mode |

## Layout

| Path | Contents |
|---|---|
| `src/config.py` | paths and constants (50 Hz, 128-sample windows, 9 channels, 6 labels) |
| `src/data_loader.py` | loads `Inertial Signals` → `(n, 128, 9)`, labels and subjects |
| `src/preprocessing.py` | gravity filter (same as on Android), normalization, subject-wise split, augmentation |
| `src/model.py` | CNN-LSTM definition |
| `src/train.py` / `evaluate.py` / `export_tflite.py` | pipeline steps |
| `notebooks/01–04` | exploration, preprocessing, model and evaluation walkthroughs |

## Input

Each window is 128 samples (2.56 s at 50 Hz) × 9 channels: `body_acc_xyz` (g), `body_gyro_xyz` (rad/s), `total_acc_xyz` (g). The per-channel mean/std from the training split goes into the model as a `Normalization` layer, so the app feeds raw values.

## Architecture

```
Input (128, 9) → Normalization
→ Conv1D(64, k5) → BN → Conv1D(64, k5) → BN → MaxPool(2) → Dropout
→ Conv1D(128, k3) → BN → MaxPool(2) → Dropout          ← local motion patterns
→ LSTM(64)                                              ← temporal dependencies
→ Dropout → Dense(64, ReLU, L2) → Dense(6, softmax)
```

103,174 parameters. Training setup:

- Adam optimizer, categorical cross-entropy loss.
- Early stopping on validation accuracy (patience 15) and ReduceLROnPlateau.
- Validation set: about 20% of the **training subjects**, held out by subject so no person appears in both train and validation.
- Augmentation: amplitude scaling + jitter, 2 extra copies of each training window.

## Results (UCI test set: 2,947 windows, 9 unseen subjects)

| Metric | Value |
|---|---|
| Accuracy | **92.26%** |
| Macro-F1 | **0.9225** |
| TFLite vs Keras | max abs diff 4e-7, 100% argmax agreement |
| Accuracy with body_acc recomputed by the on-device causal filter | 91.52% |

| Class | Precision | Recall | F1 |
|---|---|---|---|
| WALKING | 0.994 | 0.994 | 0.994 |
| WALKING_UPSTAIRS | 0.953 | 0.900 | 0.926 |
| WALKING_DOWNSTAIRS | 0.899 | 0.957 | 0.927 |
| SITTING | 0.820 | 0.855 | 0.838 |
| STANDING | 0.869 | 0.833 | 0.850 |
| LAYING | 1.000 | 1.000 | 1.000 |

Most errors are confusion between SITTING and STANDING, which is the usual pattern on UCI-HAR because a waist-worn phone looks almost the same in both postures. See `reports/confusion_matrix.png` and `reports/training_curves.png`.
