"""Hybrid CNN + LSTM architecture for HAR."""
from __future__ import annotations

import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

from .config import NUM_CHANNELS, NUM_CLASSES, WINDOW_SIZE


def build_cnn_lstm(
    mean: np.ndarray | None = None,
    std: np.ndarray | None = None,
    window_size: int = WINDOW_SIZE,
    num_channels: int = NUM_CHANNELS,
    num_classes: int = NUM_CLASSES,
) -> keras.Model:
    """CNN blocks learn local motion patterns; the LSTM models their temporal order.

    Per-channel standardisation is baked into the graph so the exported TFLite model
    accepts raw sensor windows (body_acc/total_acc in g, gyro in rad/s).
    """
    inputs = keras.Input(shape=(window_size, num_channels), name="sensor_window")
    x = inputs
    if mean is not None and std is not None:
        x = layers.Normalization(
            axis=-1, mean=np.asarray(mean), variance=np.asarray(std) ** 2, name="standardize"
        )(x)

    x = layers.Conv1D(64, 5, padding="same", name="conv1")(x)
    x = layers.BatchNormalization(name="bn1")(x)
    x = layers.Activation("relu")(x)
    x = layers.Conv1D(64, 5, padding="same", name="conv2")(x)
    x = layers.BatchNormalization(name="bn2")(x)
    x = layers.Activation("relu")(x)
    x = layers.MaxPooling1D(2, name="pool1")(x)
    x = layers.Dropout(0.2)(x)

    x = layers.Conv1D(128, 3, padding="same", name="conv3")(x)
    x = layers.BatchNormalization(name="bn3")(x)
    x = layers.Activation("relu")(x)
    x = layers.MaxPooling1D(2, name="pool2")(x)
    x = layers.Dropout(0.3)(x)

    x = layers.LSTM(64, dropout=0.1, name="lstm")(x)
    x = layers.Dropout(0.4)(x)
    x = layers.Dense(64, activation="relu", kernel_regularizer=keras.regularizers.l2(1e-4), name="dense")(x)
    outputs = layers.Dense(num_classes, activation="softmax", name="activity_probs")(x)

    model = keras.Model(inputs, outputs, name="har_cnn_lstm")
    model.compile(
        optimizer=keras.optimizers.Adam(1e-3),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    return model


def set_seed(seed: int) -> None:
    keras.utils.set_random_seed(seed)
    tf.config.experimental.enable_op_determinism()
