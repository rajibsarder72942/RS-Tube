plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.rstube.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.rstube.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 4
        versionName = "0.2.2"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging {
        jniLibs.useLegacyPackaging = true
        resources.excludes += setOf("META-INF/DEPENDENCIES", "META-INF/INDEX.LIST")
    }
    buildFeatures { buildConfig = true }
    signingConfigs {
        if (!System.getenv("RSTUBE_KEYSTORE_FILE").isNullOrBlank()) {
            create("updates") {
                storeFile = file(System.getenv("RSTUBE_KEYSTORE_FILE"))
                storePassword = System.getenv("RSTUBE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RSTUBE_KEY_ALIAS")
                keyPassword = System.getenv("RSTUBE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (signingConfigs.findByName("updates") != null) signingConfig = signingConfigs.getByName("updates")
        }
    }
    lint { abortOnError = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
    testImplementation("junit:junit:4.13.2")
}
