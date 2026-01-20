plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
    id("com.gradle.plugin-publish") version "1.3.0"
}
group = "de.felixlf"
version = "2.1"

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
    // Use compileOnly for Kotlin dependencies to defer version resolution to the consuming project
    compileOnly(platform("org.jetbrains.kotlin:kotlin-bom"))
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")

    // Testing dependencies
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Gradle TestKit for integration tests
    testImplementation(gradleTestKit())
}
kotlin {
    jvmToolchain(17)  // Use Java 17 to compile
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"  // Generate Java 11 compatible bytecode
    }
}

tasks.withType<JavaCompile>().configureEach {
    targetCompatibility = "11"  // Generate Java 11 compatible bytecode for Java files
    sourceCompatibility = "11"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
