@file:Suppress("UnstableApiUsage")

rootProject.name = "buildSrc"

pluginManagement {
    // Example on how to include the convention plugin in source format, in case you dont use maven central plugins
    includeBuild("../libs-catalog-generator")
}

plugins {
    id("de.felixlf.libs-catalog-generator")

    // In your project, the plugin SHOULD be applied inside the settings.gradle.kts file and not in the build.gradle.kts file
    // id("de.felixlf.libs-catalog-generator") version "1.2" apply false

}
