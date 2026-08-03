package com.uxcam.buildlogic

import org.gradle.api.provider.Property

/** Per-module POM fields consumed by the `uxcam-publishing` convention plugin. */
interface UXCamPomExtension {
    val name: Property<String>
    val description: Property<String>
}
