plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.mrguven.smartrecycling"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mrguven.smartrecycling"
        minSdk = 27
        targetSdk = 34
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
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    ndkVersion = "26.1.10909125"

}

dependencies {
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    val lifecycleVersion = "2.7.0"
    val roomVersion = "2.5.2"

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Navigation library
    val navVersion = "2.7.6"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    // CameraX core library
    val cameraxVersion = "1.4.0-alpha03"
    implementation("androidx.camera:camera-core:$cameraxVersion")

    // CameraX Camera2 extensions
    implementation("androidx.camera:camera-camera2:$cameraxVersion")

    // CameraX Lifecycle library
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")

    // CameraX View class
    implementation ("androidx.camera:camera-view:$cameraxVersion")

    //WindowManager
    implementation ("androidx.window:window:1.3.0-alpha01")

    // Unit testing
    testImplementation ("androidx.test.ext:junit:1.1.5")
    testImplementation ("androidx.test:rules:1.5.0")
    testImplementation ("androidx.test:runner:1.5.2")
    testImplementation ("androidx.test.espresso:espresso-core:3.5.1")
    testImplementation ("org.robolectric:robolectric:4.11.1")

    // Instrumented testing
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    androidTestImplementation ("androidx.test:core:1.5.0")
    androidTestImplementation ("androidx.test:rules:1.5.0")
    androidTestImplementation ("androidx.test:runner:1.5.2")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")

    // Tensorflow
    implementation ("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    // Import the GPU delegate plugin Library for GPU inference
    implementation ("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4")
    implementation ("org.tensorflow:tensorflow-lite-gpu:2.14.0")

    // Google Maps
    implementation ("com.google.android.gms:play-services-maps:18.2.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")

    implementation ("com.airbnb.android:lottie:4.2.0")

    // Dagger-Hilt
    implementation("com.google.dagger:hilt-android:2.47")
    kapt("com.google.dagger:hilt-android-compiler:2.47")



    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    // LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    // Saved state module for ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycleVersion")


    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")


    implementation ("com.github.AnyChart:AnyChart-Android:1.1.5")

}