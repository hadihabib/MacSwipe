plugins {
    id("com.android.application")
}

android {
    namespace = "com.macswipe.remote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.macswipe.remote"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
