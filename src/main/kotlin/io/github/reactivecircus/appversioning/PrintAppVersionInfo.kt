package io.github.reactivecircus.appversioning

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Prints app's versionCode and versionName to the console.
 */
abstract class PrintAppVersionInfo : DefaultTask() {

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val versionName: Property<String>


    @TaskAction
    fun print() {
        println("versionCode: ${versionCode.get()}")
        println("versionName: ${versionName.get()}")
    }

    companion object {
        const val TASK_NAME_PREFIX = "printAppVersionInfo"
        const val TASK_DESCRIPTION_PREFIX = "Prints app's versionCode and versionName to the console"
    }
}
