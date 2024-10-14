@file:Suppress("UnstableApiUsage")

rootProject.name = "buildSrc"

pluginManagement {
    // Example on how to include the convention plugin in source format
    includeBuild("../libs-catalog-generator")
}

// The plugin SHOULD be applied inside the settings.gradle.kts file and not in the build.gradle.kts file
plugins {
    id("de.felixlf.libs-catalog-generator")
}
