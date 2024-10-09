# libs-catalog-generator Gradle Plugin

Allows to use type-safe dependencies from the catalog library toml file inside the buildSrc folder
or other convention
plugins.

## Usage

Apply it in your `settings.gradle.kts` inside either your `buildSrc` folder, or other convention
plugin folder

```
plugins {
    id("de.felixlf.libs-catalog-generator")
}
```

It will automatically generate the libs extension, to use it inside your convention plugins or dsl

## Extra configuration

In case your libs.version.toml is not located as standard in `gradle/libs.version.toml` or you wish
to name the catalog
with another name, the plugin can be configured using the extension in the `settings.gradle.kts`
file of your buildSrc
or convention plugin

```
catalogGenerator {
    catalogTomlLocation.set("location/libs.versions.toml")
    catalogName.set("my-lib")
}
```