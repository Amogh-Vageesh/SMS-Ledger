plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "in.vageesh.smsledger"
    compileSdk = 35

    defaultConfig {
        applicationId = "in.vageesh.smsledger"
        minSdk = 26
        targetSdk = 34
        versionCode = 10
        versionName = "1.9"
    }

    // A fixed key kept in this (private) repo, so every new build installs as an update
    // over the previous one instead of needing an uninstall.
    signingConfigs {
        create("ledger") {
            storeFile = file("release.keystore")
            storePassword = "smsledger123"
            keyAlias = "smsledger"
            keyPassword = "smsledger123"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("ledger")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("com.google.android.gms:play-services-nearby:19.3.0")
}
