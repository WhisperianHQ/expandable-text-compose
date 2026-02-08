import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.whisperian.expandabletext.benchmark"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    // Macrobenchmark tests require API 23+.
    defaultConfig {
        minSdk = 23
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Allow running on emulators in dev; physical devices are recommended for stable numbers.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
        // Some emulators/devices refuse killing the system perfetto process; avoid hard-failing on cleanup.
        testInstrumentationRunnerArguments["androidx.benchmark.killExistingPerfettoRecordings"] = "false"
        // Prefer SDK tracing path on API 29+ to avoid spawning/stopping perfetto processes.
        testInstrumentationRunnerArguments["androidx.benchmark.perfettoSdkTracing.enable"] = "true"
    }

    targetProjectPath = ":sample"
    // Required for Macrobenchmark: the benchmark test APK must instrument itself, not the target app.
    // Otherwise Macrobenchmark can't kill/compile/launch the target process.
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        // The benchmark APK must be debuggable to run instrumentation.
        create("benchmark") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.ext.junit)
}
