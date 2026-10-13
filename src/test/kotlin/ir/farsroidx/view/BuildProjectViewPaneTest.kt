package ir.farsroidx.view

import com.intellij.icons.AllIcons
import com.intellij.codeInsight.daemon.impl.WolfTheProblemSolverImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.ide.SelectInTarget
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.AbstractTreeStructureBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.problems.WolfTheProblemSolver
import com.intellij.psi.PsiManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.replaceService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import ir.farsroidx.ViewConfig
import ir.farsroidx.maven.MavenSelectInTarget
import ir.farsroidx.view.model.ModuleLayout
import ir.farsroidx.view.nodes.BuildFilesNode
import ir.farsroidx.view.nodes.BuildProjectNode
import ir.farsroidx.view.nodes.PackageNode
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class BuildProjectViewPaneTest : BasePlatformTestCase() {

    override fun createTempDirTestFixture(): TempDirTestFixture {

        return TempDirTestFixtureImpl()

    }

    fun testNativeSelectionProvidesAllSelectedPsiElements() {

        val first = myFixture.addFileToProject("first.txt", "first")

        val second = myFixture.addFileToProject("second.txt", "second")

        val pane = createPane()

        PlatformTestUtil.waitForCallback(pane.selectCB(first, first.virtualFile, false))

        val firstPath = pane.tree!!.selectionPath

        assertNotNull(firstPath)

        PlatformTestUtil.waitForCallback(pane.selectCB(second, second.virtualFile, false))

        val secondPath = pane.tree!!.selectionPath

        pane.tree!!.selectionPaths = arrayOf(firstPath, secondPath)

        assertEquals(
            setOf(first.virtualFile, second.virtualFile),
            pane.selectedPSIElements.map { it.containingFile.virtualFile }.toSet()
        )

    }

    fun testReturningToPaneReusesTheSameTreeComponent() {

        myFixture.addFileToProject("first.txt", "first")

        val pane = createPane()

        val first = pane.createComponent()

        val second = pane.createComponent()

        assertSame(first, second)

        assertNotNull(pane.tree)

    }

    fun testCompactedSourcePackageSurvivesNativeTreeValidation() {

        val file = myFixture.addFileToProject(
            "src/main/java/com/example/app/Main.java",
            "package com.example.app; public class Main {}"
        )

        val pane = createPane(true)

        PlatformTestUtil.waitForCallback(pane.selectCB(file, file.virtualFile, false))

        assertNotNull("Source file must be selectable in the native tree.", pane.tree!!.selectionPath)

        assertEquals(file.virtualFile, pane.selectedPSIElements.single().containingFile.virtualFile)

        assertTrue(pane.singleSelectedPathUserObjects.orEmpty().any { it is PackageNode })

    }

    fun testSourceAndResourceSectionsKeepRequiredOrderInNativeTree() {

        val main = myFixture.addFileToProject("src/main/java/zmain/Main.java", "package zmain; public class Main {}")

        val test = myFixture.addFileToProject("src/test/java/atest/Case.java", "package atest; public class Case {}")

        val resources = myFixture.addFileToProject("src/main/resources/main.txt", "main")

        val testResources = myFixture.addFileToProject("src/test/resources/test.txt", "test")

        val pane = createPane(true)

        PlatformTestUtil.waitForCallback(pane.selectCB(main, main.virtualFile, false))

        val mainPackage = pane.tree!!.selectionPath.parentPath.lastPathComponent as DefaultMutableTreeNode

        PlatformTestUtil.waitForCallback(pane.selectCB(test, test.virtualFile, false))

        val testPackage = pane.tree!!.selectionPath.parentPath.lastPathComponent as DefaultMutableTreeNode

        assertSame(mainPackage.parent, testPackage.parent)

        assertTrue(mainPackage.parent.getIndex(mainPackage) < testPackage.parent.getIndex(testPackage))

        PlatformTestUtil.waitForCallback(pane.selectCB(resources, resources.virtualFile, false))

        val resourceNode = pane.tree!!.selectionPath.parentPath.lastPathComponent as DefaultMutableTreeNode

        PlatformTestUtil.waitForCallback(pane.selectCB(testResources, testResources.virtualFile, false))

        val testResourceNode = pane.tree!!.selectionPath.parentPath.lastPathComponent as DefaultMutableTreeNode

        assertSame(mainPackage.parent, resourceNode.parent)

        assertSame(mainPackage.parent, testResourceNode.parent)

        assertTrue(testPackage.parent.getIndex(testPackage) < resourceNode.parent.getIndex(resourceNode))

        assertTrue(resourceNode.parent.getIndex(resourceNode) < testResourceNode.parent.getIndex(testResourceNode))

    }

    fun testProblemPresentationClearsWithoutSavingDocument() {

        val file = myFixture.addFileToProject("Main.java", "public class Main {}")

        project.replaceService(
            WolfTheProblemSolver::class.java,
            WolfTheProblemSolverImpl.createTestInstance(project),
            testRootDisposable
        )

        val pane = createPane()

        PlatformTestUtil.waitForCallback(pane.selectCB(file, file.virtualFile, false))

        val solver = WolfTheProblemSolver.getInstance(project)

        val documentManager = FileDocumentManager.getInstance()

        val document = documentManager.getDocument(file.virtualFile)!!

        WriteCommandAction.runWriteCommandAction(project) {

            document.insertString(document.textLength, "\n")

        }

        assertTrue(documentManager.isDocumentUnsaved(document))

        val source = Any()

        project.extensionArea.getExtensionPoint<Condition<VirtualFile>>(HIGHLIGHT_FILTER).registerExtension(
            Condition { it == file.virtualFile },
            testRootDisposable
        )

        PlatformTestUtil.waitForFuture(
            ApplicationManager.getApplication().executeOnPooledThread {

                solver.reportProblemsFromExternalSource(file.virtualFile, source)

            }
        )

        assertTrue(solver.isProblemFile(file.virtualFile))

        PlatformTestUtil.waitWithEventsDispatching(
            "The tree should show the reported problem.",
            { selectedNode(pane).presentation.textAttributesKey != null },
            TEST_TIMEOUT
        )

        PlatformTestUtil.waitForFuture(
            ApplicationManager.getApplication().executeOnPooledThread {

                solver.clearProblemsFromExternalSource(file.virtualFile, source)

            }
        )

        PlatformTestUtil.waitWithEventsDispatching(
            "The tree should clear the problem without a save.",
            { selectedNode(pane).presentation.textAttributesKey == null },
            TEST_TIMEOUT
        )

        assertTrue(documentManager.isDocumentUnsaved(document))

    }

    fun testGeneratedPackageRemainsSelectableAfterNativeTreeRefresh() {

        val main = myFixture.addFileToProject("src/main/java/com/main/App.java", "package com.main; public class App {}")

        val generated = myFixture.addFileToProject("generated/java/com/example/Model.java", "package com.example; public class Model {}")

        val pane = createPane(true)

        PlatformTestUtil.waitForCallback(pane.selectCB(main, main.virtualFile, false))

        val mainPackage = pane.tree!!.selectionPath.parentPath.lastPathComponent as DefaultMutableTreeNode

        val moduleNode = mainPackage.parent as DefaultMutableTreeNode

        assertFalse(moduleNode.children().asSequence().map { (it as DefaultMutableTreeNode).userObject }
            .filterIsInstance<PackageNode>().any { it.isGenerated })

        project.service<GeneratedSourcesSettings>().showGeneratedSources = true

        PlatformTestUtil.waitForCallback(pane.updateFromRoot(true))

        PlatformTestUtil.waitForCallback(pane.selectCB(generated, generated.virtualFile, false))

        assertNotNull(pane.tree!!.selectionPath)

        assertTrue(pane.singleSelectedPathUserObjects.orEmpty().filterIsInstance<PackageNode>().single().isGenerated)

        project.service<GeneratedSourcesSettings>().showGeneratedSources = false

        PlatformTestUtil.waitForCallback(pane.updateFromRoot(true))

        PlatformTestUtil.waitForCallback(pane.selectCB(main, main.virtualFile, false))

        val refreshedPackage = pane.tree!!.selectionPath.parentPath.lastPathComponent as DefaultMutableTreeNode

        val refreshedModule = refreshedPackage.parent as DefaultMutableTreeNode

        assertFalse(refreshedModule.children().asSequence().map { (it as DefaultMutableTreeNode).userObject }
            .filterIsInstance<PackageNode>().any { it.isGenerated })

    }

    fun testRefreshCallbackCompletesAfterTheTreeUpdate() {

        val file = myFixture.addFileToProject("first.txt", "first")

        val pane = createPane()

        PlatformTestUtil.waitForCallback(pane.selectCB(file, file.virtualFile, false))

        myFixture.addFileToProject("second.txt", "second")

        val callback = pane.updateFromRoot(true)

        PlatformTestUtil.waitForCallback(callback)

        assertTrue(callback.isDone)

        val second = myFixture.tempDirFixture.getFile("second.txt")!!

        PlatformTestUtil.waitForCallback(
            pane.selectCB(PsiManager.getInstance(project).findFile(second), second, false)
        )

        assertEquals(second, pane.selectedPSIElements.single().containingFile.virtualFile)

    }

    private fun selectedNode(pane: BuildProjectViewPane): AbstractTreeNode<*> {

        val path: TreePath = pane.tree!!.selectionPath

        val node = path.lastPathComponent as DefaultMutableTreeNode

        return node.userObject as AbstractTreeNode<*>

    }

    private fun createPane(withSourceRoots: Boolean = false): BuildProjectViewPane {

        val directory = myFixture.tempDirFixture.getFile("")!!

        ApplicationManager.getApplication().runWriteAction {

            val model = ModuleRootManager.getInstance(module).modifiableModel

            try {

                val sourceDirectory = if (withSourceRoots) {

                    directory.findFileByRelativePath("src/main/java")!!

                } else {

                    directory

                }

                model.addContentEntry(directory).addSourceFolder(sourceDirectory, JavaSourceRootType.SOURCE)

                if (withSourceRoots) {

                    directory.findFileByRelativePath("generated/java")?.let { generatedDirectory ->

                        model.contentEntries.single { it.file == directory }.addSourceFolder(
                            generatedDirectory,
                            JavaSourceRootType.SOURCE,
                            JavaSourceRootProperties("", true)
                        )

                    }

                    directory.findFileByRelativePath("src/test/java")?.let { testDirectory ->

                        model.contentEntries.single { it.file == directory }
                            .addSourceFolder(testDirectory, JavaSourceRootType.TEST_SOURCE)

                    }

                }

                model.commit()

            } finally {

                if (!model.isDisposed) {

                    model.dispose()

                }

            }

        }

        val pane = object : BuildProjectViewPane(project) {

            override fun getId(): String = TEST_PANE_ID

            override fun getTitle(): String = "Test Tree"

            override fun getIcon(): Icon = AllIcons.Nodes.Folder

            override fun getWeight(): Int = 100

            override fun createSelectInTarget(): SelectInTarget = MavenSelectInTarget(project, TEST_PANE_ID)

            override fun createStructure(): AbstractTreeStructureBase {

                return object : ProjectTreeStructure(project, TEST_PANE_ID) {

                    override fun isHideEmptyMiddlePackages(): Boolean = withSourceRoots

                    override fun createRoot(
                        project: Project,
                        viewSettings: ViewSettings
                    ): AbstractTreeNode<*> {

                        val config = ViewConfig(project, viewSettings)

                        return object : BuildProjectNode(config) {

                            override fun collectLayouts(): List<ModuleLayout> {

                                return listOf(
                                    ModuleLayout(
                                        "test",
                                        directory,
                                        if (withSourceRoots) {

                                            (SourceRootCollector.collect(listOf(module)) +
                                                SourceRootCollector.conventional(directory)).distinctBy { it.directory }

                                        } else {

                                            emptyList()

                                        },
                                        emptySet(),
                                        emptySet()
                                    )
                                )

                            }

                            override fun buildFilesNode(layouts: List<ModuleLayout>): BuildFilesNode {

                                return BuildFilesNode(config, "Builds", AllIcons.Nodes.Folder, emptyList()) {

                                    emptyList()

                                }

                            }

                        }

                    }

                }

            }

        }

        Disposer.register(testRootDisposable, pane)

        pane.createComponent()

        PlatformTestUtil.waitForCallback(pane.updateFromRoot(true))

        return pane

    }

    companion object {

        private const val TEST_PANE_ID = "TREE_VIEW_TEST"

        private const val TEST_TIMEOUT = 10

        private const val HIGHLIGHT_FILTER = "com.intellij.problemFileHighlightFilter"

    }

}
