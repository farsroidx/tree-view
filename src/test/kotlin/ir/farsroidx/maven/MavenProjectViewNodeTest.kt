package ir.farsroidx.maven

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import ir.farsroidx.ViewConfig
import ir.farsroidx.maven.nodes.MavenPomNode
import ir.farsroidx.view.nodes.BuildFilesNode
import ir.farsroidx.view.nodes.BuildModuleNode
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager

class MavenProjectViewNodeTest : BasePlatformTestCase() {

    override fun createTempDirTestFixture(): TempDirTestFixture {

        return TempDirTestFixtureImpl()

    }

    override fun setUp() {

        super.setUp()

        MavenProjectsManager.getInstance(project).initForTests()

    }

    override fun tearDown() {

        try {

            MavenProjectsManager.getInstance(project).projectsTreeForTests.updater()
                .setRootProjects(emptyList())
                .setAggregatorMappings(emptyMap())
                .setMavenIdMappings(emptyList())

        } finally {

            super.tearDown()

        }

    }

    fun testSingleModuleKeepsOriginalSourceLayout() {

        val mavenProject = mavenProject("pom.xml", "root")

        val source = myFixture.addFileToProject("src/main/java/Main.txt", "main")

        val readme = myFixture.addFileToProject("README.md", "readme")

        installProjects(listOf(mavenProject))

        val root = rootNode()

        val node = root.children.filterIsInstance<BuildModuleNode>().single()

        val files = node.getChildrenImpl().filterIsInstance<PsiFileNode>()

        assertTrue(files.any { it.value == source })

        assertFalse(files.any { it.value == readme })

        val group = root.children.filterIsInstance<BuildFilesNode>().single()

        assertEquals(mavenProject.file, group.children.filterIsInstance<MavenPomNode>().single().virtualFile)

    }

    fun testMultipleRootProjectsRemainIndependent() {

        val first = mavenProject("first/pom.xml", "first")

        val second = mavenProject("second/pom.xml", "second")

        installProjects(listOf(first, second))

        val root = rootNode()

        val nodes = root.children.filterIsInstance<BuildModuleNode>()

        assertEquals(setOf(first.directoryFile, second.directoryFile), nodes.map { it.virtualFile }.toSet())

        val group = root.children.filterIsInstance<BuildFilesNode>().single()

        assertEquals(setOf(first.file, second.file), group.children.map { (it as MavenPomNode).virtualFile }.toSet())

    }

    fun testNestedModulesAreNotRepeatedInsideParentContent() {

        myFixture.addFileToProject("src/main/java/Root.txt", "root")

        val root = mavenProject("pom.xml", "root")

        val child = mavenProject("modules/child/pom.xml", "child")

        installProjects(listOf(root), mapOf(root to listOf(child)))

        val nodes = rootNode().children.filterIsInstance<BuildModuleNode>()

        assertEquals(2, nodes.size)

        val parent = nodes.single { it.virtualFile == root.directoryFile }

        assertFalse(parent.getChildrenImpl().filterIsInstance<PsiDirectoryNode>().any {

            it.value!!.name == "modules"

        })

    }

    fun testSameDirectoryNamesInDifferentModulesDoNotHideUnrelatedFiles() {

        val root = mavenProject("pom.xml", "root")

        myFixture.addFileToProject("src/main/java/Root.txt", "root")

        val first = mavenProject("first/pom.xml", "first")

        val second = mavenProject("second/common/pom.xml", "second-common")

        myFixture.addFileToProject("first/common/notes.txt", "notes")

        installProjects(listOf(root), mapOf(root to listOf(first, second)))

        val parent = rootNode().children.filterIsInstance<BuildModuleNode>()
            .single { it.virtualFile == first.directoryFile }

        assertTrue(parent.getChildrenImpl().filterIsInstance<PsiDirectoryNode>().any {

            it.value!!.name == "common"

        })

    }

    private fun installProjects(
        roots: List<MavenProject>,
        children: Map<MavenProject, List<MavenProject>> = emptyMap()
    ) {

        MavenProjectsManager.getInstance(project).projectsTreeForTests.updater()
            .setRootProjects(roots)
            .setAggregatorMappings(children)
            .setMavenIdMappings(roots + children.values.flatten())

    }

    private fun mavenProject(path: String, artifactId: String): MavenProject {

        val file = myFixture.addFileToProject(path, "<project/>").virtualFile

        return MavenProject(file).apply {

            updateMavenId(MavenId("test", artifactId, "1.0"))

        }

    }

    private fun rootNode(): MavenProjectViewNode {

        return MavenProjectViewNode(ViewConfig(project, ViewSettings.DEFAULT))

    }

}
