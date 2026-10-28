plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.sowarapp.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sowarapp.android"
        // Android 7.0+. Covers almost every active Android phone.
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"

        // Where the app downloads the list of categories and images: the R2 bucket, which the
        // admin page (admin/) updates. A copy is also bundled in the APK, so the app works on
        // first launch offline. Change the domain here when the bucket gets its own domain.
        buildConfigField(
            "String",
            "CATALOG_URL",
            "\"https://pub-c9090d43d21447cc85a1e0804f15d18b.r2.dev/catalog/catalog.json\"",
        )
    }

    sourceSets["main"].assets.srcDir(rootProject.file("catalog"))

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
