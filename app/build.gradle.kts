plugins {
    id("com.android.application")
}

android {
    namespace = "fr.monrythmesleeve.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.monrythmesleeve.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
