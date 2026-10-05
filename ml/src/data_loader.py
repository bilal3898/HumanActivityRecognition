"""Loading utilities for the UCI-HAR dataset (raw inertial signals)."""
from __future__ import annotations

from pathlib import Path

import numpy as np

from .config import ACTIVITY_LABELS, DATASET_DIR, SIGNALS


def _check_dataset(dataset_dir: Path) -> None:
    if not (dataset_dir / "train" / "Inertial Signals").is_dir():
        raise FileNotFoundError(
            f"UCI-HAR dataset not found at {dataset_dir}. "
            "See ml/dataset/README.md for download instructions."
        )


def load_signals(split: str, dataset_dir: Path = DATASET_DIR) -> np.ndarray:
    """Return raw inertial windows with shape (n_windows, 128, 9)."""
    _check_dataset(dataset_dir)
    signal_dir = dataset_dir / split / "Inertial Signals"
    channels = [np.loadtxt(signal_dir / f"{name}_{split}.txt", dtype=np.float32) for name in SIGNALS]
    return np.stack(channels, axis=-1)


def load_labels(split: str, dataset_dir: Path = DATASET_DIR) -> np.ndarray:
    """Return zero-based activity labels with shape (n_windows,)."""
    _check_dataset(dataset_dir)
    return np.loadtxt(dataset_dir / split / f"y_{split}.txt", dtype=np.int64) - 1


def load_subjects(split: str, dataset_dir: Path = DATASET_DIR) -> np.ndarray:
    _check_dataset(dataset_dir)
    return np.loadtxt(dataset_dir / split / f"subject_{split}.txt", dtype=np.int64)


def load_split(split: str, dataset_dir: Path = DATASET_DIR):
    """Return (X, y, subjects) for 'train' or 'test'."""
    return load_signals(split, dataset_dir), load_labels(split, dataset_dir), load_subjects(split, dataset_dir)


def load_feature_split(split: str, dataset_dir: Path = DATASET_DIR):
    """Return the 561 hand-crafted features (used only for baseline comparison)."""
    _check_dataset(dataset_dir)
    X = np.loadtxt(dataset_dir / split / f"X_{split}.txt", dtype=np.float32)
    return X, load_labels(split, dataset_dir)


def activity_name(index: int) -> str:
    return ACTIVITY_LABELS[index]
