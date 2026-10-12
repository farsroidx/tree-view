package ir.farsroidx.maven

import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import icons.MavenIcons
import ir.farsroidx.ViewConfig
import ir.farsroidx.maven.nodes.MavenPomNode
import ir.farsroidx.view.SourceRootCollector
import ir.farsroidx.view.model.ModuleLayout
import ir.farsroidx.view.model.SourceRoot
import ir.farsroidx.view.nodes.BuildFilesNode
import ir.farsroidx.view.nodes.BuildProjectNode
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager

/**
 * Adapts non-ignored Maven projects, imported roots, and Maven metadata to the shared tree model.
 *
 * @param config the project and native view settings
 */
class MavenProjectViewNode(config: ViewConfig) : BuildProjectNode(config) {

    /**
     * Returns distinct build-module layouts with imported roots, fallback roots, and hidden outputs.
     *
     * @return ordered, physically distinct module layouts
     */
    override fun collectLayouts(): List<ModuleLayout> {

        val manager = MavenProjectsManager.getInstance(project)

        val root = manager.rootProjects.firstOrNull()

        return manager.nonIgnoredProjects.filter { mavenProject ->

            mavenProject.file.isValid && mavenProject.directoryFile.isValid

        }.sortingByOrder(root).map { mavenProject ->

            val importedRoots = manager.findModule(mavenProject)?.let { module ->

                SourceRootCollector.collect(listOf(module))

            }.orEmpty()

            val roots = importedRoots + modelRoots(mavenProject) +
                SourceRootCollector.conventional(mavenProject.directoryFile)

            ModuleLayout(
                if (mavenProject.isNew) mavenProject.directoryFile.name else {

                    mavenProject.mavenId.artifactId ?: mavenProject.directoryFile.name

                },
                mavenProject.directoryFile,
                roots.distinctBy { it.directory },
                setOf(mavenProject.file),
                outputDirectories(mavenProject),
                showAdditionalFiles = mavenProject !in manager.rootProjects,
                isVisible = roots.isNotEmpty() || manager.getModules(mavenProject).isEmpty()
            )

        }

    }

    /**
     * Resolves Maven output directories, using the conventional target directory before metadata is ready.
     *
     * @param mavenProject the Maven project supplying metadata
     *
     * @return existing Maven output directories to exclude
     */
    private fun outputDirectories(mavenProject: MavenProject): Set<VirtualFile> {

        if (mavenProject.lastReadStamp == UNREAD_PROJECT_STAMP || mavenProject.hasReadingErrors()) {

            return setOfNotNull(mavenProject.directoryFile.findChild(DEFAULT_OUTPUT_DIRECTORY))

        }

        return listOf(
            mavenProject.buildDirectory,
            mavenProject.outputDirectory,
            mavenProject.testOutputDirectory
        ).mapNotNull { findDirectory(it) }.toSet()

    }

    /**
     * Creates the synthetic build-file group with concrete, selectable PSI-file children.
     *
     * @param layouts the ordered build-module layouts
     *
     * @return the synthetic group containing selectable build-file nodes
     */
    override fun buildFilesNode(layouts: List<ModuleLayout>): ProjectViewNode<*> {

        val files = layouts.flatMap { it.buildFiles }

        return BuildFilesNode(config, "Maven Poms", MavenIcons.MavenProject, files) {

            val manager = MavenProjectsManager.getInstance(project)

            val roots = manager.rootProjects.map { it.file }.toSet()

            val psiManager = PsiManager.getInstance(project)

            files.mapNotNull { file ->

                val psiFile = psiManager.findFile(file) ?: return@mapNotNull null

                val label = layouts.firstOrNull { file in it.buildFiles }?.name.orEmpty()

                MavenPomNode(config, psiFile, label, file in roots)

            }

        }

    }

    /**
     * Converts Maven source and resource metadata into semantic root descriptions.
     *
     * @param mavenProject the Maven project supplying metadata
     *
     * @return existing Maven source and resource roots with category metadata
     */
    private fun modelRoots(mavenProject: MavenProject): List<SourceRoot> {

        val sources = mavenProject.sources.mapNotNull { source ->

            findDirectory(source)?.let { SourceRoot(it, false, false) }

        }

        val tests = mavenProject.testSources.mapNotNull { source ->

            findDirectory(source)?.let { SourceRoot(it, true, false) }

        }

        val resources = mavenProject.resources.mapNotNull { resource ->

            findDirectory(resource.directory)?.let { SourceRoot(it, false, true) }

        }

        val testResources = mavenProject.testResources.mapNotNull { resource ->

            findDirectory(resource.directory)?.let { SourceRoot(it, true, true) }

        }

        return sources + tests + resources + testResources

    }

    /**
     * Resolves an existing local directory, returning null for missing or blank paths.
     *
     * @param path the local filesystem path
     *
     * @return the resolved directory, or null when it cannot be resolved
     */
    private fun findDirectory(path: String?): VirtualFile? {

        if (path.isNullOrBlank()) return null

        return LocalFileSystem.getInstance().findFileByPath(path.replace('\\', '/'))
            ?.takeIf { it.isDirectory }

    }

    companion object {

        private const val DEFAULT_OUTPUT_DIRECTORY = "target"

    }

}
