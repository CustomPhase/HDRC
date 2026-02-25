plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.customphase.hdrezkacustom"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.customphase.hdrezkacustom"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("org.jsoup:jsoup:1.15.4") // Парсинг HTML
    implementation("com.squareup.okhttp3:okhttp:4.10.0") // Сетевые запросы
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4") // Асинхронность

    implementation("com.google.android.exoplayer:exoplayer-core:2.18.7")
    implementation("com.google.android.exoplayer:exoplayer-hls:2.18.7") // Для .m3u8
    implementation("com.google.android.exoplayer:exoplayer-ui:2.18.7")   // Стандартные кнопки управления
    implementation("com.google.android.exoplayer:extension-okhttp:2.18.7")
}