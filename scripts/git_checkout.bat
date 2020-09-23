@echo off
git submodule foreach --recursive git fetch --all
git checkout "%1"
git pull
cd myfiziq-android
git checkout "%1"
git pull
cd ..\myfiziq-sdk-android
git checkout "%1"
git pull
cd ..\myfiziqsdk-android-cpp
git checkout "%1"
git pull
cd ..\myfiziqsdk-android-cpp\src\main\cpp\myfiziq-sdk-cpp-utils
git checkout "%1"
git pull
cd ..\..\..\..\..\myfiziqsdk-android-input
git checkout "%1"
git pull
cd ..\myfiziqsdk-android-onboarding
git checkout "%1"
git pull
cd ..\myfiziqsdk-android-profile
git checkout "%1"
git pull
cd ..\myfiziqsdk-android-track
git checkout "%1"
git pull
cd ..