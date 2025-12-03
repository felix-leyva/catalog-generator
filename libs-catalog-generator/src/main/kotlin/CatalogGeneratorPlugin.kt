import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.util.GradleVersion
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

/**
 * A Gradle plugin that generates type-safe accessors for version catalogs within buildSrc
 * and convention plugin modules.
 *
 * This plugin solves the limitation described in [Gradle issue #15383](https://github.com/gradle/gradle/issues/15383)
 * where version catalogs are not directly accessible from buildSrc or convention plugins.
 *
 * ## Usage
 *
 * Apply this plugin in your `settings.gradle.kts` file:
 *
 * ```kotlin
 * plugins {
 *     id("de.felixlf.libs-catalog-generator")
 * }
 * ```
 *
 * ## Configuration
 *
 * The plugin can be configured using the `catalogGenerator` extension:
 *
 * ```kotlin
 * catalogGenerator {
 *     catalogName.set("myLibs")  // Default: "projectlibs"
 *     catalogTomlLocation.set("gradle/libs.versions.toml")  // Default location
 * }
 * ```
 *
 * ## Generated Code
 *
 * The plugin generates a `GeneratedCatalog.kt` file with type-safe accessors for:
 * - Libraries
 * - Plugins
 * - Versions
 * - Bundles
 *
 * Access the catalog in your build files:
 * ```kotlin
 * dependencies {
 *     implementation(projectlibs.libraries.kotlinStdlib)
 * }
 * ```
 *
 * @see CatalogGenConfig for configuration options
 */
@Suppress("UnstableApiUsage")
class CatalogGeneratorPlugin @Inject constructor(
    private val objects: ObjectFactory
) : Plugin<Any> {
    /**
     * Holds the catalog file location once found. Null until the catalog file is located.
     */
    private var catalogFileOrNull: File? = null
    private val currentVersion = GradleVersion.current()
    private val gradle85 = GradleVersion.version("8.5")
    private val gradle70 = GradleVersion.version("7.0")
    private val gradle74 = GradleVersion.version("7.4")

    private fun Settings.fileReference(path: String): File = when {
        currentVersion >= gradle85 -> settings.layout.rootDirectory.file(path).asFile
        else -> File(path)
    }

    private fun Settings.rootDirectory(): File = when {
        currentVersion >= gradle85 -> settings.layout.rootDirectory.asFile
        else -> settings.rootDir
    }

    private fun Settings.fileCollectionFrom(file: File): FileCollection {
        println("file: $file")
        return when {
            currentVersion >= gradle85 -> layout.rootDirectory.files(file)
            else -> objects.fileCollection().from(file)
        }
    }

    override fun apply(target: Any) {
        when (target) {
            is Settings -> with(target) {
                val config = extensions.create<CatalogGenConfig>(CATALOG_CONFIG_ACCESSOR)

                // Enable version catalogs feature preview for Gradle 7.0-7.3
                // In Gradle 7.4+, version catalogs became stable and don't require feature preview
                if (currentVersion >= gradle70 && currentVersion < gradle74) {
                    enableFeaturePreview("VERSION_CATALOGS")
                }

                gradle.settingsEvaluated {
                    val catalogPath = target.findCatalogLocation(config)
                    val catalogSource = fileReference(catalogPath)

                    dependencyResolutionManagement {
                        versionCatalogs {
                            create(config.catalogName.getOrElse(CATALOG_NAME)) {
                                from(
                                    fileCollectionFrom(catalogSource)
                                )
                            }
                        }
                    }
                }

                gradle.rootProject {
                    extensions.extraProperties.set(CATALOG_CONFIG_ACCESSOR, config)
                    applyPluginToProject(this)
                }
            }

            is Project -> applyPluginToProject(target)

        }
    }

    private fun Settings.findCatalogLocation(config: CatalogGenConfig): String {
        val location = config.catalogTomlLocation.getOrElse(TOML_CATALOG_LOCATION)
        var currentDir = rootDirectory()

        while (currentDir.exists()) {
            val file = File(currentDir, location)
            if (file.exists()) {
                catalogFileOrNull = file
                return file.absolutePath
            }
            currentDir = currentDir.parentFile ?: break
        }
        throw FileNotFoundException(
            "Could not find $location in the project hierarchy. Please specify " +
                    "the correct location in the settings.gradle.kts file using the " +
                    "catalogTomlLocation property of the catalogGenerator extension: \n" +
                    "catalogGenerator {\n" +
                    "    catalogTomlLocation.set(\"path/to/your/catalog.toml\")\n" +
                    "}" +
                    ""
        )
    }

    private fun findCatalogLocationInProject(project: Project, config: CatalogGenConfig): File {
        val location = config.catalogTomlLocation.getOrElse(TOML_CATALOG_LOCATION)
        var currentDir = project.rootDir

        while (currentDir.exists()) {
            val file = File(currentDir, location)
            if (file.exists()) {
                return file
            }
            currentDir = currentDir.parentFile ?: break
        }
        throw FileNotFoundException(
            "Could not find $location in the project hierarchy. Please specify " +
                    "the correct location in the build.gradle.kts file using the " +
                    "catalogTomlLocation property of the catalogGenerator extension: \n" +
                    "catalogGenerator {\n" +
                    "    catalogTomlLocation.set(\"path/to/your/catalog.toml\")\n" +
                    "}"
        )
    }

    private fun applyPluginToProject(project: Project) {
        val config = project.extensions.findByName(CATALOG_CONFIG_ACCESSOR) as? CatalogGenConfig
            ?: try {
                project.extensions.extraProperties.get(CATALOG_CONFIG_ACCESSOR) as? CatalogGenConfig
            } catch (_: ExtraPropertiesExtension.UnknownPropertyException) {
                null
            } ?: project.extensions.create<CatalogGenConfig>(CATALOG_CONFIG_ACCESSOR)

        // Get or find the catalog file
        val catalogFile = catalogFileOrNull ?: findCatalogLocationInProject(project, config)

        // Apply Kotlin plugin if not already applied, using string to avoid class reference
        if (!project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
            try {
                project.plugins.apply("org.jetbrains.kotlin.jvm")
            } catch (_: Exception) {
                try {
                    project.plugins.apply("kotlin")
                } catch (_: Exception) {
                    project.logger.warn("Could not apply Kotlin plugin. The plugin may still work if Kotlin is applied elsewhere.")
                }
            }
        }

        project.tasks.register<TypeSafeCatalogTask>(TypeSafeCatalogTask.NAME) {
            catalogName.convention(config.catalogName.getOrElse(CATALOG_NAME))
            this.catalogFile.convention(
                project.layout.projectDirectory.file(
                    project.rootDir.toPath().relativize(catalogFile.toPath()).toString()
                )
            )
            project.layout.buildDirectory.file("generated-sources/kotlin-dsl-plugins/kotlin/GeneratedCatalog.kt")
                .let(generatedSourcesFile::convention)
        }

        // Make all Kotlin compile tasks depend on our generator task - using string-based API to avoid class reference
        project.tasks.matching { task ->
            task.name.startsWith("compile") && task.name.endsWith("Kotlin")
        }.configureEach {
            dependsOn(TypeSafeCatalogTask.NAME)
        }
    }

    private companion object {
        const val TOML_CATALOG_LOCATION = "gradle/libs.versions.toml"
        const val CATALOG_NAME = "projectlibs"
        const val CATALOG_CONFIG_ACCESSOR = "catalogGenerator"
    }
}

/**
 * Configuration options for the Catalog Generator plugin.
 *
 * Configure this extension in your `settings.gradle.kts`:
 *
 * ```kotlin
 * catalogGenerator {
 *     catalogName.set("myCustomCatalog")
 *     catalogTomlLocation.set("config/dependencies.toml")
 * }
 * ```
 */
interface CatalogGenConfig {
    /**
     * The name of the version catalog to generate type-safe accessors for.
     *
     * This name will be used to:
     * - Create the catalog in Gradle's version catalog system
     * - Generate the accessor property name in generated code
     *
     * **Default:** `projectlibs`
     *
     * **Example:**
     * ```kotlin
     * catalogName.set("myLibs")  // Accessible as: myLibs.libraries.kotlinStdlib
     * ```
     */
    val catalogName: Property<String>

    /**
     * The file path to the version catalog TOML file, relative to the project root.
     *
     * The plugin will search for this file starting from the current directory and
     * walking up the directory tree until it finds it.
     *
     * **Default:** `gradle/libs.versions.toml`
     *
     * **Example:**
     * ```kotlin
     * catalogTomlLocation.set("config/dependencies.toml")
     * ```
     */
    val catalogTomlLocation: Property<String>
}