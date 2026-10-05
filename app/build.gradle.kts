plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "in.vageesh.smsledger"
    compileSdk = 35

    defaultConfig {
        applicationId = "in.vageesh.smsledger"
        minSdk = 26
        targetSdk = 34
        versionCode = 67
        versionName = "1.72"
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
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("com.google.android.gms:play-services-nearby:19.3.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Firebase: Auth now, Firestore joins in the next step once sign-in is confirmed working.
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Google Sign-In via the modern Credential Manager API (replaces the older, deprecated
    // GoogleSignInClient). This is what talks to google-services.json's "web" OAuth client.
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}
