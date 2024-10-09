plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
    id("com.gradle.plugin-publish") version "1.2.1"
}
group = "de.felixlf"
version = "1.0"

gradlePlugin {
    plugins {
        create("libs-catalog-generator") {
            id = "de.felixlf.libs-catalog-generator"
            displayName = "Libs catalog generator for buildSrc and convention plugins"
            description =
                "This plugin generates a file to access from a buildSrc or convention plugin source files in a type-safe way, the libs, plugins and versions from a version catalog"
            implementationClass = "CatalogGeneratorPlugin"
        }

    }
}

dependencies {
    implementation("com.squareup:kotlinpoet:1.18.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
}
