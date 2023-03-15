plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "net.phbwt.paperwork"
    compileSdk = 33
    buildToolsVersion = "33.0.2"

    defaultConfig {
        applicationId = "net.phbwt.paperwork"
        minSdk = 24
        targetSdk = 33
        versionCode = 32
        versionName = "0.$versionCode"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.schemaLocation", "$projectDir/schemas")
            }
        }

        vectorDrawables {
            useSupportLibrary = true
        }
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

    composeOptions {
        // https://developer.android.com/jetpack/androidx/releases/compose-kotlin
        kotlinCompilerExtensionVersion = "1.4.3"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // https://developer.android.com/jetpack/androidx/versions
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.core:core-splashscreen:1.0.0")

    // kotlinx
    // https://github.com/Kotlin/kotlinx.collections.immutable/blob/master/CHANGELOG.md
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.5")

    // Saved state module for ViewModel
    // https://developer.android.com/jetpack/androidx/releases/lifecycle#groovy
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:${Versions.lifecycle}")

    // compose
    // https://developer.android.com/jetpack/compose/bom/bom-mapping
    implementation(platform("androidx.compose:compose-bom:2023.01.00"))
//    api(platform("dev.chrisbanes.compose:compose-bom:2023.02.00-beta01"))

    implementation("androidx.activity:activity-compose:1.6.1")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.accompanist:accompanist-flowlayout:${Versions.accompanist}")
    implementation("com.google.accompanist:accompanist-systemuicontroller:${Versions.accompanist}")

    // navigation
    implementation("androidx.navigation:navigation-compose:${Versions.navigation}")
    implementation("androidx.hilt:hilt-navigation-compose:${Versions.hiltNavigationCompose}")

    // pager
    implementation("com.google.accompanist:accompanist-pager:${Versions.accompanist}")
    // copy from https://github.com/google/accompanist/tree/main/pager-indicators/src/main/java/com/google/accompanist/pager
    // cf https://github.com/google/accompanist/issues/1076
    // implementation("com.google.accompanist:accompanist-pager-indicators:${Versions.accompanist}")

    // https://github.com/coil-kt/coil/blob/main/CHANGELOG.md
    implementation("io.coil-kt:coil-compose:2.2.2")

    // room
    implementation("androidx.room:room-runtime:${Versions.room}")
    kapt("androidx.room:room-compiler:${Versions.room}")
    implementation("androidx.room:room-ktx:${Versions.room}")

    // hilt
    implementation("com.google.dagger:hilt-android:${Versions.hiltDagger}")
    kapt("com.google.dagger:hilt-compiler:${Versions.hiltDagger}")

    // OkHttp
    // https://square.github.io/okhttp/changelogs/changelog/
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.10.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:okhttp-tls")
    implementation("com.squareup.okhttp3:logging-interceptor")

    // workmanager + hilt and coroutine integration
    implementation("androidx.work:work-runtime-ktx:2.8.0")
    implementation("androidx.hilt:hilt-work:${Versions.hiltBase}")
    kapt("androidx.hilt:hilt-compiler:${Versions.hiltBase}")
    // https://github.com/gildor/kotlin-coroutines-okhttp/blob/master/CHANGELOG.md
    implementation("ru.gildor.coroutines:kotlin-coroutines-okhttp:1.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}