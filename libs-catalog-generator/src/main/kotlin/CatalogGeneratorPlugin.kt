import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.io.FileNotFoundException

/**
 * A plugin that generates a type-safe access to the version catalog inside the buildSrc and
 * convention plugins modules.
 * Solves: https://github.com/gradle/gradle/issues/15383
 */
@Suppress("UnstableApiUsage")
class CatalogGeneratorPlugin : Plugin<Any> {
    override fun apply(target: Any) {
        when (target) {
            is Settings -> with(target) {
                val config = extensions.create<CatalogGenConfig>(CATALOG_CONFIG_ACCESSOR)
                gradle.settingsEvaluated {
                    dependencyResolutionManagement {
                        versionCatalogs {
                            create(config.catalogName.getOrElse(CATALOG_NAME)) {
                                from(
                                    layout.rootDirectory.files(target.findCatalogLocation(config))
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
        var currentDir = layout.rootDirectory.asFile

        while (currentDir.exists()) {
            val file = File(currentDir, location)
            if (file.exists()) {
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

    private fun applyPluginToProject(project: Project) {
        val config = project.extensions.extraProperties[CATALOG_CONFIG_ACCESSOR] as CatalogGenConfig

        project.plugins.apply("org.jetbrains.kotlin.jvm")

        project.tasks.register<TypeSafeCatalogTask>(TypeSafeCatalogTask.NAME) {
            catalogName.convention(config.catalogName.getOrElse(CATALOG_NAME))
            project.layout.buildDirectory.file("generated-sources/kotlin-dsl-plugins/kotlin/GeneratedCatalog.kt")
                .let(generatedSourcesFile::convention)
        }
        project.tasks.withType<KotlinCompile> {
            dependsOn(TypeSafeCatalogTask.NAME)
        }
    }

    private companion object {
        const val TOML_CATALOG_LOCATION = "gradle/libs.versions.toml"
        const val CATALOG_NAME = "libs"
        const val CATALOG_CONFIG_ACCESSOR = "catalogGenerator"
    }
}

interface CatalogGenConfig {
    /**
     * The name of the version catalog to generate a type-safe access for. Default: `libs`
     */
    val catalogName: Property<String>

    /**
     * The filename and location of the catalog toml file. Default: `gradle/libs.versions.toml`
     */
    val catalogTomlLocation: Property<String>
}