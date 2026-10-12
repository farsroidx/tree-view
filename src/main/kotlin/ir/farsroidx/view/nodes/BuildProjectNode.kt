package ir.farsroidx.view.nodes

import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.RootsProvider
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import ir.farsroidx.ViewConfig
import ir.farsroidx.view.model.ModuleLayout

/**
 * Builds shared module nodes from build-system layouts and tracks represented roots.
 *
 * Build-file roots include configuration files outside the project directory.
 *
 * @param config the project and native view settings
 */
abstract class BuildProjectNode(
    protected val config: ViewConfig
) : ProjectViewProjectNode(config.project, config.viewSettings) {

    @Volatile
    private var representedRoots: Set<VirtualFile> = emptySet()

    /**
     * Returns distinct build-module layouts with imported roots, fallback roots, and hidden outputs.
     *
     * @return ordered, physically distinct module layouts
     */
    protected abstract fun collectLayouts(): List<ModuleLayout>

    /**
     * Creates the synthetic build-file group with concrete, selectable PSI-file children.
     *
     * @param layouts the ordered build-module layouts
     *
     * @return the synthetic group containing selectable build-file nodes
     */
    protected abstract fun buildFilesNode(layouts: List<ModuleLayout>): ProjectViewNode<*>

    /**
     * Returns the current tree children without mutating build configuration.
     *
     * @return the current module or build-file children
     */
    override fun getChildren(): Collection<AbstractTreeNode<*>> {

        val layouts = collectLayouts()

        val psiManager = PsiManager.getInstance(project)

        if (layouts.isEmpty()) {

            representedRoots = emptySet()

            val directory = project.guessProjectDir()?.let { psiManager.findDirectory(it) }
                ?: return emptyList()

            return listOf(
                PsiDirectoryNode(project, directory, settings)
            )

        }

        val moduleDirectories = layouts.map { it.directory }.toSet()

        val buildFiles = buildFilesNode(layouts)

        representedRoots = moduleDirectories + layouts.flatMap { layout ->

            layout.sourceRoots.map { it.directory }

        } + (buildFiles as? RootsProvider)?.roots.orEmpty()

        val modules = layouts.filter { it.isVisible }.mapNotNull { layout ->

            val directory = psiManager.findDirectory(layout.directory) ?: return@mapNotNull null

            BuildModuleNode(config, directory, layout, moduleDirectories)

        }

        return modules + buildFiles

    }

    /**
     * Returns whether this node contains the supplied file in its represented roots.
     *
     * @param file the file being represented or tested
     *
     * @return true if the file belongs to the represented roots
     */
    override fun contains(file: VirtualFile): Boolean {

        val baseDirectory = project.guessProjectDir()

        return baseDirectory?.let { VfsUtilCore.isAncestor(it, file, false) } == true ||
            representedRoots.any { VfsUtilCore.isAncestor(it, file, false) }

    }

    /**
     * Returns physical roots represented by this node for native selection and tree updates.
     *
     * @return valid represented roots used by native tree traversal
     */
    override fun getRoots(): Collection<VirtualFile> {

        return representedRoots + listOfNotNull(project.guessProjectDir())

    }

}
