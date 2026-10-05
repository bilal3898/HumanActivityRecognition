"""Train the CNN-LSTM on UCI-HAR raw inertial signals.

Usage (from ml/):  python -m src.train --epochs 40
"""
from __future__ import annotations

import argparse
import json

import numpy as np
from tensorflow import keras

from . import config
from .data_loader import load_split
from .model import build_cnn_lstm, set_seed
from .preprocessing import augment, compute_normalization, save_normalization, subject_validation_split


def train(epochs: int = 60, batch_size: int = 64, use_augmentation: bool = True, patience: int = 15) -> keras.Model:
    set_seed(config.RANDOM_SEED)
    X, y, subjects = load_split("train")
    (X_tr, y_tr), (X_val, y_val), val_subjects = subject_validation_split(X, y, subjects)
    print(f"train windows={len(X_tr)}  val windows={len(X_val)}  val subjects={val_subjects}")

    stats = compute_normalization(X_tr)
    save_normalization(stats, config.NORMALIZATION_PATH)

    if use_augmentation:
        X_tr, y_tr = augment(X_tr, y_tr, copies=2)

    model = build_cnn_lstm(np.array(stats["mean"]), np.array(stats["std"]))
    model.summary()

    callbacks = [
        keras.callbacks.EarlyStopping(monitor="val_accuracy", patience=patience, restore_best_weights=True),
        keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=5, min_lr=1e-5),
    ]
    history = model.fit(
        X_tr, y_tr,
        validation_data=(X_val, y_val),
        epochs=epochs,
        batch_size=batch_size,
        shuffle=True,
        callbacks=callbacks,
        verbose=2,
    )

    config.MODELS_DIR.mkdir(parents=True, exist_ok=True)
    model.save(config.KERAS_MODEL_PATH)
    hist = {k: [float(v) for v in vals] for k, vals in history.history.items()}
    hist["val_subjects"] = val_subjects
    config.HISTORY_PATH.write_text(json.dumps(hist, indent=2))
    print(f"Saved model to {config.KERAS_MODEL_PATH}")
    return model


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--epochs", type=int, default=60)
    parser.add_argument("--patience", type=int, default=15)
    parser.add_argument("--batch-size", type=int, default=64)
    parser.add_argument("--no-augment", action="store_true")
    args = parser.parse_args()
    train(args.epochs, args.batch_size, not args.no_augment, args.patience)


if __name__ == "__main__":
    main()
