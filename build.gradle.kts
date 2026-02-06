import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

group = property("GROUP").toString()
version = property("VERSION_NAME").toString()

android {
    namespace = "app.whisperian.expandabletext"
    resourcePrefix = "expandable_text_"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        // Newer AGP versions may default to requiring consumers to use compileSdk >= ours unless overridden.
        // This library does not rely on API 36-only resources, so allow consumers still on 34+.
        aarMetadata {
            minCompileSdk = libs.versions.androidConsumerMinCompileSdk.get().toInt()
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        // Target Java 8 bytecode for maximum consumer compatibility.
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
    }

    lint {
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
    }
}

kotlin {
    compilerOptions {
        // Keep Kotlin bytecode target aligned with Java compatibility.
        jvmTarget = JvmTarget.JVM_1_8
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    // Compose dependencies
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    // Test dependencies
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

mavenPublishing {
    // Newer gradle-maven-publish versions publish via Sonatype Central Portal.
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}
