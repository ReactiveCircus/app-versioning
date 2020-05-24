package io.github.reactivecircus.appversioning

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AppVersioningPluginTest {

    private val rootProject = ProjectBuilder.builder().withName("root").build()
    private val appProject = ProjectBuilder.builder().withParent(rootProject).withName("app").build()
    private val libraryProject = ProjectBuilder.builder().withParent(rootProject).withName("library").build()

    @Test
    fun `plugin registers tasks for Android Application without product flavor`() {
        appProject.pluginManager.apply(AppPlugin::class.java)
        appProject.pluginManager.apply(AppVersioningPlugin::class.java)

        appProject.createAndroidAppProject(hasProductFlavor = false)

        (appProject as DefaultProject).evaluate()

        assertTaskRegistered(appProject, "generateAppVersionInfoForRelease")

        assertTaskNotRegistered(appProject, "generateAppVersionInfoForDebug")
    }

    @Test
    fun `plugin registers tasks for Android Application with product flavor`() {
        appProject.pluginManager.apply(AppPlugin::class.java)
        appProject.pluginManager.apply(AppVersioningPlugin::class.java)

        appProject.createAndroidAppProject(hasProductFlavor = true)

        (appProject as DefaultProject).evaluate()

        assertTaskRegistered(appProject, "generateAppVersionInfoForProdRelease")
        assertTaskRegistered(appProject, "generateAppVersionInfoForMockRelease")

        assertTaskNotRegistered(appProject, "generateAppVersionInfoForProdDebug")
        assertTaskNotRegistered(appProject, "generateAppVersionInfoForMockDebug")
    }

    @Test
    fun `plugin registers tasks for Debug build type when releaseBuildOnly is false`() {
        appProject.pluginManager.apply(AppPlugin::class.java)
        appProject.pluginManager.apply(AppVersioningPlugin::class.java)

        appProject.createAndroidAppProject(hasProductFlavor = true)
        appProject.extensions.getByType(AppVersioningExtension::class.java).apply {
            releaseBuildOnly.set(false)
        }

        (appProject as DefaultProject).evaluate()

        assertTaskRegistered(appProject, "generateAppVersionInfoForProdRelease")
        assertTaskRegistered(appProject, "generateAppVersionInfoForMockRelease")

        assertTaskRegistered(appProject, "generateAppVersionInfoForProdDebug")
        assertTaskRegistered(appProject, "generateAppVersionInfoForMockDebug")
    }

    @Test
    fun `plugin throws ProjectConfigurationException when maxDigits is lower than minimum maxDigits required`() {
        appProject.pluginManager.apply(AppPlugin::class.java)
        appProject.pluginManager.apply(AppVersioningPlugin::class.java)

        appProject.createAndroidAppProject(hasProductFlavor = false)
        appProject.extensions.getByType(AppVersioningExtension::class.java).apply {
            maxDigits.set(AppVersioningExtension.MAX_DIGITS_RANGE_MIN - 1)
        }

        assertFailsWith<ProjectConfigurationException>("`maxDigits` must be at least `1` and at most `4`") {
            (appProject as DefaultProject).evaluate()
        }
    }

    @Test
    fun `plugin throws ProjectConfigurationException when maxDigits is higher than maximum maxDigits required`() {
        appProject.pluginManager.apply(AppPlugin::class.java)
        appProject.pluginManager.apply(AppVersioningPlugin::class.java)

        appProject.createAndroidAppProject(hasProductFlavor = false)
        appProject.extensions.getByType(AppVersioningExtension::class.java).apply {
            maxDigits.set(AppVersioningExtension.MAX_DIGITS_RANGE_MAX + 1)
        }

        assertFailsWith<ProjectConfigurationException>("`maxDigits` must be at least `1` and at most `4`") {
            (appProject as DefaultProject).evaluate()
        }
    }

    @Test
    fun `plugin cannot be applied to project without Android App plugin`() {
        rootProject.pluginManager.apply(AppVersioningPlugin::class.java)
        assertFailsWith<ProjectConfigurationException> {
            (rootProject as DefaultProject).evaluate()
        }

        libraryProject.pluginManager.apply(LibraryPlugin::class.java)
        libraryProject.pluginManager.apply(AppVersioningPlugin::class.java)

        assertFailsWith<ProjectConfigurationException> {
            (libraryProject as DefaultProject).evaluate()
        }
    }


    private fun assertTaskRegistered(project: Project, taskName: String) {
        val task = project.tasks.getByName(taskName)
        assertNotNull(task)
        assertEquals(APP_VERSIONING_TASK_GROUP, task.group)
        assertEquals(GenerateAppVersionInfo.TASK_DESCRIPTION, task.description)
    }

    private fun assertTaskNotRegistered(project: Project, taskName: String) {
        val task = project.tasks.findByName(taskName)
        assertNull(task)
    }
}
