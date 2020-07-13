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

        assertTaskRegistered(
            appProject,
            taskName = "generateAppVersionInfoForRelease",
            taskDescription = "${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the release variant."
        )
        assertTaskRegistered(
            appProject,
            taskName = "printAppVersionInfoForRelease",
            taskDescription = "${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the release variant."
        )

        assertTaskNotRegistered(appProject, "generateAppVersionInfoForDebug")
        assertTaskNotRegistered(appProject, "printAppVersionInfoForDebug")
    }

    @Test
    fun `plugin registers tasks for Android Application with product flavor`() {
        appProject.pluginManager.apply(AppPlugin::class.java)
        appProject.pluginManager.apply(AppVersioningPlugin::class.java)

        appProject.createAndroidAppProject(hasProductFlavor = true)

        (appProject as DefaultProject).evaluate()

        // prodRelease
        assertTaskRegistered(
            appProject,
            taskName = "generateAppVersionInfoForProdRelease",
            taskDescription = "${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the prodRelease variant."
        )
        assertTaskRegistered(
            appProject,
            taskName = "printAppVersionInfoForProdRelease",
            taskDescription = "${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the prodRelease variant."
        )

        // mockRelease
        assertTaskRegistered(
            appProject,
            taskName = "generateAppVersionInfoForMockRelease",
            taskDescription = "${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the mockRelease variant."
        )
        assertTaskRegistered(
            appProject,
            taskName = "printAppVersionInfoForMockRelease",
            taskDescription = "${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the mockRelease variant."
        )

        assertTaskNotRegistered(appProject, "generateAppVersionInfoForProdDebug")
        assertTaskNotRegistered(appProject, "generateAppVersionInfoForMockDebug")
        assertTaskNotRegistered(appProject, "printAppVersionInfoForProdDebug")
        assertTaskNotRegistered(appProject, "printAppVersionInfoForMockDebug")
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

        // prodRelease
        assertTaskRegistered(
            appProject,
            taskName = "generateAppVersionInfoForProdRelease",
            taskDescription = "${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the prodRelease variant."
        )
        assertTaskRegistered(
            appProject,
            taskName = "printAppVersionInfoForProdRelease",
            taskDescription = "${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the prodRelease variant."
        )

        // mockRelease
        assertTaskRegistered(
            appProject,
            taskName = "generateAppVersionInfoForMockRelease",
            taskDescription = "${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the mockRelease variant."
        )
        assertTaskRegistered(
            appProject,
            taskName = "printAppVersionInfoForMockRelease",
            taskDescription = "${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the mockRelease variant."
        )

        // prodDebug
        assertTaskRegistered(
            appProject,
            taskName = "generateAppVersionInfoForProdDebug",
            taskDescription = "${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the prodDebug variant."
        )
        assertTaskRegistered(
            appProject,
            taskName = "printAppVersionInfoForProdDebug",
            taskDescription = "${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the prodDebug variant."
        )

        // mockDebug
        assertTaskRegistered(
            appProject,
            taskName = "generateAppVersionInfoForMockDebug",
            taskDescription = "${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the mockDebug variant."
        )
        assertTaskRegistered(
            appProject,
            taskName = "printAppVersionInfoForMockDebug",
            taskDescription = "${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the mockDebug variant."
        )
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

    private fun assertTaskRegistered(project: Project, taskName: String, taskDescription: String) {
        val task = project.tasks.getByName(taskName)
        assertNotNull(task)
        assertEquals(APP_VERSIONING_TASK_GROUP, task.group)
        assertEquals(taskDescription, task.description)
    }

    private fun assertTaskNotRegistered(project: Project, taskName: String) {
        val task = project.tasks.findByName(taskName)
        assertNull(task)
    }
}
