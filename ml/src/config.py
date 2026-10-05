"""Shared configuration for the HAR ML pipeline."""
from pathlib import Path

ML_DIR = Path(__file__).resolve().parents[1]
DATASET_DIR = ML_DIR / "dataset" / "UCI_HAR_Dataset"
MODELS_DIR = ML_DIR / "models"
REPORTS_DIR = ML_DIR / "reports"
ANDROID_ASSETS_DIR = ML_DIR.parent / "app" / "src" / "main" / "assets"

# UCI-HAR windows: 2.56 s at 50 Hz with 50% overlap.
SAMPLING_RATE_HZ = 50
WINDOW_SIZE = 128
WINDOW_STEP = 64

# Channel order is part of the model contract with the Android app.
SIGNALS = [
    "body_acc_x", "body_acc_y", "body_acc_z",
    "body_gyro_x", "body_gyro_y", "body_gyro_z",
    "total_acc_x", "total_acc_y", "total_acc_z",
]
NUM_CHANNELS = len(SIGNALS)

ACTIVITY_LABELS = [
    "WALKING",
    "WALKING_UPSTAIRS",
    "WALKING_DOWNSTAIRS",
    "SITTING",
    "STANDING",
    "LAYING",
]
NUM_CLASSES = len(ACTIVITY_LABELS)

# Gravity separation used by UCI-HAR: 3rd-order Butterworth low-pass at 0.3 Hz.
GRAVITY_CUTOFF_HZ = 0.3
GRAVITY_FILTER_ORDER = 3

KERAS_MODEL_PATH = MODELS_DIR / "cnn_lstm.keras"
TFLITE_MODEL_PATH = MODELS_DIR / "har_cnn_lstm.tflite"
NORMALIZATION_PATH = MODELS_DIR / "normalization.json"
HISTORY_PATH = MODELS_DIR / "training_history.json"
METRICS_PATH = MODELS_DIR / "metrics.json"
MODEL_INFO_PATH = MODELS_DIR / "model_info.json"

RANDOM_SEED = 42
