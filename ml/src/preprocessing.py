"""Preprocessing shared by training and (mirrored in Kotlin) the Android app."""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from scipy.signal import butter, lfilter, lfilter_zi

from .config import (
    GRAVITY_CUTOFF_HZ,
    GRAVITY_FILTER_ORDER,
    NUM_CHANNELS,
    RANDOM_SEED,
    SAMPLING_RATE_HZ,
    SIGNALS,
    WINDOW_SIZE,
    WINDOW_STEP,
)

STANDARD_GRAVITY = 9.80665


def gravity_filter_coefficients(fs: float = SAMPLING_RATE_HZ):
    """Butterworth low-pass coefficients used to estimate gravity from total acceleration."""
    return butter(GRAVITY_FILTER_ORDER, GRAVITY_CUTOFF_HZ / (fs / 2.0), btype="low")


def split_gravity(total_acc_g: np.ndarray, fs: float = SAMPLING_RATE_HZ):
    """Causally split total acceleration (n, 3) in g into (body_acc, gravity).

    The Android app applies the same causal IIR filter sample-by-sample.
    """
    b, a = gravity_filter_coefficients(fs)
    # Start in steady state at the first sample (the Kotlin filter does the same).
    zi = lfilter_zi(b, a)[:, None] * total_acc_g[0][None, :]
    gravity, _ = lfilter(b, a, total_acc_g, axis=0, zi=zi)
    return total_acc_g - gravity, gravity


def build_model_input(acc_ms2: np.ndarray, gyro_rads: np.ndarray, fs: float = SAMPLING_RATE_HZ) -> np.ndarray:
    """Convert a continuous phone recording into model channels (n, 9).

    acc_ms2: accelerometer incl. gravity in m/s^2, gyro_rads: gyroscope in rad/s.
    """
    total_acc_g = acc_ms2 / STANDARD_GRAVITY
    body_acc, _ = split_gravity(total_acc_g, fs)
    return np.concatenate([body_acc, gyro_rads, total_acc_g], axis=1).astype(np.float32)


def sliding_windows(signal: np.ndarray, size: int = WINDOW_SIZE, step: int = WINDOW_STEP) -> np.ndarray:
    """Slice a continuous (n, channels) signal into (n_windows, size, channels)."""
    if len(signal) < size:
        return np.empty((0, size, signal.shape[1]), dtype=signal.dtype)
    starts = range(0, len(signal) - size + 1, step)
    return np.stack([signal[s:s + size] for s in starts])


def compute_normalization(X: np.ndarray) -> dict:
    flat = X.reshape(-1, NUM_CHANNELS)
    return {
        "channels": SIGNALS,
        "mean": flat.mean(axis=0).astype(float).tolist(),
        "std": (flat.std(axis=0) + 1e-8).astype(float).tolist(),
    }


def normalize(X: np.ndarray, stats: dict) -> np.ndarray:
    return (X - np.asarray(stats["mean"], np.float32)) / np.asarray(stats["std"], np.float32)


def save_normalization(stats: dict, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(stats, indent=2))


def load_normalization(path: Path) -> dict:
    return json.loads(path.read_text())


def subject_validation_split(X, y, subjects, val_fraction: float = 0.2, seed: int = RANDOM_SEED):
    """Hold out whole subjects for validation so no person appears in both sets."""
    rng = np.random.default_rng(seed)
    unique = np.unique(subjects)
    n_val = max(1, int(round(len(unique) * val_fraction)))
    val_subjects = rng.choice(unique, size=n_val, replace=False)
    val_mask = np.isin(subjects, val_subjects)
    return (X[~val_mask], y[~val_mask]), (X[val_mask], y[val_mask]), sorted(val_subjects.tolist())


def augment(X: np.ndarray, y: np.ndarray, seed: int = RANDOM_SEED, copies: int = 1):
    """Light augmentation: jitter + per-window amplitude scaling."""
    rng = np.random.default_rng(seed)
    Xs, ys = [X], [y]
    for _ in range(copies):
        scale = rng.normal(1.0, 0.1, size=(len(X), 1, X.shape[2])).astype(np.float32)
        noise = rng.normal(0.0, 0.01, size=X.shape).astype(np.float32)
        Xs.append(X * scale + noise)
        ys.append(y)
    return np.concatenate(Xs), np.concatenate(ys)
