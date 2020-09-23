@echo off
call gradlew :myfiziqsdk:publishReleasePublicationToMavenLocal -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-cpp:publishReleasePublicationToMavenLocal -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-input:publishReleasePublicationToMavenLocal -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-onboarding:publishReleasePublicationToMavenLocal -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-profile:publishReleasePublicationToMavenLocal -DVERSION_NUMBER="%1"
call gradlew :myfiziqsdk-android-track:publishReleasePublicationToMavenLocal -DVERSION_NUMBER="%1"
call gradlew :orm:publishPrivateReleasePublicationToMavenLocal -DVERSION_NUMBER="%1"