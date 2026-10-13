package ir.farsroidx.view

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import com.intellij.util.xmlb.XmlSerializer
import ir.farsroidx.ViewConfig
import ir.farsroidx.view.model.ModuleLayout
import ir.farsroidx.view.model.SourceRoot
import ir.farsroidx.view.nodes.BuildModuleNode
import ir.farsroidx.view.nodes.PackageNode
import ir.farsroidx.view.nodes.SourceFileNode
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JavaSourceRootType

class GeneratedSourcesTest : BasePlatformTestCase() {

    override fun createTempDirTestFixture(): TempDirTestFixture {

        return TempDirTestFixtureImpl()

    }

    override fun tearDown() {

        try {

            project.service<GeneratedSourcesSettings>().showGeneratedSources = false

        } finally {

            super.tearDown()

        }

    }

    fun testImportedGeneratedRootUsesMetadataRatherThanDirectoryName() {

        myFixture.addFileToProject("custom-output/com/example/Model.java", "package com.example; public class Model {}")

        myFixture.addFileToProject("generated/com/manual/App.java", "package com.manual; public class App {}")

        val output = myFixture.tempDirFixture.getFile("custom-output")!!

        val ordinary = myFixture.tempDirFixture.getFile("generated")!!

        registerRoots(output, ordinary)

        val roots = SourceRootCollector.collect(listOf(module))

        assertTrue(roots.single { it.directory == output }.isGenerated)

        assertFalse(roots.single { it.directory == ordinary }.isGenerated)

    }

    fun testGeneratedPackagesAreHiddenByDefaultAndReturnWhenEnabled() {

        myFixture.addFileToProject("custom-output/com/example/Model.java", "package com.example; public class Model {}")

        val main = myFixture.addFileToProject("src/main/java/com/main/App.java", "package com.main; public class App {}")

        registerRoots(myFixture.tempDirFixture.getFile("custom-output")!!, main.virtualFile.parent.parent)

        val node = moduleNode(SourceRootCollector.collect(listOf(module)))

        val settings = project.service<GeneratedSourcesSettings>()

        assertFalse(settings.showGeneratedSources)

        assertEquals(1, node.getChildrenImpl().filterIsInstance<PackageNode>().size)

        assertFalse(node.getChildrenImpl().filterIsInstance<PsiDirectoryNode>().any {

            it.virtualFile?.name == "custom-output"

        })

        settings.showGeneratedSources = true

        val packages = node.getChildrenImpl().filterIsInstance<PackageNode>()

        assertEquals(2, packages.size)

        val generated = packages.single { it.isGenerated }

        val presentation = PresentationData()

        generated.update(presentation)

        assertEquals("com.example (generated)", presentation.coloredText.joinToString("") { it.text })

        settings.showGeneratedSources = false

        assertEquals(1, node.getChildrenImpl().filterIsInstance<PackageNode>().size)

    }

    fun testGeneratedTestFilesKeepBothLabels() {

        val file = myFixture.addFileToProject("custom-output/Case.txt", "case")

        val root = SourceRoot(file.virtualFile.parent, true, false, isGenerated = true)

        val node = moduleNode(listOf(root))

        assertTrue(node.getChildrenImpl().isEmpty())

        project.service<GeneratedSourcesSettings>().showGeneratedSources = true

        val generated = node.getChildrenImpl().filterIsInstance<SourceFileNode>().single()

        val presentation = PresentationData()

        generated.update(presentation)

        assertEquals("Case.txt (test) (generated)", presentation.coloredText.joinToString("") { it.text })

    }

    fun testNestedGeneratedRootsDoNotLeakThroughOrdinaryPackages() {

        myFixture.addFileToProject("src/main/java/com/example/App.java", "package com.example; public class App {}")

        myFixture.addFileToProject("src/main/java/com/example/output/Model.java", "package com.example.output; public class Model {}")

        val main = myFixture.tempDirFixture.getFile("src/main/java")!!

        val generated = myFixture.tempDirFixture.getFile("src/main/java/com/example/output")!!

        val node = moduleNode(
            listOf(
                SourceRoot(main, false, false),
                SourceRoot(generated, false, false, isGenerated = true)
            )
        )

        val packageNode = node.getChildrenImpl().filterIsInstance<PackageNode>().single()

        assertTrue(packageNode.getChildrenImpl().filterIsInstance<PsiFileNode>().any { it.value!!.name == "App.java" })

        assertFalse(packageNode.getChildrenImpl().filterIsInstance<PsiDirectoryNode>().any { it.virtualFile == generated })

        project.service<GeneratedSourcesSettings>().showGeneratedSources = true

        val visibleChildren = node.getChildrenImpl()

        assertTrue(visibleChildren.filterIsInstance<SourceFileNode>().single().isGenerated)

        assertFalse(visibleChildren.filterIsInstance<PackageNode>().single()
            .getChildrenImpl().filterIsInstance<PsiDirectoryNode>().any { it.virtualFile == generated })

    }

    fun testGeneratedPreferencePersistsAndDefaultsToDisabled() {

        val settings = GeneratedSourcesSettings()

        assertFalse(settings.showGeneratedSources)

        settings.showGeneratedSources = true

        val xml = XmlSerializer.serialize(settings.getState())

        val restored = GeneratedSourcesSettings()

        restored.loadState(XmlSerializer.deserialize(xml, GeneratedSourcesSettings.SettingsState::class.java))

        assertTrue(restored.showGeneratedSources)

    }

    fun testToggleIsRegisteredInNativeAppearanceMenu() {

        val manager = ActionManager.getInstance()

        val action = manager.getAction("TreeView.ShowGeneratedSources")

        assertNotNull(action)

        val group = manager.getAction("ProjectView.ToolWindow.Appearance.Actions") as DefaultActionGroup

        assertTrue(group.getChildren(null).contains(action))

    }

    private fun registerRoots(generated: VirtualFile, ordinary: VirtualFile) {

        ApplicationManager.getApplication().runWriteAction {

            val model = ModuleRootManager.getInstance(module).modifiableModel

            try {

                val entry = model.addContentEntry(myFixture.tempDirFixture.getFile("")!!)

                entry.addSourceFolder(generated, JavaSourceRootType.SOURCE, JavaSourceRootProperties("", true))

                entry.addSourceFolder(ordinary, JavaSourceRootType.SOURCE)

                model.commit()

            } finally {

                if (!model.isDisposed) {

                    model.dispose()

                }

            }

        }

    }

    private fun moduleNode(roots: List<SourceRoot>): BuildModuleNode {

        val directory = myFixture.tempDirFixture.getFile("")!!

        val layout = ModuleLayout("root", directory, roots, emptySet(), emptySet())

        return BuildModuleNode(
            ViewConfig(project, ViewSettings.DEFAULT),
            PsiManager.getInstance(project).findDirectory(directory)!!,
            layout,
            setOf(directory)
        )

    }

}
