import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency
import java.util.Locale

/**
 * Gradle task that generates type-safe Kotlin accessors for a version catalog.
 *
 * This task reads a TOML version catalog file and generates a Kotlin source file
 * containing type-safe accessors for libraries, plugins, versions, and bundles defined
 * in the catalog.
 *
 * ## Generated Code Structure
 *
 * The task generates a `GeneratedCatalog.kt` file containing:
 * - `GeneratedCatalog` data class - Main wrapper class
 * - `Versions` data class - Accessors for version declarations
 * - `Libraries` class - Accessors for library dependencies
 * - `Bundles` data class - Accessors for dependency bundles
 * - `Plugins` data class - Accessors for Gradle plugins
 * - `PluginsId` enum - Plugin IDs for use in plugin management
 *
 * ## Task Caching
 *
 * This task is annotated with [@CacheableTask], which means its outputs will be cached
 * by Gradle's build cache when the inputs haven't changed, significantly improving
 * build performance.
 *
 * @see CatalogGeneratorPlugin
 */
@CacheableTask
abstract class TypeSafeCatalogTask : DefaultTask() {
    /**
     * The name of the version catalog to generate accessors for.
     *
     * This must match the catalog name configured in the settings file.
     */
    @get:Input
    abstract val catalogName: Property<String>

    /**
     * The TOML file containing the version catalog definitions.
     *
     * Path sensitivity is set to [PathSensitivity.RELATIVE] to support build cache
     * across different machines and directory structures.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val catalogFile: RegularFileProperty

    /**
     * The Kotlin source file where generated accessors will be written.
     *
     * Typically located in `build/generated-sources/kotlin-dsl-plugins/kotlin/GeneratedCatalog.kt`
     */
    @get:OutputFile
    abstract val generatedSourcesFile: RegularFileProperty

    init {
        group = "build"
        description = "Generates a type-safe access to the version catalog inside the buildSrc"
    }

    @TaskAction
    fun generate() {
        val catalogs = project.extensions.getByType<VersionCatalogsExtension>()
        val catalogName = catalogName.get()
        val catalog = catalogs.named(catalogName)
        val sourceCode = generateCode(catalog = catalog, catalogName = catalogName)
        generatedSourcesFile.get().asFile.writeText(sourceCode.toString())
    }

    private fun generateCode(catalog: VersionCatalog, catalogName: String): FileSpec =
        FileSpec.builder("", "GeneratedCatalog").addImports().generateExtensionFunction()
            .generateAccessorFunction(catalogName).generateMainWrapperGeneratedCatalog()
            .generateVersionsDataClass(catalog).generateLibrariesDataClass(catalog)
            .generateBundlesDataClass(catalog).generatePluginsDataClass(catalog)
            .generatePluginsIdEnum(catalog).build()

    private fun FileSpec.Builder.addImports() = addImport(
        "org.gradle.api.artifacts",
        "MinimalExternalModuleDependency"
    ).addImport("org.gradle.api.artifacts", "VersionCatalogsExtension")
        .addImport("org.gradle.kotlin.dsl", "getByType")

    private fun FileSpec.Builder.generateExtensionFunction() =
        FunSpec.builder("orElseThrowIllegalArgs").addTypeVariable(TypeVariableName("T"))
            .receiver(ClassName("java.util", "Optional").parameterizedBy(TypeVariableName("T")))
            .addParameter("alias", String::class).addParameter("type", String::class)
            .returns(TypeVariableName("T"))
            .addStatement("return this.orElseThrow{ IllegalArgumentException(\" \$type alias '\$alias' not found\") }")
            .build().let(::addFunction)

    private fun FileSpec.Builder.generateAccessorFunction(catalogName: String) =
        PropertySpec.builder(catalogName, ClassName("", "GeneratedCatalog"))
            .receiver(Project::class).getter(
                FunSpec.getterBuilder().addStatement(
                    "return GeneratedCatalog(extensions.getByType<VersionCatalogsExtension>().named(\"${catalogName}\"))"
                ).build()
            ).build().let(::addProperty)

    private fun FileSpec.Builder.generateMainWrapperGeneratedCatalog() =
        TypeSpec.classBuilder("GeneratedCatalog").addModifiers(KModifier.DATA).primaryConstructor(
            FunSpec.constructorBuilder().addParameter("catalog", VersionCatalog::class).build()
        ).addProperty(
            PropertySpec.builder("catalog", VersionCatalog::class).initializer("catalog").build()
        ).addProperty(
            PropertySpec.builder("versions", ClassName("", "Versions"))
                .initializer("Versions(catalog)").build()
        ).addProperty(
            PropertySpec.builder("libraries", ClassName("", "Libraries"))
                .initializer("Libraries(catalog)").build()
        ).addProperty(
            PropertySpec.builder("bundles", ClassName("", "Bundles"))
                .initializer("Bundles(catalog)").build()
        ).addProperty(
            PropertySpec.builder("plugins", ClassName("", "Plugins"))
                .initializer("Plugins(catalog)").build()
        ).build().let(::addType)

    private fun FileSpec.Builder.generateVersionsDataClass(
        catalog: VersionCatalog,
    ) = TypeSpec.classBuilder("Versions").addModifiers(KModifier.DATA).primaryConstructor(
        FunSpec.constructorBuilder().addParameter("catalog", VersionCatalog::class).build()
    ).addProperty(
        PropertySpec.builder("catalog", VersionCatalog::class).initializer("catalog").build()
    ).apply {
        addProperties(
            catalog.versionAliases.map { alias ->
                PropertySpec.builder(alias.toCamelCase(), String::class).getter(
                    FunSpec.getterBuilder().addStatement(
                        "return catalog.findVersion(%S).orElseThrowIllegalArgs(%S, %S).requiredVersion",
                        alias,
                        alias,
                        "Version"
                    ).build()
                ).build()
            }
        )
    }.build().let(::addType)

    private fun FileSpec.Builder.generateBundlesDataClass(catalog: VersionCatalog) =
        TypeSpec.classBuilder("Bundles").addModifiers(KModifier.DATA).primaryConstructor(
            FunSpec.constructorBuilder().addParameter("catalog", VersionCatalog::class).build()
        ).addProperty(
            PropertySpec.builder("catalog", VersionCatalog::class).initializer("catalog").build()
        ).apply {
            catalog.bundleAliases.forEach { alias ->
                addProperty(
                    PropertySpec.Companion.builder(
                        alias.toCamelCase(),
                        Provider::class.parameterizedBy(ExternalModuleDependencyBundle::class)
                    ).getter(
                        FunSpec.getterBuilder().addStatement(
                            "return catalog.findBundle(%S).orElseThrowIllegalArgs(%S, %S)",
                            alias,
                            alias,
                            "Bundle"
                        ).build()
                    ).build()
                )
            }
        }.build().let(::addType)

    private fun FileSpec.Builder.generateLibrariesDataClass(
        catalog: VersionCatalog,
    ) = TypeSpec.classBuilder("Libraries").primaryConstructor(
        FunSpec.constructorBuilder().addParameter("catalog", VersionCatalog::class).build()
    ).addProperty(
        PropertySpec.builder("catalog", VersionCatalog::class).initializer("catalog").build()
    ).apply {
        addProperties(
            catalog.libraryAliases.map { alias ->
                PropertySpec.Companion.builder(
                    alias.toCamelCase(),
                    Provider::class.parameterizedBy(MinimalExternalModuleDependency::class)
                ).getter(
                    FunSpec.getterBuilder().addStatement(
                        "return catalog.findLibrary(%S).orElseThrowIllegalArgs(%S, %S)",
                        alias,
                        alias,
                        "Library"
                    ).build()
                ).build()
            }
        )
    }.build().let(::addType)

    private fun FileSpec.Builder.generatePluginsDataClass(catalog: VersionCatalog) =
        TypeSpec.classBuilder("Plugins").addModifiers(KModifier.DATA).primaryConstructor(
            FunSpec.constructorBuilder().addParameter("catalog", VersionCatalog::class).build()
        ).addProperty(
            PropertySpec.builder("catalog", VersionCatalog::class).initializer("catalog").build()
        ).apply {
            catalog.pluginAliases.forEach { alias ->
                addProperty(
                    PropertySpec.Companion.builder(
                        alias.toCamelCase(),
                        Provider::class.parameterizedBy(PluginDependency::class)
                    ).getter(
                        FunSpec.getterBuilder().addStatement(
                            "return catalog.findPlugin(%S).orElseThrowIllegalArgs(%S, %S)",
                            alias,
                            alias,
                            "Plugin"
                        ).build()
                    ).build()
                )
            }
        }.build().let(::addType)

    private fun FileSpec.Builder.generatePluginsIdEnum(catalog: VersionCatalog) =
        TypeSpec.enumBuilder("PluginsId").primaryConstructor(
            FunSpec.constructorBuilder().addParameter("id", String::class).build()
        ).addProperty(
            PropertySpec.builder("id", String::class).initializer("id").build()
        ).apply {
            catalog.pluginAliases.forEach { alias ->
                val plugin = catalog.findPlugin(alias)
                    .orElseThrow { IllegalStateException("Plugin '$alias' not found in catalog") }
                    .get()
                val pluginId = plugin.pluginId
                addEnumConstant(
                    alias.toCamelCase(),
                    TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter("%S", pluginId).build()
                )
            }
        }.build().let(::addType)

    companion object {
        const val NAME = "generateTypeSafeCatalog"
    }
}

/**
 * Converts a string with delimiters (-, _, .) to camelCase format suitable for Kotlin identifiers.
 *
 * ## Conversion Rules
 * - Splits on: `-`, `_`, `.`
 * - Capitalizes first letter of each part
 * - Makes first letter lowercase for the result
 * - Empty parts are filtered out
 * - Returns "empty" for empty strings or strings with only delimiters
 *
 * ## Identifier Validation
 * If the result is not a valid Kotlin identifier, it's wrapped in backticks (` `name` `).
 *
 * ## Examples
 * ```
 * "kotlin-stdlib" -> "kotlinStdlib"
 * "my_lib" -> "myLib"
 * "some.library" -> "someLibrary"
 * "2-invalid" -> "`2Invalid`"  // Not a valid identifier, uses backticks
 * "" -> "empty"
 * "---" -> "empty"
 * ```
 *
 * @receiver The string to convert to camelCase
 * @return A valid Kotlin identifier in camelCase format
 */
internal fun String.toCamelCase(): String {
    if (isEmpty()) return "empty"

    val parts = split("-", "_", ".").filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "empty"

    val camelCase = parts.joinToString("") { part ->
        part.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault())
            else char.toString()
        }
    }.replaceFirstChar { it.lowercase(Locale.getDefault()) }

    // Ensure it's a valid Kotlin identifier
    return when {
        camelCase.isEmpty() -> "empty"
        camelCase.first().isJavaIdentifierStart() && camelCase.all { it.isJavaIdentifierPart() } -> camelCase
        else -> "`$camelCase`"  // Use backticks for invalid identifiers
    }
}
