#!/bin/bash

# Download Android SDK
curl https://dl.google.com/android/android-sdk_r24.4.1-linux.tgz -o android-sdk-linux.tgz
tar xzf android-sdk-linux.tgz

function install {
  for package; do
    $ANDROID_HOME/tools/android update sdk -u -t $package <<< "y"
  done
}

install android-24 build-tools-24.0.1 platform-tools tools extra-android-m2repository

