package io.github.reactivecircus.appversioning

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Prints app's versionName and versionCode to the console.
 */
abstract class PrintAppVersionInfo : DefaultTask() {

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionCode: Property<Int>


    @TaskAction
    fun print() {
        println("versionName: ${versionName.get()}")
        println("versionCode: ${versionCode.get()}")
    }

    companion object {
        const val TASK_NAME_PREFIX = "printAppVersionInfo"
        const val TASK_DESCRIPTION_PREFIX = "Prints app's versionName and versionCode to the console"
    }
}
