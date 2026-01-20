import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for the Catalog Generator Plugin using Gradle TestKit.
 *
 * These tests verify that the plugin works correctly in real Gradle builds,
 * including code generation, caching, and task dependencies.
 */
@DisplayName("Catalog Generator Plugin Integration Tests")
class CatalogGeneratorPluginIntegrationTest {

    @TempDir
    lateinit var testProjectDir: File

    private fun File.createCatalogFile(content: String = DEFAULT_CATALOG): File {
        return File(this, "gradle/libs.versions.toml").apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }

    private fun File.createSettingsFile(pluginConfig: String = ""): File {
        return File(this, "settings.gradle.kts").apply {
            writeText(
                """
                @file:Suppress("UnstableApiUsage")

                rootProject.name = "test-project"

                plugins {
                    id("de.felixlf.libs-catalog-generator")
                }

                $pluginConfig

                dependencyResolutionManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()

                    }
                }
                """.trimIndent()
            )
        }
    }

    private fun File.createBuildFile(): File {
        return File(this, "build.gradle.kts").apply {
            writeText(
                """
                plugins {
                    `kotlin-dsl`
                }
                """.trimIndent()
            )
        }
    }

    @Nested
    @DisplayName("Plugin Application")
    inner class PluginApplication {

        @Test
        @DisplayName("should apply plugin with gradle 9.2.1 successfully to settings")
        fun `plugin applies to settings with gradle 9-2-1 successfully`() {
            testProjectDir.createCatalogFile()
            testProjectDir.createSettingsFile()
            testProjectDir.createBuildFile()

            val result = try {
                GradleRunner.create()
                    .withProjectDir(testProjectDir)
                    .withArguments("tasks", "--stacktrace")
                    .withPluginClasspath()
                    .withGradleVersion("9.2.1")
                    .build()
            } catch (e: Exception) {
                println("Build failed with error:")
                println(e.message)
                e.printStackTrace()
                throw e
            }
            assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
            assertTrue(result.output.contains("generateTypeSafeCatalog"))
        }
        @Test
        @DisplayName("should apply plugin with gradle 8-14-3 successfully to settings")
        fun `plugin applies to settings with gradle 8-14-3 successfully`() {
            testProjectDir.createCatalogFile()
            testProjectDir.createSettingsFile()
            testProjectDir.createBuildFile()

            val result = try {
                GradleRunner.create()
                    .withProjectDir(testProjectDir)
                    .withArguments("tasks", "--stacktrace")
                    .withPluginClasspath()
                    .withGradleVersion("8.14.3")
                    .build()
            } catch (e: Exception) {
                println("Build failed with error:")
                println(e.message)
                e.printStackTrace()
                throw e
            }
            assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
            assertTrue(result.output.contains("generateTypeSafeCatalog"))
        }

        @Test
        @DisplayName("should apply plugin with gradle 7-6-6 successfully to settings")
        fun `plugin applies to settings with gradle 7-6-6 successfully`() {
            testProjectDir.createCatalogFile()
            testProjectDir.createSettingsFile()
            testProjectDir.createBuildFile()

            val result = try {
                GradleRunner.create()
                    .withProjectDir(testProjectDir)
                    .withArguments("tasks", "--stacktrace")
                    .withPluginClasspath()
                    .withGradleVersion("7.6.6")
                    .build()
            } catch (e: Exception) {
                println("Build failed with error:")
                println(e.message)
                e.printStackTrace()
                throw e
            }
            assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
            assertTrue(result.output.contains("generateTypeSafeCatalog"))
        }

        @Test
        @DisplayName("should apply plugin with gradle 7-0 successfully to settings")
        fun `plugin applies to settings with gradle 7-0 successfully`() {
            // Note: Gradle 7.0-7.1 doesn't support [plugins] section in TOML catalogs
            // Plugin support was added in Gradle 7.2
            testProjectDir.createCatalogFile(DEFAULT_CATALOG_NO_PLUGINS)
            testProjectDir.createSettingsFile()
            testProjectDir.createBuildFile()

            val result = try {
                GradleRunner.create()
                    .withProjectDir(testProjectDir)
                    .withArguments("tasks", "--stacktrace")
                    .withPluginClasspath()
                    .withGradleVersion("7.0")
                    .build()
            } catch (e: Exception) {
                println("Build failed with error:")
                println(e.message)
                e.printStackTrace()
                throw e
            }
            assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
            assertTrue(result.output.contains("generateTypeSafeCatalog"))
        }

        @Test
        @DisplayName("should apply plugin successfully to settings")
        fun `plugin applies to settings successfully`() {
            testProjectDir.createCatalogFile()
            testProjectDir.createSettingsFile()
            testProjectDir.createBuildFile()

            val result = try {
                GradleRunner.create()
                    .withProjectDir(testProjectDir)
                    .withArguments("tasks", "--stacktrace")
                    .withPluginClasspath()
                    .build()
            } catch (e: Exception) {
                println("Build failed with error:")
                println(e.message)
                e.printStackTrace()
                throw e
            }
            assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
            assertTrue(result.output.contains("generateTypeSafeCatalog"))
        }

        @Test
        @DisplayName("should create generateTypeSafeCatalog task")
        fun `creates generateTypeSafeCatalog task`() {
            testProjectDir.createCatalogFile()
            testProjectDir.createSettingsFile()
            testProjectDir.createBuildFile()

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--all", "--stacktrace")
                .withPluginClasspath()
                .build()

            assertTrue(result.output.contains("generateTypeSafeCatalog"))
        }
    }

    @Nested
    @DisplayName("Code Generation")
    inner class CodeGeneration {

        @Test
        @DisplayName("should generate GeneratedCatalog.kt file")
        fun `generates GeneratedCatalog file`() {
            testProjectDir.createCatalogFile()
            testProjectDir.createSettingsFile()
            testProjectDir.createBuildFile()

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTypeSafeCatalog", "--stacktrace")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":generateTypeSafeCatalog")?.outcome)

            val generatedFile = File(testProjectDir, "build/generated-sources/kotlin-dsl-plugins/kotlin/GeneratedCatalog.kt")
            assertTrue(generatedFile.exists(), "GeneratedCatalog.kt should exist")
            assertTrue(generatedFile.length() > 0, "GeneratedCatalog.kt should not be empty")
        }

        @Test
        @DisplayName("should generate code with library accessors")
        fun `generated code contains library accessors`() {
            testProjectDir.createCatalogFile(
                """
                [versions]
                kotlin = "1.9.0"

                [libraries]
                kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
                androidx-core = "androidx.core:core-ktx:1.12.0"
                """.trimIndent()
            )
            testProjectDir.createSettingsFile()
            testProjectDir.createBuildFile()

            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTypeSafeCatalog", "--stacktrace")
                .withPluginClasspath()
                .build()

            val generatedFile = File(testProjectDir, "build/generated-sources/kotlin-dsl-plugins/kotlin/GeneratedCatalog.kt")
            val content = generatedFile.readText()

            assertTrue(content.contains("class Libraries"), "Should contain Libraries class")
            assertTrue(content.contains("kotlinStdlib"), "Should contain kotlinStdlib accessor")
            assertTrue(content.contains("androidxCore"), "Should contain androidxCore accessor")
        }

        @Test
        @DisplayName("should generate code with version accessors")
        fun `generated code contains version accessors`() {
            testProjectDir.createCatalogFile(
                """
                [versions]
                kotlin = "1.9.0"
                agp = "8.1.0"
                """.trimIndent()
            )
            testProjectDir.createSettingsFile()
            testProjectDir.createBuildFile()

            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTypeSafeCatalog", "--stacktrace")
                .withPluginClasspath()
                .build()

            val generatedFile = File(testProjectDir, "build/generated-sources/kotlin-dsl-plugins/kotlin/GeneratedCatalog.kt")
            val content = generatedFile.readText()

            assertTrue(content.contains("class Versions"), "Should contain Versions class")
            assertTrue(content.contains("kotlin"), "Should contain kotlin version accessor")
            assertTrue(content.contains("agp"), "Should contain agp version accessor")
        }
    }

    @Nested
    @DisplayName("Different Catalog Configurations")
    inner class DifferentCatalogConfigurations {

        @Test
        @DisplayName("should work with custom catalog name")
        fun `works with custom catalog name`() {
            testProjectDir.createCatalogFile()
            testProjectDir.createSettingsFile(
                """
                catalogGenerator {
                    catalogName.set("myLibs")
                }
                """.trimIndent()
            )
            testProjectDir.createBuildFile()

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTypeSafeCatalog", "--stacktrace")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":generateTypeSafeCatalog")?.outcome)

            val generatedFile = File(testProjectDir, "build/generated-sources/kotlin-dsl-plugins/kotlin/GeneratedCatalog.kt")
            val content = generatedFile.readText()
            assertTrue(content.contains("myLibs"), "Should contain custom catalog name")
        }

        @Test
        @DisplayName("should work with custom catalog location")
        fun `works with custom catalog location`() {
            File(testProjectDir, "config/dependencies.toml").apply {
                parentFile.mkdirs()
                writeText(DEFAULT_CATALOG)
            }

            testProjectDir.createSettingsFile(
                """
                catalogGenerator {
                    catalogTomlLocation.set("config/dependencies.toml")
                }
                """.trimIndent()
            )
            testProjectDir.createBuildFile()

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTypeSafeCatalog", "--stacktrace")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":generateTypeSafeCatalog")?.outcome)

            val generatedFile = File(testProjectDir, "build/generated-sources/kotlin-dsl-plugins/kotlin/GeneratedCatalog.kt")
            assertTrue(generatedFile.exists())
        }

        @Test
        @DisplayName("should work with absolute path to catalog file")
        fun `works with absolute path to catalog file`() {
            // Create catalog file in a custom location
            val catalogFile = File(testProjectDir, "custom-location/libs.versions.toml").apply {
                parentFile.mkdirs()
                writeText(DEFAULT_CATALOG)
            }

            // Use absolute path in configuration
            testProjectDir.createSettingsFile(
                """
                catalogGenerator {
                    catalogTomlLocation.set("${catalogFile.absolutePath.replace("\\", "\\\\")}")
                }
                """.trimIndent()
            )
            testProjectDir.createBuildFile()

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTypeSafeCatalog", "--stacktrace")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":generateTypeSafeCatalog")?.outcome)

            val generatedFile = File(testProjectDir, "build/generated-sources/kotlin-dsl-plugins/kotlin/GeneratedCatalog.kt")
            assertTrue(generatedFile.exists(), "GeneratedCatalog.kt should exist")
        }
    }

    @Nested
    @DisplayName("Nested Project Structure (build-logic pattern)")
    inner class NestedProjectStructure {

        @Test
        @DisplayName("should work with catalog in parent directory using relative path")
        fun `works with catalog in parent directory using relative path`() {
            // Simulate a build-logic/convention structure where:
            // - Main project root has gradle/libs.versions.toml
            // - build-logic/convention is a separate Gradle build that needs to access it

            // Create the main project catalog at parent level
            val parentDir = testProjectDir
            File(parentDir, "gradle/libs.versions.toml").apply {
                parentFile.mkdirs()
                writeText(DEFAULT_CATALOG)
            }

            // Create the nested build-logic directory
            val buildLogicDir = File(parentDir, "build-logic").apply { mkdirs() }

            // Create settings for build-logic that references parent catalog with relative path
            File(buildLogicDir, "settings.gradle.kts").writeText(
                """
                @file:Suppress("UnstableApiUsage")

                rootProject.name = "build-logic"

                plugins {
                    id("de.felixlf.libs-catalog-generator")
                }

                catalogGenerator {
                    // Use relative path to access catalog in parent directory
                    catalogTomlLocation.set("../gradle/libs.versions.toml")
                }

                dependencyResolutionManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }
                """.trimIndent()
            )

            File(buildLogicDir, "build.gradle.kts").writeText(
                """
                plugins {
                    `kotlin-dsl`
                }
                """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(buildLogicDir)
                .withArguments("generateTypeSafeCatalog", "--stacktrace")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":generateTypeSafeCatalog")?.outcome)

            val generatedFile = File(buildLogicDir, "build/generated-sources/kotlin-dsl-plugins/kotlin/GeneratedCatalog.kt")
            assertTrue(generatedFile.exists(), "GeneratedCatalog.kt should exist")
        }

        @Test
        @DisplayName("should work with catalog in parent directory using absolute path via file()")
        fun `works with catalog in parent directory using absolute path`() {
            // Simulate a build-logic/convention structure with absolute path

            // Create the main project catalog at parent level
            val parentDir = testProjectDir
            val catalogFile = File(parentDir, "gradle/libs.versions.toml").apply {
                parentFile.mkdirs()
                writeText(DEFAULT_CATALOG)
            }

            // Create the nested build-logic directory
            val buildLogicDir = File(parentDir, "build-logic").apply { mkdirs() }

            // Create settings for build-logic that references parent catalog with absolute path
            File(buildLogicDir, "settings.gradle.kts").writeText(
                """
                @file:Suppress("UnstableApiUsage")

                rootProject.name = "build-logic"

                plugins {
                    id("de.felixlf.libs-catalog-generator")
                }

                catalogGenerator {
                    // Use absolute path (simulating what user would do with file().absolutePath)
                    catalogTomlLocation.set("${catalogFile.absolutePath.replace("\\", "\\\\")}")
                }

                dependencyResolutionManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }
                """.trimIndent()
            )

            File(buildLogicDir, "build.gradle.kts").writeText(
                """
                plugins {
                    `kotlin-dsl`
                }
                """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(buildLogicDir)
                .withArguments("generateTypeSafeCatalog", "--stacktrace")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":generateTypeSafeCatalog")?.outcome)

            val generatedFile = File(buildLogicDir, "build/generated-sources/kotlin-dsl-plugins/kotlin/GeneratedCatalog.kt")
            assertTrue(generatedFile.exists(), "GeneratedCatalog.kt should exist")
        }

        @Test
        @DisplayName("should work with deeply nested project structure")
        fun `works with deeply nested project structure`() {
            // Simulate build-logic/convention pattern where convention is two levels down

            // Create the main project catalog at root level
            val rootDir = testProjectDir
            File(rootDir, "gradle/libs.versions.toml").apply {
                parentFile.mkdirs()
                writeText(DEFAULT_CATALOG)
            }

            // Create deeply nested convention directory
            val conventionDir = File(rootDir, "build-logic/convention").apply { mkdirs() }

            // Create settings for convention that references root catalog
            File(conventionDir, "settings.gradle.kts").writeText(
                """
                @file:Suppress("UnstableApiUsage")

                rootProject.name = "convention"

                plugins {
                    id("de.felixlf.libs-catalog-generator")
                }

                catalogGenerator {
                    // Path relative to convention dir going up two levels
                    catalogTomlLocation.set("../../gradle/libs.versions.toml")
                }

                dependencyResolutionManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }
                """.trimIndent()
            )

            File(conventionDir, "build.gradle.kts").writeText(
                """
                plugins {
                    `kotlin-dsl`
                }
                """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(conventionDir)
                .withArguments("generateTypeSafeCatalog", "--stacktrace")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":generateTypeSafeCatalog")?.outcome)

            val generatedFile = File(conventionDir, "build/generated-sources/kotlin-dsl-plugins/kotlin/GeneratedCatalog.kt")
            assertTrue(generatedFile.exists(), "GeneratedCatalog.kt should exist")
        }

        @Test
        @DisplayName("should work with file() and rootDir pattern from user issue")
        fun `works with file rootDir pattern from user issue`() {
            // This test reproduces the exact pattern from the GitHub issue:
            // catalogGenerator {
            //     val catalogFileLocal = file("$rootDir/../gradle/libs.versions.toml")
            //     catalogTomlLocation = catalogFileLocal.absolutePath
            // }

            // Create the main project catalog at parent level (simulating the real project root)
            val parentDir = testProjectDir
            File(parentDir, "gradle/libs.versions.toml").apply {
                parentFile.mkdirs()
                writeText(DEFAULT_CATALOG)
            }

            // Create the build-logic directory (this is where the plugin is applied)
            val buildLogicDir = File(parentDir, "build-logic").apply { mkdirs() }

            // Create settings using the exact pattern from the user's issue
            File(buildLogicDir, "settings.gradle.kts").writeText(
                """
                @file:Suppress("UnstableApiUsage")

                rootProject.name = "build-logic"

                plugins {
                    id("de.felixlf.libs-catalog-generator")
                }

                catalogGenerator {
                    // This mimics the user's pattern:
                    // val catalogFileLocal = file("${'$'}rootDir/../gradle/libs.versions.toml")
                    // catalogTomlLocation = catalogFileLocal.absolutePath
                    val catalogFileLocal = file("${'$'}rootDir/../gradle/libs.versions.toml")
                    println("catalogFile = ${'$'}catalogFileLocal")
                    catalogTomlLocation.set(catalogFileLocal.absolutePath)
                    println("catalogTomlLocation after setting = ${'$'}{catalogTomlLocation.get()}")
                }

                dependencyResolutionManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }
                """.trimIndent()
            )

            File(buildLogicDir, "build.gradle.kts").writeText(
                """
                plugins {
                    `kotlin-dsl`
                }
                """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(buildLogicDir)
                .withArguments("generateTypeSafeCatalog", "--stacktrace")
                .withPluginClasspath()
                .build()

            // Verify the println outputs show correct values
            assertTrue(
                result.output.contains("catalogFile = ") && result.output.contains("libs.versions.toml"),
                "Should print the catalog file path"
            )
            assertTrue(
                result.output.contains("catalogTomlLocation after setting = ") && result.output.contains("libs.versions.toml"),
                "Should print the catalogTomlLocation after setting"
            )

            assertEquals(TaskOutcome.SUCCESS, result.task(":generateTypeSafeCatalog")?.outcome)

            val generatedFile = File(buildLogicDir, "build/generated-sources/kotlin-dsl-plugins/kotlin/GeneratedCatalog.kt")
            assertTrue(generatedFile.exists(), "GeneratedCatalog.kt should exist")

            // Verify the generated file contains expected content
            val content = generatedFile.readText()
            assertTrue(content.contains("projectlibs"), "Should contain default catalog name")
        }
    }

    @Nested
    @DisplayName("Build Cache")
    inner class BuildCache {

        @Test
        @DisplayName("should use cache when inputs unchanged")
        fun `task uses cache when inputs unchanged`() {
            testProjectDir.createCatalogFile()
            testProjectDir.createSettingsFile()
            testProjectDir.createBuildFile()

            // First build
            val firstRun = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTypeSafeCatalog", "--build-cache", "--stacktrace")
                .withPluginClasspath()
                .build()

            // First run can be SUCCESS or FROM_CACHE if cache was already populated
            val firstOutcome = firstRun.task(":generateTypeSafeCatalog")?.outcome
            assertTrue(
                firstOutcome == TaskOutcome.SUCCESS || firstOutcome == TaskOutcome.FROM_CACHE,
                "First run should be SUCCESS or FROM_CACHE, but was $firstOutcome"
            )

            // Second build - should be up-to-date or from cache
            val secondRun = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTypeSafeCatalog", "--build-cache", "--stacktrace")
                .withPluginClasspath()
                .build()

            val outcome = secondRun.task(":generateTypeSafeCatalog")?.outcome
            assertTrue(
                outcome == TaskOutcome.UP_TO_DATE || outcome == TaskOutcome.FROM_CACHE,
                "Task should be UP_TO_DATE or FROM_CACHE, but was $outcome"
            )
        }

        @Test
        @DisplayName("should regenerate when catalog changes")
        fun `regenerates when catalog file changes`() {
            val catalogFile = testProjectDir.createCatalogFile()
            testProjectDir.createSettingsFile()
            testProjectDir.createBuildFile()

            // First build
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTypeSafeCatalog", "--stacktrace")
                .withPluginClasspath()
                .build()

            // Modify catalog
            catalogFile.writeText(
                """
                [versions]
                kotlin = "1.9.20"

                [libraries]
                kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
                new-library = "com.example:new-lib:1.0.0"
                """.trimIndent()
            )

            // Second build - should execute
            val secondRun = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTypeSafeCatalog", "--stacktrace")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, secondRun.task(":generateTypeSafeCatalog")?.outcome)
        }
    }

    @Nested
    @DisplayName("Task Dependencies")
    inner class TaskDependencies {

        @Test
        @DisplayName("should run before compileKotlin")
        fun `runs before compileKotlin`() {
            testProjectDir.createCatalogFile()
            testProjectDir.createSettingsFile()
            testProjectDir.createBuildFile()

            // Create a simple Kotlin source file
            File(testProjectDir, "src/main/kotlin/Main.kt").apply {
                parentFile.mkdirs()
                writeText("fun main() { println(\"Hello\") }")
            }

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("compileKotlin", "--dry-run", "--stacktrace")
                .withPluginClasspath()
                .build()

            assertTrue(result.output.contains("generateTypeSafeCatalog"))
        }

        @Test
        @DisplayName("compileKotlin should depend on generateTypeSafeCatalog")
        fun `compileKotlin depends on generateTypeSafeCatalog`() {
            testProjectDir.createCatalogFile()
            testProjectDir.createSettingsFile()
            testProjectDir.createBuildFile()

            File(testProjectDir, "src/main/kotlin/Main.kt").apply {
                parentFile.mkdirs()
                writeText("fun main() { println(\"Hello\") }")
            }

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("compileKotlin", "--stacktrace")
                .withPluginClasspath()
                .build()

            // Verify both tasks executed
            assertEquals(TaskOutcome.SUCCESS, result.task(":generateTypeSafeCatalog")?.outcome)
            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlin")?.outcome)
        }
    }

    companion object {
        private val DEFAULT_CATALOG = """
            [versions]
            kotlin = "1.9.0"

            [libraries]
            kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }

            [plugins]
            kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
        """.trimIndent()

        // Catalog without [plugins] section for Gradle 7.0-7.1 compatibility
        // Plugin support in version catalogs was added in Gradle 7.2
        private val DEFAULT_CATALOG_NO_PLUGINS = """
            [versions]
            kotlin = "1.9.0"

            [libraries]
            kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
        """.trimIndent()
    }
}