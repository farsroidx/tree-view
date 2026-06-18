package ir.farsroidx.maven

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.ui.SimpleTextAttributes
import icons.MavenIcons
import ir.farsroidx.ViewConfig
import ir.farsroidx.maven.nodes.MavenPackageNode
import ir.farsroidx.maven.nodes.MavenPomNode
import ir.farsroidx.maven.nodes.MavenVirtualFolderNode
import ir.farsroidx.withTooltip
import org.jetbrains.idea.maven.project.MavenProjectsManager
import javax.swing.Icon

class MavenProjectViewNode(private val config: ViewConfig, psiDirectory: PsiDirectory) : ProjectViewProjectNode(
    config.project, config.viewSettings
) {

    private var rootDirectory: PsiDirectory = psiDirectory

    override fun contains(vFile: VirtualFile): Boolean {
        return VfsUtilCore.isAncestor(rootDirectory.virtualFile, vFile, true)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {

        val mavenManager = MavenProjectsManager.getInstance(project)

        if (!mavenManager.isMavenizedProject) {

            return listOf(
                PsiDirectoryNode(
                    project, rootDirectory, settings
                )
            )
        }

        val rootMavenProject = mavenManager.rootProjects.firstOrNull()
            ?: return listOf(PsiDirectoryNode(project, rootDirectory, settings))

        val allProjects = mavenManager.projects

        val subProjects = allProjects.filter { it != rootMavenProject }

        val resultNodes = mutableListOf<AbstractTreeNode<*>>()

        val subProjectNames = subProjects
            .map { it.directoryFile.name }
            .toSet()

        // ===============================
        // Maven Poms Folder
        // ===============================

        val pomsIcon = MavenIcons.MavenProject.withTooltip("Maven Poms")

        val pomsVirtualFolder = MavenVirtualFolderNode(config, "Maven Poms", pomsIcon) {

            allProjects.sortingByOrder(rootMavenProject).mapNotNull { mavenProj ->

                val psiFile = PsiManager.getInstance(project).findFile(mavenProj.file)

                psiFile?.let { psiFile ->

                    val label = (mavenProj.mavenId.artifactId ?: mavenProj.name ?: "unknown")
                        .replace(" ", "-")
                        .lowercase()

                    MavenPomNode(config, psiFile, label, isRoot = mavenProj == rootMavenProject)
                }
            }
        }

        val rootHasSrc = rootDirectory.virtualFile.findChild("src")?.exists() == true

        if (subProjects.isEmpty()) {

            // =================================
            // single module
            // =================================

            val rootProjectName = (rootMavenProject.mavenId.artifactId ?: project.name)

            val singleModuleNode = virtualFolderNode(
                icon = AllIcons.Modules.SourceRoot,
                nodeName = rootProjectName,
                isRootProject = true,
                baseDirectory = rootDirectory.virtualFile,
                subProjectNames = subProjectNames
            )

            resultNodes.add(singleModuleNode)
            resultNodes.add(pomsVirtualFolder)

        } else {

            // =================================
            // multi module
            // =================================

            if (rootHasSrc) {

                val rootName = (rootMavenProject.mavenId.artifactId ?: project.name)
                    .replace(" ", "-")
                    .lowercase()

                val rootSrcNode = virtualFolderNode(
                    icon = AllIcons.Modules.SourceRoot,
                    nodeName = rootName,
                    isRootProject = true,
                    baseDirectory = rootDirectory.virtualFile,
                    subProjectNames = subProjectNames
                )

                resultNodes.add(rootSrcNode)
            }

            for (subProject in subProjects.sortingByOrder()) {

                val moduleDir = subProject.directoryFile

                val nodeName = (subProject.mavenId.artifactId ?: moduleDir.name)
                    .replace(" ", "-")
                    .lowercase()

                val moduleNode = virtualFolderNode(
                    icon = AllIcons.Modules.SourceRoot,
                    nodeName = nodeName,
                    isRootProject = false,
                    baseDirectory = moduleDir,
                    subProjectNames = subProjectNames
                )

                resultNodes.add(moduleNode)
            }

            resultNodes.add(pomsVirtualFolder)
        }

        return resultNodes
    }

    @Suppress("SameParameterValue")
    private fun virtualFolderNode(
        icon: Icon,
        nodeName: String,
        isRootProject: Boolean,
        baseDirectory: VirtualFile,
        subProjectNames: Set<String>
    ): AbstractTreeNode<*> {

        return MavenVirtualFolderNode(config, nodeName, icon.withTooltip(nodeName)) {

            val childrenNodes = mutableListOf<AbstractTreeNode<*>>()

            val psiManager = PsiManager.getInstance(project)

            val mainCodeDirectories     = mutableListOf<PsiDirectory>()
            val mainResourceDirectories = mutableListOf<PsiDirectory>()

            val testCodeDirectories     = mutableListOf<PsiDirectory>()
            val testResourceDirectories = mutableListOf<PsiDirectory>()

            val srcDir = baseDirectory.findChild("src")

            if (srcDir != null) {

                // دستی و مطمئن مسیرهای src/main و src/test را بررسی می‌کنیم
                fun scanDirectory(parentDir: VirtualFile, isTest: Boolean) {

                    val javaDir   = parentDir.findChild("java")
                    val kotlinDir = parentDir.findChild("kotlin")
                    val resDir    = parentDir.findChild("resources")

                    javaDir?.let { dir -> psiManager.findDirectory(dir) }?.let { dir ->
                        if (isTest) testCodeDirectories.add(dir) else mainCodeDirectories.add(dir)
                    }

                    kotlinDir?.let { dir -> psiManager.findDirectory(dir) }?.let { dir ->
                        if (isTest) testCodeDirectories.add(dir) else mainCodeDirectories.add(dir)
                    }

                    resDir?.let { dir -> psiManager.findDirectory(dir) }?.let { dir ->
                        if (isTest) testResourceDirectories.add(dir) else mainResourceDirectories.add(dir)
                    }
                }

                srcDir.findChild("main")?.let { dir -> scanDirectory(dir, isTest = false) }
                srcDir.findChild("test")?.let { dir -> scanDirectory(dir, isTest = true)  }
            }

            val allCodeNodes = mutableListOf<AbstractTreeNode<*>>()

            mainCodeDirectories.forEach { dir ->

                dir.subdirectories.forEach { subDir ->

                    allCodeNodes.add(
                        compactDirectoryNode(subDir)
                    )
                }
            }

            testCodeDirectories.forEach { dir ->

                dir.subdirectories.forEach { subDir ->

                    allCodeNodes.add(
                        compactDirectoryNode(subDir, suffix = "(test)")
                    )
                }
            }

            allCodeNodes.sortBy { (it as? MavenPackageNode)?.value?.name?.lowercase() ?: it.name?.lowercase() ?: "" }

            childrenNodes.addAll(allCodeNodes)

            val resourceNodes = mutableListOf<AbstractTreeNode<*>>()

            mainResourceDirectories.forEach {  resDir ->
                resourceNodes.add(
                    PsiDirectoryNode(project, resDir, settings).apply {
                        presentation.apply {
                            addText(resDir.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                            setIcon(AllIcons.Modules.ResourcesRoot)
                        }
                    }
                )
            }

            testResourceDirectories.forEach { resDir ->
                resourceNodes.add(
                    PsiDirectoryNode(project, resDir, settings).apply {
                        presentation.apply {
                            addText(resDir.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                            addText(" (test)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                            setIcon(AllIcons.Modules.TestResourcesRoot)
                        }
                    }
                )
            }

            resourceNodes.sortBy { it.name?.lowercase() ?: "" }
            childrenNodes.addAll(resourceNodes)

            if (!isRootProject) {

                val psiBaseDir = psiManager.findDirectory(baseDirectory)

                if (psiBaseDir != null) {

                    val extraFiles = psiBaseDir.notNullChildren(config, subProjectNames)

                    val sortedExtraFiles = extraFiles.sortedWith(compareBy<AbstractTreeNode<*>> {
                        if (it is PsiDirectoryNode) 0 else 1
                    }.thenBy {
                        it.name?.lowercase() ?: ""
                    })

                    childrenNodes.addAll(sortedExtraFiles)
                }
            }

            childrenNodes
        }
    }

    private fun compactDirectoryNode(dir: PsiDirectory, suffix: String = ""): AbstractTreeNode<*> {

        val (directory, packageName) = compactMiddlePackages(dir)

        return MavenPackageNode(
            config = config,
            icon = AllIcons.Nodes.ModuleGroup,
            suffix = suffix,
            directory = directory,
            packageLabel = packageName
        )
    }

    private fun compactMiddlePackages(directory: PsiDirectory): Pair<PsiDirectory, String> {

        var current = directory

        val parts = mutableListOf<String>()

        while (true) {

            val subDirs = current.subdirectories

            val files = current.files

            if (subDirs.size == 1 && files.isEmpty()) {

                parts.add(current.name)

                current = subDirs[0]

            } else break
        }

        val name = if (parts.isEmpty()) current.name else {

            parts.joinToString(".") + "." + current.name

        }

        return current to name
    }

    override fun update(presentation: PresentationData) {
        super.update(presentation)
    }
}