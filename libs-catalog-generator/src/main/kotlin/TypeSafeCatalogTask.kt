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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency
import java.util.Locale

abstract class TypeSafeCatalogTask : DefaultTask() {
    @get:Input
    abstract val catalogName: Property<String>

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
        val sourceCode = generateCode(catalog = catalog)
        generatedSourcesFile.get().asFile.writeText(sourceCode.toString())
    }

    private fun generateCode(catalog: VersionCatalog): FileSpec =
        FileSpec.builder("", "GeneratedCatalog").addImports().generateExtensionFunction()
            .generateAccessorFunction().generateMainWrapperGeneratedCatalog()
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

    private fun FileSpec.Builder.generateAccessorFunction() =
        PropertySpec.builder("libs", ClassName("", "GeneratedCatalog"))
            .receiver(Project::class).getter(
                FunSpec.getterBuilder().addStatement(
                    "return GeneratedCatalog(extensions.getByType<VersionCatalogsExtension>().named(\"${catalogName.get()}\"))"
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
                val pluginId = catalog.findPlugin(alias).get().get().pluginId
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

private fun String.toCamelCase(): String = split("-", "_", ".").joinToString("") {
    it.replaceFirstChar { firstChar ->
        when {
            firstChar.isLowerCase() -> firstChar.titlecase(Locale.getDefault())
            else -> firstChar.toString()
        }
    }
}.replaceFirstChar { it.lowercase(Locale.getDefault()) }
