package com.uxcam.kmp.gradle

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UXCamKmpPluginTest {

    // --- compareVersions ---

    @Test
    fun `compareVersions orders plain versions numerically`() {
        assertTrue(compareVersions("2.2.0", "2.4.0") < 0)
        assertTrue(compareVersions("2.4.0", "2.2.21") > 0)
        assertEquals(0, compareVersions("2.4.0", "2.4.0"))
        // Numeric, not lexicographic: 2.10 > 2.9.
        assertTrue(compareVersions("2.10.0", "2.9.0") > 0)
    }

    @Test
    fun `compareVersions ignores pre-release suffixes and missing components`() {
        assertEquals(0, compareVersions("2.4.0-RC1", "2.4.0"))
        assertEquals(0, compareVersions("2.4", "2.4.0"))
        assertTrue(compareVersions("2.4-Beta", "2.3.21") > 0)
    }

    // --- Plugin application ---

    @Test
    fun `apply registers the extension with safe defaults`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(UXCamKmpPlugin::class.java)

        val ext = project.extensions.getByType(UXCamKmpExtension::class.java)
        assertTrue(ext.verifyKotlinVersion.get())
        assertTrue(ext.installComposeHelpers.get())
        assertFalse(ext.exportToIosFrameworks.get())
        assertTrue(ext.iosLinkReminder.get())
        assertEquals(UXCamVersions.UXCAM_KMP, ext.libraryVersion.get())
    }

    @Test
    fun `kmp module gets the library dependency honouring a later libraryVersion override`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply(UXCamKmpPlugin::class.java)
        // Set AFTER apply, the way a build script does — the provider-based wiring must honour it.
        project.extensions.getByType(UXCamKmpExtension::class.java).libraryVersion.set("9.9.9")

        val deps = project.configurations.getByName("commonMainImplementation").dependencies
        assertTrue(
            deps.any { it.group == "com.uxcam" && it.name == "uxcam-kmp" && it.version == "9.9.9" },
            "expected com.uxcam:uxcam-kmp:9.9.9 in commonMainImplementation, got $deps",
        )
    }

    @Test
    fun `apply on a plain project adds no uxcam dependencies`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(UXCamKmpPlugin::class.java)
        // Trigger afterEvaluate hooks — the plain-project path only wires dependencies there.
        (project as ProjectInternal).evaluate()

        val uxcamDependencies = project.configurations
            .flatMap { it.dependencies }
            .filter { it.group == "com.uxcam" }
        assertTrue(uxcamDependencies.isEmpty(), "expected no dependency wiring, got $uxcamDependencies")
    }
}
