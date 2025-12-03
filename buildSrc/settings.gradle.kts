@file:Suppress("UnstableApiUsage")

rootProject.name = "buildSrc"

// You need to add the repositories, where your dependencies and plugins are located. Version 2.0 of this plugin
//  does NOT add them anymore.
dependencyResolutionManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

pluginManagement {
    // Example on how to include the convention plugin in source format, in case you don't use maven central plugins
    includeBuild("../libs-catalog-generator")
}


// In your project, the plugin SHOULD be applied inside the settings.gradle.kts file and not in the build.gradle.kts file
plugins {
    // Due that we apply this plugin as source format, we do not need to add a version
    id("de.felixlf.libs-catalog-generator")

    // In case you add the plugin using the gradlePluginPortal, you need to add the plugin using the version
    // id("de.felixlf.libs-catalog-generator") version "2.0"
}
