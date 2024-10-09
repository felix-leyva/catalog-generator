package org.gradle.kotlin.dsl

import CatalogGenConfig
import org.gradle.api.initialization.Settings

/**
 * Configures the catalog generator.
 */
inline fun Settings.catalogGenerator(configure: CatalogGenConfig.() -> Unit) {
    // Inline function added due that Gradle might not generate the accessors in the settings dsl
    extensions.getByType<CatalogGenConfig>().configure()
}