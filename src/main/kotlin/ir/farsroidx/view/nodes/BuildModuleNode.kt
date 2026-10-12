package ir.farsroidx.view.nodes

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.ui.SimpleTextAttributes
import ir.farsroidx.ViewConfig
import ir.farsroidx.view.model.ModuleLayout

/**
 * Presents module sources, resources, and optional additional content without duplicate build files.
 *
 * @param config the project and native view settings
 * @param directory the physical directory represented by this node
 * @param layout the build-system-independent module layout
 * @param moduleDirectories physical directories of separately represented modules
 */
class BuildModuleNode(
    private val config: ViewConfig,
    directory: PsiDirectory,
    private val layout: ModuleLayout,
    private val moduleDirectories: Set<VirtualFile>
) : PsiDirectoryNode(config.project, directory, config.viewSettings) {

    /**
     * Returns current physical or synthetic children while excluding separately represented content.
     *
     * @return visible children excluding separately represented and hidden paths
     */
    override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> {

        val children = mutableListOf<AbstractTreeNode<*>>()

        val roots = layout.sourceRoots.filter { it.directory.isValid }.sortedBy { it.isTest }

        roots.filterNot { it.isResource }.forEach { root ->

            children.addAll(SourceNodes.children(config, root))

        }

        roots.filter { it.isResource }.forEach { root ->

            SourceNodes.resourceNode(config, root)?.let { children.add(it) }

        }

        val hiddenFiles = roots.map { it.directory }.toSet() +
            layout.buildFiles + layout.outputDirectories +
            moduleDirectories.filter { it != layout.directory }

        if (layout.showAdditionalFiles) {

            value?.let { directory ->

                children.addAll(ContentDirectoryNode.children(config, directory, hiddenFiles))

            }

        }

        return children

    }

    /**
     * Returns whether this node contains the supplied file in its represented roots.
     *
     * @param file the file being represented or tested
     *
     * @return true if the file belongs to the represented roots
     */
    override fun contains(file: VirtualFile): Boolean {

        return super.contains(file) || layout.sourceRoots.any { root ->

            VfsUtilCore.isAncestor(root.directory, file, false)

        }

    }

    /**
     * Returns physical roots represented by this node for native selection and tree updates.
     *
     * @return valid represented roots used by native tree traversal
     */
    override fun getRoots(): Collection<VirtualFile> {

        return listOf(layout.directory) + layout.sourceRoots.map { it.directory }

    }

    /**
     * Updates native presentation while applying the node-specific label and category.
     *
     * @param presentation the native presentation updated in place
     */
    override fun update(presentation: PresentationData) {

        super.update(presentation)

        presentation.clearText()

        presentation.locationString = null

        presentation.addText(layout.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)

        presentation.setIcon(AllIcons.Modules.SourceRoot)

    }

}
