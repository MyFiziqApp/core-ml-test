@echo off
cd myfiziq-android
git pull origin "%1"
cd ..\myfiziq-sdk-android
git pull origin "%1"
cd ..\myfiziqsdk-android-cpp
git pull origin "%1"
cd ..\myfiziqsdk-android-cpp\src\main\cpp\myfiziq-sdk-cpp-utils
git pull origin "%1"
cd ..\..\..\..\..\myfiziqsdk-android-input
git pull origin "%1"
cd ..\myfiziqsdk-android-onboarding
git pull origin "%1"
cd ..\myfiziqsdk-android-profile
git pull origin "%1"
cd ..\myfiziqsdk-android-track
git pull origin "%1"
cd ..