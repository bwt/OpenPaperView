

// https://github.com/google/ksp/releases
plugins {
    id("com.google.devtools.ksp") version "${Versions.kotlin}-1.0.13" apply false
}

buildscript {

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${Versions.hiltDagger}")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}