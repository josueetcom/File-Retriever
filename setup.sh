#!/bin/bash

# Download Android SDK
curl https://dl.google.com/android/android-sdk_r24.4.1-linux.tgz -o android-sdk-linux.tgz
tar tzf android-sdk-linux.tgz $ANDROID_HOME

android="$ANDROID_HOME/tools/android"

android update sdk -u -t android-24 <<< "y"
android update sdk -u -t build-tools-24.0.1 <<< "y"
android update sdk -u -t platform-tools <<< "y"
android update sdk -u -t tools <<< "y"
