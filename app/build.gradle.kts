plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services")

}


android {
    namespace = "com.example.conecta4"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.conecta4"
        minSdk = 24
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
    buildFeatures {
        compose = true
    }
}

dependencies {

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.0") // O la versión más reciente



    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0") // Or the latest version
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0") // Or the latest stable version
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // Or the latest stable version
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0") // For compose-specific ViewModel integration

    implementation("androidx.compose.material:material-icons-extended")
    // Para Google Sign-In (si planeas usarlo)
    implementation("com.google.android.gms:play-services-auth:21.0.0") //

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))


    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")


    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries

    // Core Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Jetpack Compose UI y Material3
    implementation("androidx.compose.material3:material3:1.3.2") // ¡Esta es la última versión estable!
    implementation("androidx.compose.ui:ui:1.8.0")
    implementation("androidx.compose.foundation:foundation:1.8.0")
    implementation("androidx.compose.runtime:runtime:1.8.0")

    // Activity Compose
    implementation ("androidx.activity:activity-compose:1.9.0") // O la versión más reciente



    // Kotlin y otras dependencias esenciales
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.ads.mobile.sdk)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.animation.core.lint)
    implementation(libs.firebase.auth.ktx)

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debugging y toolings
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
