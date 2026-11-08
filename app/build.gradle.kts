plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.marcelo.huba50"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.marcelo.huba50"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
