@echo off
call gradlew :myfiziqsdk:publishReleasePublicationToPublicReleaseRepository -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-cpp:publishReleasePublicationToPublicReleaseRepository -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-input:publishReleasePublicationToPublicReleaseRepository -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-onboarding:publishReleasePublicationToPublicReleaseRepository -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-profile:publishReleasePublicationToPublicReleaseRepository -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-track:publishReleasePublicationToPublicReleaseRepository -DVERSION_NUMBER="%1"
call gradlew :orm:publishPublicReleasePublicationToPublicReleaseRepository -DVERSION_NUMBER="%1"