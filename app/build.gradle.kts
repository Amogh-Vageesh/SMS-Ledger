plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'org.jetbrains.kotlin.plugin.compose'
    id 'com.google.gms.google-services'
}

android {
    namespace 'com.example.smsledger' // Replace with your actual package name
    compileSdk 35

    defaultConfig {
        applicationId "com.example.smsledger"
        minSdk 24
        targetSdk 35
        versionCode 1
        versionName "1.0"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures {
        compose true
    }
}

dependencies {
    // Add your app dependencies here
}