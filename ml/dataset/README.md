# UCI-HAR dataset

The training data is **not committed** to this repository. Download it locally before training.

- Source: [UCI Machine Learning Repository: Human Activity Recognition Using Smartphones](https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones)
- 30 volunteers, Samsung Galaxy S II worn on the waist, 50 Hz accelerometer + gyroscope
- 10,299 windows of 2.56 s (128 samples, 50% overlap), with 7,352 train windows (21 subjects) and 2,947 test windows (9 subjects)
- Labels: WALKING, WALKING_UPSTAIRS, WALKING_DOWNSTAIRS, SITTING, STANDING, LAYING

## Setup

```bash
cd ml/dataset
curl -L -o uci_har.zip "https://archive.ics.uci.edu/static/public/240/human+activity+recognition+using+smartphones.zip"
unzip uci_har.zip            # produces "UCI HAR Dataset.zip"
unzip "UCI HAR Dataset.zip"  # produces "UCI HAR Dataset/"
mv "UCI HAR Dataset" UCI_HAR_Dataset
rm -rf __MACOSX *.zip
```

Expected layout:

```
ml/dataset/UCI_HAR_Dataset/
├── activity_labels.txt
├── features.txt
├── features_info.txt
├── README.txt
├── train/
│   ├── X_train.txt  y_train.txt  subject_train.txt
│   └── Inertial Signals/   (body_acc_*, body_gyro_*, total_acc_* × train)
└── test/
    ├── X_test.txt   y_test.txt   subject_test.txt
    └── Inertial Signals/
```

The CNN-LSTM is trained on the raw **Inertial Signals**. The 561-feature `X_*.txt` matrices are loaded only by the exploration notebook for comparison.

## Citation

Davide Anguita, Alessandro Ghio, Luca Oneto, Xavier Parra and Jorge L. Reyes-Ortiz. *A Public Domain Dataset for Human Activity Recognition Using Smartphones.* ESANN 2013.
