@echo off
cd myfiziq-android
git checkout -b "%1"
git push --set-upstream origin "%1"
cd ..\myfiziq-sdk-android
git checkout -b "%1"
git push --set-upstream origin "%1"
cd ..\myfiziqsdk-android-cpp
git checkout -b "%1"
cd ..\myfiziqsdk-android-cpp\src\main\cpp\myfiziq-sdk-cpp-utils
git checkout -b "%1"
git push --set-upstream origin "%1"
cd ..\..\..\..\..\myfiziqsdk-android-cpp
git commit -am "create branch %1"
git push --set-upstream origin "%1"
cd ..\myfiziqsdk-android-input
git checkout -b "%1"
git push --set-upstream origin "%1"
cd ..\myfiziqsdk-android-onboarding
git checkout -b "%1"
git push --set-upstream origin "%1"
cd ..\myfiziqsdk-android-profile
git checkout -b "%1"
git push --set-upstream origin "%1"
cd ..\myfiziqsdk-android-track
git checkout -b "%1"
git push --set-upstream origin "%1"
cd ..
git checkout -b "%1"
git commit -am "create branch %1"
git push --set-upstream origin "%1"
