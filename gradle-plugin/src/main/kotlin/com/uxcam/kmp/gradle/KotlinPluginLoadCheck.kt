package com.uxcam.kmp.gradle

import org.gradle.api.Project

/**
 * Detects the Kotlin Gradle plugin being loaded by more than one classloader in the same build —
 * the classic result of declaring Kotlin/Android plugins only in subprojects, without a root
 * `plugins { ... apply false }` block. Each subproject then gets its own copy of the plugin
 * classes, and the copies cannot share KGP's build services: Kotlin/Native task creation fails
 * with a cryptic "property ... loaded with InstrumentingVisitableURLClassLoader" error that
 * points nowhere near the actual fix.
 *
 * The check runs twice: at plugin apply (sibling projects configured earlier are already
 * visible, and KGP's own task wiring can crash before any later hook fires) and again once all
 * projects are evaluated (to catch Kotlin projects configured after this one). It warns once.
 */
internal object KotlinPluginLoadCheck {

    private const val HOOKED_KEY = "com.uxcam.kmp.kgpLoadCheckHooked"
    private const val WARNED_KEY = "com.uxcam.kmp.kgpLoadCheckWarned"

    fun register(project: Project) {
        warnIfKotlinPluginDuplicated(project)
        val rootExtras = project.rootProject.extensions.extraProperties
        if (rootExtras.has(HOOKED_KEY)) return
        rootExtras.set(HOOKED_KEY, true)
        project.gradle.projectsEvaluated { warnIfKotlinPluginDuplicated(project) }
    }

    private fun warnIfKotlinPluginDuplicated(project: Project) {
        val root = project.rootProject
        val rootExtras = root.extensions.extraProperties
        if (rootExtras.has(WARNED_KEY)) return

        // Any KGP-shipped plugin class works as a witness for which classloader loaded the jar
        // in that project. Unconfigured projects simply have empty plugin containers here.
        val loadersByProject = root.allprojects.mapNotNull { p ->
            val kotlinPlugin = p.plugins.firstOrNull {
                it.javaClass.name.startsWith("org.jetbrains.kotlin.gradle.plugin.Kotlin")
            } ?: return@mapNotNull null
            p.path to kotlinPlugin.javaClass.classLoader
        }
        if (loadersByProject.map { it.second }.toSet().size <= 1) return

        rootExtras.set(WARNED_KEY, true)
        project.logger.warn(
            "[uxcam-kmp] The Kotlin Gradle plugin is loaded by multiple classloaders " +
                "(in ${loadersByProject.joinToString { it.first }}). Kotlin/Native task creation will " +
                "fail with a cryptic \"shared build service\"/classloader error. Fix: declare every " +
                "Kotlin and Android plugin once in the ROOT build script with `apply false`:\n" +
                "    plugins {\n" +
                "        kotlin(\"multiplatform\") version \"<kotlin>\" apply false\n" +
                "        kotlin(\"android\") version \"<kotlin>\" apply false\n" +
                "        id(\"com.android.application\") version \"<agp>\" apply false\n" +
                "    }",
        )
    }
}
