package io.github.reactivecircus.appversioning

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import java.io.File

fun Project.createAndroidAppProject(hasProductFlavor: Boolean) {
    extensions.getByType(AppExtension::class.java).apply {
        compileSdkVersion(29)
        if (hasProductFlavor) {
            flavorDimensions("environment")
            productFlavors.create("mock")
            productFlavors.create("prod")
        }
    }

    File(projectDir, "src/main/AndroidManifest.xml").apply {
        parentFile.mkdirs()
        writeText("""<manifest package="com.foo.bar"/>""")
    }
}

fun Project.createAndroidLibraryProject() {
    extensions.getByType(LibraryExtension::class.java).apply {
        compileSdkVersion(29)
    }

    File(projectDir, "src/main/AndroidManifest.xml").apply {
        parentFile.mkdirs()
        writeText("""<manifest package="com.foo.bar"/>""")
    }
}
