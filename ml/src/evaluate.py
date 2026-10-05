"""Evaluate the trained model on the held-out UCI-HAR test subjects.

Usage (from ml/):  python -m src.evaluate
"""
from __future__ import annotations

import json

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, f1_score
from tensorflow import keras

from . import config
from .data_loader import load_split


def evaluate(model: keras.Model | None = None) -> dict:
    model = model or keras.models.load_model(config.KERAS_MODEL_PATH)
    X_test, y_test, _ = load_split("test")
    probs = model.predict(X_test, batch_size=256, verbose=0)
    y_pred = probs.argmax(axis=1)

    report = classification_report(y_test, y_pred, target_names=config.ACTIVITY_LABELS, output_dict=True, digits=4)
    cm = confusion_matrix(y_test, y_pred)
    metrics = {
        "test_accuracy": float(accuracy_score(y_test, y_pred)),
        "macro_f1": float(f1_score(y_test, y_pred, average="macro")),
        "weighted_f1": float(f1_score(y_test, y_pred, average="weighted")),
        "per_class": {k: report[k] for k in config.ACTIVITY_LABELS},
        "confusion_matrix": cm.tolist(),
        "labels": config.ACTIVITY_LABELS,
        "test_windows": int(len(y_test)),
    }
    config.METRICS_PATH.write_text(json.dumps(metrics, indent=2))

    print(classification_report(y_test, y_pred, target_names=config.ACTIVITY_LABELS, digits=4))
    print(f"Test accuracy: {metrics['test_accuracy']:.4f}   macro-F1: {metrics['macro_f1']:.4f}")

    config.REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    plot_confusion_matrix(cm, config.REPORTS_DIR / "confusion_matrix.png")
    if config.HISTORY_PATH.exists():
        plot_history(json.loads(config.HISTORY_PATH.read_text()), config.REPORTS_DIR / "training_curves.png")
    return metrics


def plot_confusion_matrix(cm: np.ndarray, path) -> None:
    norm = cm / cm.sum(axis=1, keepdims=True)
    fig, ax = plt.subplots(figsize=(8, 6.5))
    sns.heatmap(norm, annot=cm, fmt="d", cmap="Blues", cbar=True,
                xticklabels=config.ACTIVITY_LABELS, yticklabels=config.ACTIVITY_LABELS, ax=ax)
    ax.set_xlabel("Predicted")
    ax.set_ylabel("True")
    ax.set_title("CNN-LSTM confusion matrix (UCI-HAR test set)")
    plt.xticks(rotation=35, ha="right")
    fig.tight_layout()
    fig.savefig(path, dpi=130)
    plt.close(fig)


def plot_history(hist: dict, path) -> None:
    fig, axes = plt.subplots(1, 2, figsize=(11, 4))
    for ax, key in zip(axes, ["accuracy", "loss"]):
        ax.plot(hist[key], label=f"train {key}")
        ax.plot(hist[f"val_{key}"], label=f"val {key}")
        ax.set_xlabel("epoch")
        ax.set_title(key)
        ax.legend()
        ax.grid(alpha=0.3)
    fig.tight_layout()
    fig.savefig(path, dpi=130)
    plt.close(fig)


if __name__ == "__main__":
    evaluate()
