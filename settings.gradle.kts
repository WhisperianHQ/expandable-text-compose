pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "expandable-text-compose"

// LeakCanary enables this for strongly-typed `projects.*` accessors in large builds.
// It's harmless here and keeps our build aligned with that convention.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":sample")
include(":benchmark")
