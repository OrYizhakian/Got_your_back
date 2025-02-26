

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.gms.google-services") // for Firebase
    id("kotlin-parcelize")
    id ("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")

}

android {
    namespace = "com.example.GotYourBack"
    compileSdk = 35

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.GotYourBack"
        minSdk = 29
        targetSdk = 35
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

}

dependencies {
    implementation(libs.androidx.media3.common.ktx)
    //implementation(libs.firebase.firestore)
    val roomVersion = "2.6.1"

    // Room database
    implementation("androidx.room:room-runtime:$roomVersion") // Runtime library
    kapt("androidx.room:room-compiler:$roomVersion") // Annotation processor
    implementation("androidx.room:room-ktx:$roomVersion") // Kotlin extensions

    // Firebase dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database) // Realtime Database
    implementation(libs.google.firebase.firestore.ktx) // Firestore
    implementation ("com.google.firebase:firebase-auth:23.1.0")


    // Core libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Unit and Instrumented Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation ("com.google.android.gms:play-services-maps:17.0.0")

    implementation ("com.google.maps.android:android-maps-utils:2.2.5")

    implementation( "com.squareup.okhttp3:okhttp:4.10.0")


}

// run Google Services Plugin
apply(plugin = "com.google.gms.google-services")
