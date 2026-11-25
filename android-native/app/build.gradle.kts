plugins {
    id("com.android.application")
}

android {
    namespace = "com.gabigtr123.pixelstride.nativealpha"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.gabigtr123.pixelstride.nativealpha"
        minSdk = 26
        targetSdk = 37
        versionCode = 40014
        versionName = "4.0.0-beta.5"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
}
