plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
    id("com.gradle.plugin-publish") version "1.3.0"
}
group = "de.felixlf"
version = "1.0"

gradlePlugin {
    plugins {
        vcsUrl = "https://github.com/felix-leyva/catalog-generator"
        website = "https://github.com/felix-leyva/catalog-generator"
        create("libs-catalog-generator") {
            id = "de.felixlf.libs-catalog-generator"
            displayName = "Libs catalog generator for buildSrc and convention plugins"
            tags = listOf("catalog", "generator", "buildSrc", "convention")
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
