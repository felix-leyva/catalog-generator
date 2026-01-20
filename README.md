# Libs Catalog Generator - Gradle Plugin

A Gradle plugin that generates type-safe Kotlin accessors for version catalogs, making them available inside `buildSrc`
and convention plugins. This solves the limitation described
in [Gradle issue #15383](https://github.com/gradle/gradle/issues/15383) where version catalogs are not directly
accessible from buildSrc or convention plugins.

## Version 2.1

This release fixes the eager evaluation bug for `catalogTomlLocation`, ensuring user configuration is properly respected for relative and absolute paths.

### What's New in 2.1
- Fixed: Plugin now correctly respects custom `catalogTomlLocation` settings
- Fixed: Support for relative paths (e.g., `../gradle/libs.versions.toml`)
- Fixed: Support for absolute paths in nested project structures (build-logic/convention pattern)

## Features

- **Type-safe access** to libraries, plugins, versions, and bundles from version catalogs
- **Automatic code generation** at build time
- **Multi-version Gradle support** (7.0 - 9.x)
- **Build cache compatible** for optimal performance
- **Zero configuration** - works out of the box with standard catalog locations
- **Convention plugin ready** - designed for use in buildSrc and convention plugins

## Requirements

- **Gradle**: 7.0 or higher
- **Java**: 11 or higher (the plugin generates Java 11 compatible bytecode)
- **Kotlin**: Any version (uses compileOnly dependencies)

**Note**: While the plugin is compiled using Java 17, it generates Java 11 compatible bytecode, making it compatible
with any Java version 11 or higher. Your project can use any Java version independently.

## Gradle Version Compatibility

| Gradle Version | Support Status  | Notes                                            |
|----------------|-----------------|--------------------------------------------------|
| 6.x and below  | Not Supported   | Version Catalogs API introduced in Gradle 7.0    |
| 7.0 - 7.1      | Supported       | Limited - no `[plugins]` section support in TOML |
| 7.2 - 7.3      | Supported       | Feature preview automatically enabled            |
| 7.4 - 8.x      | Fully Supported | Version Catalogs stable API                      |
| 9.x            | Fully Supported | Compatible with Java 11+                         |

**Note**: The plugin automatically handles version-specific configuration, including enabling the `VERSION_CATALOGS`
feature preview for Gradle 7.0-7.3. While Gradle 9.x requires Java 17+ to run, the plugin generates Java 11 compatible
bytecode and works with any Java 11+ installation.

## Installation

### Step 1: Apply the Plugin

In your `buildSrc/settings.gradle.kts` or convention plugin's `settings.gradle.kts`:

```kotlin
plugins {
    id("de.felixlf.libs-catalog-generator") version "2.1"
}
```

### Step 2: Configure Repositories

The plugin does NOT configure repositories automatically. Add them to your settings file:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()  // Only for Android projects
        gradlePluginPortal()  // If using Gradle plugins
    }
}
```

## Basic Usage

After applying the plugin, it automatically generates type-safe accessors for your version catalog. The default catalog
name is `projectlibs`.

### Example: Using in Convention Plugins

Given a `gradle/libs.versions.toml` file:

```toml
[versions]
kotlin = "1.9.0"
coroutines = "1.7.3"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
```

You can access these in your convention plugins or buildSrc:

```kotlin
// In your convention plugin or buildSrc build file
dependencies {
    // Access libraries
    implementation(projectlibs.libraries.kotlinStdlib)
    implementation(projectlibs.libraries.kotlinxCoroutinesCore)

    // Access versions
    val kotlinVersion = projectlibs.versions.kotlin
    println("Using Kotlin version: $kotlinVersion")

    // Access plugin dependencies
    implementation(projectlibs.plugins.kotlinJvm)
}
```

### Example: Using Plugin IDs

```kotlin
plugins {
    id(projectlibs.plugins.kotlinJvm.get().pluginId)
}
```

## Configuration

### Custom Catalog Location

If your catalog is not in the default location (`gradle/libs.versions.toml`):

```kotlin
catalogGenerator {
    catalogTomlLocation.set("config/dependencies.toml")
}
```

### Custom Catalog Name

If you want to use a different accessor name (default is `projectlibs`):

```kotlin
catalogGenerator {
    catalogName.set("myLibs")
}
```

Then access it as:

```kotlin
dependencies {
    implementation(myLibs.libraries.kotlinStdlib)
}
```

### Complete Configuration Example

```kotlin
// In buildSrc/settings.gradle.kts
plugins {
    id("de.felixlf.libs-catalog-generator") version "2.1"
}

// Optional configuration only if you need a different catalog name or location
catalogGenerator {
    catalogTomlLocation.set("gradle/libs.versions.toml")
    catalogName.set("libs")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
```

## Generated Code Structure

The plugin generates a `GeneratedCatalog.kt` file in `build/generated-sources/kotlin-dsl-plugins/kotlin/` containing:

- **`GeneratedCatalog`** - Main wrapper class
- **`Versions`** - Accessors for version declarations
- **`Libraries`** - Accessors for library dependencies
- **`Bundles`** - Accessors for dependency bundles
- **`Plugins`** - Accessors for Gradle plugins
- **`PluginsId`** - Enum with plugin IDs for plugin management

## Common Use Cases

### buildSrc Configuration

```kotlin
// buildSrc/settings.gradle.kts
plugins {
    id("de.felixlf.libs-catalog-generator") version "2.1"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

```kotlin
// buildSrc/build.gradle.kts
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(projectlibs.plugins.kotlinJvm)
}
```

### Convention Plugins Module

```kotlin
// convention-plugins/settings.gradle.kts
plugins {
    id("de.felixlf.libs-catalog-generator") version "2.1"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
```

```kotlin
// convention-plugins/build.gradle.kts
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(projectlibs.plugins.kotlinJvm)
    implementation(projectlibs.plugins.androidApplication)
}
```

## Repository Configuration Examples

### Kotlin/JVM Projects

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

### Android Projects

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

### Projects with Gradle Plugins

```kotlin
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

## Troubleshooting

### Issue: "Could not find version catalog"

**Cause**: The plugin cannot locate your `libs.versions.toml` file.

**Solution**: Specify the correct location in your settings file:

```kotlin
catalogGenerator {
    catalogTomlLocation.set("path/to/your/catalog.toml")
}
```

### Issue: Build cache not working

**Cause**: In versions before 2.0, absolute paths prevented cache reuse.

**Solution**: Update to version 2.0 or later. The plugin now uses relative paths automatically.

### Issue: Gradle 7.0-7.1 error with `[plugins]` section

**Cause**: Plugin support in TOML catalogs was added in Gradle 7.2.

**Solution**: Either:

- Upgrade to Gradle 7.2 or higher, or
- Remove the `[plugins]` section from your catalog for Gradle 7.0-7.1

### Issue: "Feature preview must be enabled"

**Cause**: Gradle 7.0-7.3 requires the VERSION_CATALOGS feature preview.

**Solution**: Update to version 2.0 - it automatically enables the feature preview when needed.

## Known Limitations

- **Gradle 6.x**: Not supported (Version Catalogs API does not exist)
- **Gradle 7.0-7.1**: The `[plugins]` section in TOML files is not supported
- **Single catalog**: Currently only one catalog can be configured per module

## Performance

The plugin is designed for optimal build performance:

- **Cacheable task**: Uses `@CacheableTask` for build cache support
- **Relative paths**: All file inputs use relative paths for cache portability
- **Incremental builds**: Only regenerates when the catalog file changes

## License

This plugin is released under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request with corresponding unit tests.

For bug reports and feature requests, please use
the [GitHub issue tracker](https://github.com/felix-leyva/catalog-generator/issues).

## Links

- [GitHub Repository](https://github.com/felix-leyva/catalog-generator)
- [Gradle Plugin Portal](https://plugins.gradle.org/plugin/de.felixlf.libs-catalog-generator)
- [Gradle Issue #15383](https://github.com/gradle/gradle/issues/15383)