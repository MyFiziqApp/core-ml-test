@echo off
cd myfiziq-android
git clean -f
git reset --hard
cd ..\myfiziq-sdk-android
git clean -f
git reset --hard
cd ..\myfiziqsdk-android-cpp
git clean -f
git reset --hard
cd ..\myfiziqsdk-android-cpp\src\main\cpp\myfiziq-sdk-cpp-utils
git clean -f
git reset --hard
cd ..\..\..\..\..\myfiziqsdk-android-input
git clean -f
git reset --hard
cd ..\myfiziqsdk-android-onboarding
git clean -f
git reset --hard
cd ..\myfiziqsdk-android-profile
git clean -f
git reset --hard
cd ..\myfiziqsdk-android-track
git clean -f
git reset --hard
cd ..