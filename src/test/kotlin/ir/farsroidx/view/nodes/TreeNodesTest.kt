package ir.farsroidx.view.nodes

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import ir.farsroidx.ViewConfig
import ir.farsroidx.view.SourceRootCollector
import ir.farsroidx.view.model.ModuleLayout
import ir.farsroidx.view.model.SourceRoot

class TreeNodesTest : BasePlatformTestCase() {

    fun testFilesDirectlyInSourceRootAreVisible() {

        val sourceFile = myFixture.addFileToProject("src/main/java/Root.txt", "root")

        val root = SourceRoot(sourceFile.virtualFile.parent, false, false)

        val nodes = SourceNodes.children(config(), root)

        assertTrue(nodes.filterIsInstance<PsiFileNode>().any { it.value == sourceFile })

    }

    fun testCompactedPackageRepresentsEveryDirectoryInItsChain() {

        val sourceFile = myFixture.addFileToProject("src/main/java/com/example/app/Main.txt", "main")

        val rootDirectory = myFixture.tempDirFixture.getFile("src/main/java")!!

        val nodes = SourceNodes.children(config(), SourceRoot(rootDirectory, false, false))

        val node = nodes.filterIsInstance<PackageNode>().single()

        val intermediateDirectory = myFixture.tempDirFixture.getFile("src/main/java/com/example")!!

        assertTrue(node.canRepresent(intermediateDirectory))

        assertTrue(node.contains(sourceFile.virtualFile))

        assertFalse(node.canRepresent(sourceFile.virtualFile))

        assertEquals(sourceFile.virtualFile.parent, node.virtualFile)

    }

    fun testNonstandardSourceContentIsRetainedWithoutDuplicatingPackages() {

        myFixture.addFileToProject("src/main/java/com/example/Main.txt", "main")

        myFixture.addFileToProject("src/scripts/task.txt", "script")

        val directory = rootDirectory()

        val hiddenFiles = setOf(myFixture.tempDirFixture.getFile("src/main/java")!!)

        val nodes = ContentDirectoryNode.children(config(), directory, hiddenFiles)

        val sourceNode = nodes.filterIsInstance<ContentDirectoryNode>().single()

        val children = sourceNode.getChildrenImpl()

        assertEquals(1, children.size)

        assertEquals("scripts", (children.single() as PsiDirectoryNode).value!!.name)

    }

    fun testFullyRepresentedSourceContainerIsNotRepeated() {

        myFixture.addFileToProject("src/main/java/com/example/Main.txt", "main")

        val hiddenFiles = setOf(myFixture.tempDirFixture.getFile("src/main/java")!!)

        val nodes = ContentDirectoryNode.children(config(), rootDirectory(), hiddenFiles)

        assertFalse(nodes.filterIsInstance<PsiDirectoryNode>().any { it.value!!.name == "src" })

    }

    fun testBuildFileGroupHasNoFilesystemMoveTarget() {

        val file = myFixture.addFileToProject("pom.xml", "<project/>").virtualFile

        val node = BuildFilesNode(config(), "Maven Poms", AllIcons.Nodes.Folder, listOf(file)) {

            emptyList()

        }

        assertNull(node.virtualFile)

        assertFalse(node.canRepresent(rootDirectory().virtualFile))

        assertTrue(node.contains(file))

        assertEquals(listOf(file), node.roots.toList())

    }

    fun testModuleNodeTargetsItsOwnDirectory() {

        myFixture.addFileToProject("module/src/main/java/com/example/Main.txt", "main")

        val directoryFile = myFixture.tempDirFixture.getFile("module")!!

        val directory = PsiManager.getInstance(project).findDirectory(directoryFile)!!

        val layout = ModuleLayout(
            "module",
            directoryFile,
            SourceRootCollector.conventional(directoryFile),
            emptySet(),
            emptySet()
        )

        val node = BuildModuleNode(config(), directory, layout, setOf(directoryFile))

        assertEquals(directoryFile, node.virtualFile)

        assertEquals(directory, node.value)

    }

    fun testNativeMoveProcessorMovesFileUsingModuleNodeDestination() {

        val source = myFixture.addFileToProject("first/File.txt", "content")

        val targetFile = myFixture.tempDirFixture.findOrCreateDir("second")

        val target = PsiManager.getInstance(project).findDirectory(targetFile)!!

        val layout = ModuleLayout("second", targetFile, emptyList(), emptySet(), emptySet())

        val node = BuildModuleNode(config(), target, layout, setOf(targetFile))

        MoveFilesOrDirectoriesProcessor(
            project,
            arrayOf(source),
            node.value!!,
            false,
            false,
            null,
            null
        ).run()

        assertNotNull(targetFile.findChild("File.txt"))

        assertNull(myFixture.tempDirFixture.getFile("first/File.txt"))

    }

    fun testModuleRefreshDetectsNewSourceFiles() {

        val file = myFixture.addFileToProject("src/main/java/First.txt", "first")

        val directory = rootDirectory()

        val layout = ModuleLayout(
            "root",
            directory.virtualFile,
            listOf(SourceRoot(file.virtualFile.parent, false, false)),
            emptySet(),
            emptySet()
        )

        val node = BuildModuleNode(config(), directory, layout, setOf(directory.virtualFile))

        assertEquals(1, node.getChildrenImpl().filterIsInstance<PsiFileNode>().size)

        myFixture.addFileToProject("src/main/java/Second.txt", "second")

        assertEquals(2, node.getChildrenImpl().filterIsInstance<PsiFileNode>().size)

    }

    fun testConventionalRootsRecognizeGroovyKotlinAndTestResources() {

        myFixture.addFileToProject("src/main/groovy/App.txt", "app")

        myFixture.addFileToProject("src/main/kotlin/App.txt", "app")

        myFixture.addFileToProject("src/test/resources/test.properties", "value=true")

        val roots = SourceRootCollector.conventional(rootDirectory().virtualFile)

        assertEquals(3, roots.size)

        assertTrue(roots.any { it.directory.name == "groovy" && !it.isTest && !it.isResource })

        assertTrue(roots.any { it.directory.name == "kotlin" && !it.isTest && !it.isResource })

        assertTrue(roots.any { it.directory.name == "resources" && it.isTest && it.isResource })

    }

    fun testRenamedPackageIsRebuiltFromCurrentFilesystem() {

        val file = myFixture.addFileToProject("src/main/java/com/example/Main.txt", "main")

        val rootDirectory = myFixture.tempDirFixture.getFile("src/main/java")!!

        WriteCommandAction.runWriteCommandAction(project) {

            file.virtualFile.parent.rename(this, "renamed")

        }

        val nodes = SourceNodes.children(config(), SourceRoot(rootDirectory, false, false))

        val node = nodes.filterIsInstance<PackageNode>().single()

        assertEquals("renamed", node.value!!.name)

        assertTrue(node.contains(file.virtualFile))

    }

    fun testTestSourceFileKeepsItsNameInThePresentation() {

        val file = myFixture.addFileToProject("src/test/java/Case.txt", "case")

        val node = SourceNodes.children(
            config(),
            SourceRoot(file.virtualFile.parent, true, false)
        ).filterIsInstance<SourceFileNode>().single()

        val presentation = com.intellij.ide.projectView.PresentationData()

        node.update(presentation)

        assertEquals("Case.txt (test)", presentation.coloredText.joinToString("") { it.text })

    }

    private fun config(): ViewConfig {

        return ViewConfig(project, ViewSettings.DEFAULT)

    }

    private fun rootDirectory(): com.intellij.psi.PsiDirectory {

        val directory: VirtualFile = myFixture.tempDirFixture.getFile("")!!

        return PsiManager.getInstance(project).findDirectory(directory)!!

    }

}
