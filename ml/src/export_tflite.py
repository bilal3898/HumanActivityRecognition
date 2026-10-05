"""Convert the trained Keras model to TensorFlow Lite and copy assets into the Android app.

Usage (from ml/):  python -m src.export_tflite
"""
from __future__ import annotations

import json
import shutil
import tempfile

import numpy as np
import tensorflow as tf
from tensorflow import keras

from . import config
from .data_loader import load_split

DEMO_WINDOWS_PER_CLASS = 12


def convert(model: keras.Model) -> bytes:
    """Convert with a fixed batch of 1 so the LSTM maps to the builtin fused TFLite op.

    Goes through a SavedModel export: converting a Keras 3 model's concrete function
    directly fails on the LSTM's resource variables in TF 2.16.
    """
    signature = tf.TensorSpec([1, config.WINDOW_SIZE, config.NUM_CHANNELS], tf.float32, name="sensor_window")
    with tempfile.TemporaryDirectory() as saved_model_dir:
        archive = keras.export.ExportArchive()
        archive.track(model)
        archive.add_endpoint(name="serve", fn=lambda x: model(x, training=False), input_signature=[signature])
        archive.write_out(saved_model_dir)
        converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir, signature_keys=["serve"])
        converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
        return converter.convert()


def verify(tflite_bytes: bytes, model: keras.Model, X: np.ndarray) -> float:
    """Return agreement between Keras and TFLite predictions on X."""
    interpreter = tf.lite.Interpreter(model_content=tflite_bytes)
    interpreter.allocate_tensors()
    inp = interpreter.get_input_details()[0]["index"]
    out = interpreter.get_output_details()[0]["index"]
    tfl = []
    for window in X:
        interpreter.set_tensor(inp, window[None].astype(np.float32))
        interpreter.invoke()
        tfl.append(interpreter.get_tensor(out)[0])
    tfl = np.array(tfl)
    ref = model.predict(X, verbose=0)
    print(f"max |keras - tflite| = {np.abs(tfl - ref).max():.2e}")
    return float((tfl.argmax(1) == ref.argmax(1)).mean())


def export_demo_windows(X: np.ndarray, y: np.ndarray, subjects: np.ndarray) -> dict:
    """Pick consecutive test windows per class for the app's demo replay mode."""
    rng = np.random.default_rng(config.RANDOM_SEED)
    windows = []
    for cls in range(config.NUM_CLASSES):
        idx = np.where(y == cls)[0]
        start = rng.integers(0, len(idx) - DEMO_WINDOWS_PER_CLASS)
        for i in idx[start:start + DEMO_WINDOWS_PER_CLASS]:
            windows.append({
                "label": config.ACTIVITY_LABELS[cls],
                "subject": int(subjects[i]),
                "data": np.round(X[i], 6).tolist(),
            })
    return {"channels": config.SIGNALS, "window_size": config.WINDOW_SIZE, "windows": windows}


def main() -> None:
    model = keras.models.load_model(config.KERAS_MODEL_PATH)
    tflite_bytes = convert(model)
    config.TFLITE_MODEL_PATH.write_bytes(tflite_bytes)
    print(f"TFLite model: {config.TFLITE_MODEL_PATH} ({len(tflite_bytes) / 1024:.1f} KB)")

    X_test, y_test, s_test = load_split("test")
    agreement = verify(tflite_bytes, model, X_test[:300])
    print(f"Keras/TFLite argmax agreement on 300 test windows: {agreement:.4f}")

    metrics = json.loads(config.METRICS_PATH.read_text()) if config.METRICS_PATH.exists() else {}
    info = {
        "name": "HAR CNN-LSTM",
        "version": "1.0.0",
        "dataset": "UCI Human Activity Recognition Using Smartphones",
        "input_shape": [1, config.WINDOW_SIZE, config.NUM_CHANNELS],
        "channels": config.SIGNALS,
        "sampling_rate_hz": config.SAMPLING_RATE_HZ,
        "window_size": config.WINDOW_SIZE,
        "labels": config.ACTIVITY_LABELS,
        "parameters": int(model.count_params()),
        "size_kb": round(len(tflite_bytes) / 1024, 1),
        "test_accuracy": metrics.get("test_accuracy"),
        "macro_f1": metrics.get("macro_f1"),
    }
    config.MODEL_INFO_PATH.write_text(json.dumps(info, indent=2))

    assets = config.ANDROID_ASSETS_DIR
    (assets / "models").mkdir(parents=True, exist_ok=True)
    (assets / "labels").mkdir(parents=True, exist_ok=True)
    (assets / "demo").mkdir(parents=True, exist_ok=True)
    shutil.copy(config.TFLITE_MODEL_PATH, assets / "models" / config.TFLITE_MODEL_PATH.name)
    shutil.copy(config.MODEL_INFO_PATH, assets / "models" / "model_info.json")
    (assets / "labels" / "labels.txt").write_text("\n".join(config.ACTIVITY_LABELS) + "\n")
    demo = export_demo_windows(X_test, y_test, s_test)
    (assets / "demo" / "uci_test_windows.json").write_text(json.dumps(demo, separators=(",", ":")))
    print(f"Copied model, labels and {len(demo['windows'])} demo windows to {assets}")


if __name__ == "__main__":
    main()
