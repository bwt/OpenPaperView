plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.parcelize)
    alias(libs.plugins.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "net.phbwt.paperwork"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.phbwt.paperwork"
        minSdk = 26
        targetSdk = 36
        versionCode = 1003000
        versionName = "1.3.0"

        resourceConfigurations += arrayOf("en", "fr")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        setProperty("archivesBaseName", "${applicationId}_${versionName}")
    }

    signingConfigs {
        create("release") {
            storeFile = file(File(System.getenv("ANDROID_KEYSTORE") ?: ""))
            storePassword = System.getenv("ANDROID_PASSWORD")
            keyAlias = "OpenPaperView"
            keyPassword = System.getenv("ANDROID_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
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
        buildConfig = true
    }

//    composeCompiler {
//        enableStrongSkippingMode = true
//    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.generateKotlin", "true")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)

    // Saved state module for ViewModel
    // https://developer.android.com/jetpack/androidx/releases/lifecycle#groovy
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)

    // compose
    // https://developer.android.com/jetpack/compose/bom/bom-mapping
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // accompanist
    implementation(libs.accompanist.permissions)

    // navigation
    implementation(libs.hilt.navigation.compose)
    // Provided by Compose Destinations
    // implementation(libs.androidx.navigation.compose)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)

    // Compose Destinations
    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.dagger.hilt)
    // both hilt-compiler are required
    // cf https://github.com/google/dagger/issues/4058#issuecomment-1739045490
    ksp(libs.dagger.compiler)
    ksp(libs.hilt.compiler)

    // OkHttp
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.okhttp)
    implementation(libs.okhttp.tls)
    implementation(libs.okhttp.logging.interceptor)

    // workmanager + hilt and coroutine integration
    implementation(libs.workmanager.runtime)
    implementation(libs.hilt.workmanager)

    // https://github.com/gildor/kotlin-coroutines-okhttp/blob/master/CHANGELOG.md
    implementation(libs.gildor.coroutines.okhttp)

    implementation(libs.zxing.journeyapps)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
