plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dagger.hilt)
    kotlin("kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.catlovercompose"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.catlovercompose"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        mlModelBinding = true  // For TensorFlow Lite
    }

    // Prevent compression of tflite files
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    aaptOptions {
        noCompress("tflite")
        noCompress("lite")
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Hilt for Dependency Injection
    implementation(libs.dagger.hilt.android)
    kapt(libs.dagger.hilt.compiler)
    implementation(libs.dagger.hilt.compose)

    // Coil for Image Loading
    implementation(libs.coil)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-analytics:23.0.0")
    implementation("com.google.firebase:firebase-auth:24.0.1")
    implementation("com.google.firebase:firebase-database:22.0.1")
    implementation("com.google.firebase:firebase-storage:22.0.1")
    implementation("com.google.firebase:firebase-firestore:26.0.2")
    implementation("com.google.firebase:firebase-messaging:23.4.0")
    implementation(libs.firebase.crashlytics.buildtools)

    // ===== TensorFlow Lite for YOLO Detection =====
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.14.0")

    // ===== CameraX Dependencies =====
    val cameraxVersion = "1.4.0-alpha04"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ===== Permissions Handling =====
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // AndroidX Preference (for OSMDroid configuration)
    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.coil-kt:coil-compose:2.5.0")

    implementation("androidx.datastore:datastore-preferences:1.0.0")

}

// Allow Gradle to resolve kapt correctly
kapt {
    correctErrorTypes = true
}

