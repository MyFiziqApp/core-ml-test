@echo off
call gradlew :myfiziqsdk:publishDebugPublicationToMavenLocal -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-cpp:publishDebugPublicationToMavenLocal -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-input:publishDebugPublicationToMavenLocal -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-onboarding:publishDebugPublicationToMavenLocal -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-profile:publishDebugPublicationToMavenLocal -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-track:publishDebugPublicationToMavenLocal -DVERSION_NUMBER="%1"
call gradlew :orm:publishPrivateDebugPublicationToMavenLocal -DVERSION_NUMBER="%1"