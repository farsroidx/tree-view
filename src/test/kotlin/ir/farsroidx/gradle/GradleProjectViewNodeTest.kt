package ir.farsroidx.gradle

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import ir.farsroidx.ViewConfig
import ir.farsroidx.view.GeneratedSourcesSettings
import ir.farsroidx.view.nodes.PackageNode
import ir.farsroidx.view.nodes.BuildFilesNode
import ir.farsroidx.view.nodes.BuildModuleNode
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

class GradleProjectViewNodeTest : BasePlatformTestCase() {

    override fun setUp() {

        super.setUp()

        ApplicationManager.getApplication().runWriteAction {

            GradleSettings.getInstance(project).serviceDirectoryPath = "${myFixture.tempDirFixture.getFile("")!!.path}/unused-home"

        }

    }

    override fun createTempDirTestFixture(): TempDirTestFixture {

        return TempDirTestFixtureImpl()

    }

    override fun tearDown() {

        try {

            project.service<GeneratedSourcesSettings>().showGeneratedSources = false

            ApplicationManager.getApplication().runWriteAction {

                GradleSettings.getInstance(project).linkedProjectsSettings = emptyList()

                GradleSettings.getInstance(project).serviceDirectoryPath = null

                ExternalSystemModulePropertyManager.getInstance(module).unlinkExternalOptions()

            }

        } finally {

            super.tearDown()

        }

    }

    fun testGroovyBuildFilesAreGrouped() {

        myFixture.addFileToProject("build.gradle", "plugins {}")

        myFixture.addFileToProject("settings.gradle", "rootProject.name = 'sample'")

        linkProject()

        val group = rootNode().children.filterIsInstance<BuildFilesNode>().single()

        assertEquals(
            setOf("build.gradle", "settings.gradle"),
            group.children.filterIsInstance<PsiFileNode>().map { it.value!!.name }.toSet()
        )

    }

    fun testKotlinBuildFilesAreGrouped() {

        myFixture.addFileToProject("build.gradle.kts", "plugins {}")

        myFixture.addFileToProject("settings.gradle.kts", "rootProject.name = \"sample\"")

        myFixture.addFileToProject("gradle.properties", "version=1.0")

        myFixture.addFileToProject("gradle/libs.versions.toml", "[versions]")

        linkProject()

        val group = rootNode().children.filterIsInstance<BuildFilesNode>().single()

        assertEquals(
            setOf("build.gradle.kts", "settings.gradle.kts", "gradle.properties", "libs.versions.toml"),
            group.children.filterIsInstance<PsiFileNode>().map { it.value!!.name }.toSet()
        )

    }

    fun testGlobalPropertiesUseConfiguredGradleUserHome() {

        myFixture.addFileToProject("build.gradle.kts", "plugins {}")

        val global = myFixture.addFileToProject("user-home/gradle.properties", "org.gradle.parallel=true")

        linkProject()

        ApplicationManager.getApplication().runWriteAction {

            GradleSettings.getInstance(project).serviceDirectoryPath = global.virtualFile.parent.path

        }

        val group = rootNode().children.filterIsInstance<BuildFilesNode>().single()

        val node = group.children.filterIsInstance<PsiFileNode>().single { it.virtualFile == global.virtualFile }

        val presentation = com.intellij.ide.projectView.PresentationData()

        node.update(presentation)

        assertEquals("gradle.properties (Gradle User Home)", presentation.coloredText.joinToString("") { it.text })

    }

    fun testCustomImportedSourceSetUsesTheIdeSourceRoot() {

        myFixture.addFileToProject("build.gradle.kts", "plugins {}")

        val source = myFixture.addFileToProject("custom/integration/Case.txt", "case")

        val directory = myFixture.tempDirFixture.getFile("")!!

        linkProject()

        ApplicationManager.getApplication().runWriteAction {

            val properties = ExternalSystemModulePropertyManager.getInstance(module)

            properties.setExternalId(GradleConstants.SYSTEM_ID)

            properties.setLinkedProjectPath(directory.path)

            val model = ModuleRootManager.getInstance(module).modifiableModel

            try {

                model.addContentEntry(directory).addSourceFolder(
                    source.virtualFile.parent,
                    JavaSourceRootType.TEST_SOURCE
                )

                model.commit()

            } finally {

                if (!model.isDisposed) {

                    model.dispose()

                }

            }

        }

        val node = rootNode().children.filterIsInstance<BuildModuleNode>().single()

        assertTrue(node.getChildrenImpl().filterIsInstance<PsiFileNode>().any { it.value == source })

    }

    fun testImportedGradleGeneratedPackagesFollowVisibilitySetting() {

        myFixture.addFileToProject("build.gradle.kts", "plugins {}")

        myFixture.addFileToProject("build/generated/sources/com/example/Model.java", "package com.example; public class Model {}")

        val directory = myFixture.tempDirFixture.getFile("")!!

        val generated = myFixture.tempDirFixture.getFile("build/generated/sources")!!

        linkProject()

        ApplicationManager.getApplication().runWriteAction {

            val properties = ExternalSystemModulePropertyManager.getInstance(module)

            properties.setExternalId(GradleConstants.SYSTEM_ID)

            properties.setLinkedProjectPath(directory.path)

            val model = ModuleRootManager.getInstance(module).modifiableModel

            try {

                model.addContentEntry(directory).addSourceFolder(
                    generated,
                    JavaSourceRootType.SOURCE,
                    JavaSourceRootProperties("", true)
                )

                model.commit()

            } finally {

                if (!model.isDisposed) {

                    model.dispose()

                }

            }

        }

        val node = rootNode().children.filterIsInstance<BuildModuleNode>().single()

        assertTrue(node.getChildrenImpl().isEmpty())

        project.service<GeneratedSourcesSettings>().showGeneratedSources = true

        assertTrue(node.getChildrenImpl().filterIsInstance<PackageNode>().single().isGenerated)

    }

    fun testImportedModelUsesRemappedModuleDirectoryWithoutDuplicatingSourceSets() {

        myFixture.addFileToProject("root/build.gradle.kts", "plugins {}")

        myFixture.addFileToProject("physical-child/build.gradle", "plugins {}")

        myFixture.addFileToProject("physical-child/gradle.properties", "farsroidx.order=5")

        myFixture.addFileToProject("another-child/build.gradle.kts", "plugins {}")

        myFixture.addFileToProject("another-child/gradle.properties", "farsroidx.order=10")

        val rootDirectory = myFixture.tempDirFixture.getFile("root")!!

        val childDirectory = myFixture.tempDirFixture.getFile("physical-child")!!

        val projectData = ProjectData(
            GradleConstants.SYSTEM_ID,
            "root",
            rootDirectory.path,
            rootDirectory.path
        )

        val model = DataNode(ProjectKeys.PROJECT, projectData, null)

        val anotherDirectory = myFixture.tempDirFixture.getFile("another-child")!!

        model.createChild(
            ProjectKeys.MODULE,
            ModuleData(
                ":another-child",
                GradleConstants.SYSTEM_ID,
                "JAVA_MODULE",
                "another-child",
                anotherDirectory.path,
                anotherDirectory.path
            )
        )

        model.createChild(
            ProjectKeys.MODULE,
            ModuleData(
                ":logical-child",
                GradleConstants.SYSTEM_ID,
                "JAVA_MODULE",
                "logical-child",
                childDirectory.path,
                childDirectory.path
            )
        )

        model.createChild(
            ProjectKeys.MODULE,
            ModuleData(
                ":logical-child:main",
                GradleConstants.SYSTEM_ID,
                "JAVA_MODULE",
                "logical-child.main",
                childDirectory.path,
                childDirectory.path
            )
        )

        ExternalProjectsDataStorage.getInstance(project).update(
            object : ExternalProjectInfo {

                override fun getProjectSystemId(): ProjectSystemId = GradleConstants.SYSTEM_ID

                override fun getExternalProjectPath(): String = rootDirectory.path

                override fun getExternalProjectStructure(): DataNode<ProjectData> = model

                override fun getLastSuccessfulImportTimestamp(): Long = IMPORT_TIMESTAMP

                override fun getLastImportTimestamp(): Long = IMPORT_TIMESTAMP

                override fun getBuildNumber(): String = TEST_BUILD_NUMBER

                override fun copy(): ExternalProjectInfo = this

            }
        )

        ApplicationManager.getApplication().runWriteAction {

            GradleSettings.getInstance(project).linkedProjectsSettings = listOf(
                GradleProjectSettings(rootDirectory.path)
            )

        }

        val modules = rootNode().children.filterIsInstance<BuildModuleNode>()

        assertEquals(listOf(rootDirectory, childDirectory, anotherDirectory), modules.map { it.virtualFile })

        assertEquals(3, modules.size)

        val child = modules.single { it.virtualFile == childDirectory }

        val presentation = com.intellij.ide.projectView.PresentationData()

        child.update(presentation)

        assertEquals("logical-child", presentation.coloredText.joinToString("") { it.text })

        val builds = rootNode().children.filterIsInstance<BuildFilesNode>().single()

        val build = builds.children.filterIsInstance<PsiFileNode>()
            .single { it.virtualFile == childDirectory.findChild("build.gradle") }

        build.update(presentation)

        assertEquals("build.gradle (logical-child)", presentation.coloredText.joinToString("") { it.text })

    }

    private fun linkProject() {

        val directory = myFixture.tempDirFixture.getFile("")!!

        ApplicationManager.getApplication().runWriteAction {

            GradleSettings.getInstance(project).linkedProjectsSettings = listOf(
                GradleProjectSettings(directory.path)
            )

        }

    }

    private fun rootNode(): GradleProjectViewNode {

        return GradleProjectViewNode(ViewConfig(project, ViewSettings.DEFAULT))

    }

    companion object {

        private const val IMPORT_TIMESTAMP = 1L

        private const val TEST_BUILD_NUMBER = "262"

    }

}
