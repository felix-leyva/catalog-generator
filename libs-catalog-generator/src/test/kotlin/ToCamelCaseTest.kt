import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.test.assertEquals

/**
 * Unit tests for the toCamelCase() extension function.
 *
 * Tests cover various scenarios including:
 * - Normal camelCase conversions
 * - Edge cases (empty strings, only delimiters)
 * - Invalid identifiers (numbers at start, special characters)
 * - Different delimiters (-, _, .)
 * - Mixed delimiters
 */
@DisplayName("ToCamelCase Extension Function Tests")
class ToCamelCaseTest {

    @Nested
    @DisplayName("Standard camelCase conversions")
    inner class StandardConversions {

        @Test
        @DisplayName("should convert hyphen-separated string to camelCase")
        fun `hyphen-separated to camelCase`() {
            assertEquals("kotlinStdlib", "kotlin-stdlib".toCamelCase())
            assertEquals("myAwesomeLibrary", "my-awesome-library".toCamelCase())
        }

        @Test
        @DisplayName("should convert underscore-separated string to camelCase")
        fun `underscore-separated to camelCase`() {
            assertEquals("myLib", "my_lib".toCamelCase())
            assertEquals("someLibraryName", "some_library_name".toCamelCase())
        }

        @Test
        @DisplayName("should convert dot-separated string to camelCase")
        fun `dot-separated to camelCase`() {
            assertEquals("someLibrary", "some.library".toCamelCase())
            assertEquals("orgGradleApi", "org.gradle.api".toCamelCase())
        }

        @Test
        @DisplayName("should handle single word")
        fun `single word unchanged`() {
            assertEquals("library", "library".toCamelCase())
            assertEquals("kotlin", "kotlin".toCamelCase())
        }

        @Test
        @DisplayName("should handle already camelCase string")
        fun `already camelCase unchanged`() {
            assertEquals("kotlinStdlib", "kotlinStdlib".toCamelCase())
        }
    }

    @Nested
    @DisplayName("Mixed delimiters")
    inner class MixedDelimiters {

        @Test
        @DisplayName("should handle mixed delimiters")
        fun `mixed delimiters to camelCase`() {
            assertEquals("myLibraryVersion", "my-library_version".toCamelCase())
            assertEquals("comExampleProject", "com.example-project".toCamelCase())
            assertEquals("testLibraryCore", "test_library.core".toCamelCase())
        }
    }

    @Nested
    @DisplayName("Edge cases")
    inner class EdgeCases {

        @Test
        @DisplayName("should return 'empty' for empty string")
        fun `empty string returns empty`() {
            assertEquals("empty", "".toCamelCase())
        }

        @Test
        @DisplayName("should return 'empty' for only delimiters")
        fun `only delimiters returns empty`() {
            assertEquals("empty", "---".toCamelCase())
            assertEquals("empty", "___".toCamelCase())
            assertEquals("empty", "...".toCamelCase())
            assertEquals("empty", "-_-.".toCamelCase())
        }

        @Test
        @DisplayName("should filter out empty parts")
        fun `multiple consecutive delimiters filtered out`() {
            assertEquals("myLib", "my--lib".toCamelCase())
            assertEquals("someLibrary", "some___library".toCamelCase())
            assertEquals("testProject", "test...project".toCamelCase())
        }

        @Test
        @DisplayName("should handle delimiters at start and end")
        fun `delimiters at boundaries`() {
            assertEquals("myLib", "-my-lib-".toCamelCase())
            assertEquals("someLibrary", "_some_library_".toCamelCase())
            assertEquals("testProject", ".test.project.".toCamelCase())
        }
    }

    @Nested
    @DisplayName("Invalid Kotlin identifiers")
    inner class InvalidIdentifiers {

        @Test
        @DisplayName("should wrap identifiers starting with numbers in backticks")
        fun `numbers at start use backticks`() {
            assertEquals("`2Invalid`", "2-invalid".toCamelCase())
            assertEquals("`123Test`", "123-test".toCamelCase())
        }

        @Test
        @DisplayName("should handle uppercase first letter")
        fun `uppercase first letter converted to lowercase`() {
            assertEquals("kotlinStdlib", "Kotlin-Stdlib".toCamelCase())
            assertEquals("myLibrary", "My-Library".toCamelCase())
        }

        @Test
        @DisplayName("should preserve valid identifiers with numbers in middle")
        fun `numbers in middle are valid`() {
            assertEquals("lib2Core", "lib-2-core".toCamelCase())
            assertEquals("version3Api", "version-3-api".toCamelCase())
        }
    }

    @Nested
    @DisplayName("Special cases")
    inner class SpecialCases {

        @Test
        @DisplayName("should handle single character parts")
        fun `single character parts`() {
            assertEquals("aBC", "a-b-c".toCamelCase())
            assertEquals("xYZ", "x_y_z".toCamelCase())  // Each part is capitalized: X, Y, Z -> xYZ
        }

        @Test
        @DisplayName("should handle numbers only")
        fun `numbers only`() {
            assertEquals("`123`", "1-2-3".toCamelCase())
        }

        @Test
        @DisplayName("should handle mixed case inputs")
        fun `mixed case inputs`() {
            assertEquals("kotlinStdLib", "Kotlin-Std-Lib".toCamelCase())
            assertEquals("mYAWESOMELIBRARY", "MY-AWESOME-LIBRARY".toCamelCase())  // Each part stays uppercase, first char lowercased
        }
    }

    @Nested
    @DisplayName("Real-world catalog aliases")
    inner class RealWorldAliases {

        @Test
        @DisplayName("should handle common library names")
        fun `common library names`() {
            assertEquals("kotlinStdlibJdk8", "kotlin-stdlib-jdk8".toCamelCase())
            assertEquals("androidxCoreKtx", "androidx-core-ktx".toCamelCase())
            assertEquals("comGoogleAndroidMaterial", "com-google-android-material".toCamelCase())
        }

        @Test
        @DisplayName("should handle common plugin names")
        fun `common plugin names`() {
            assertEquals("orgJetbrainsKotlinAndroid", "org-jetbrains-kotlin-android".toCamelCase())
            assertEquals("comAndroidApplication", "com-android-application".toCamelCase())
        }

        @Test
        @DisplayName("should handle version aliases")
        fun `version aliases`() {
            assertEquals("agp", "agp".toCamelCase())
            assertEquals("kotlin", "kotlin".toCamelCase())
            assertEquals("compileSdk", "compileSdk".toCamelCase())
        }
    }
}