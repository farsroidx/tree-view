package ir.farsroidx.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.RootsProvider
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VirtualFile
import ir.farsroidx.ViewConfig
import javax.swing.Icon

/**
 * Groups build files without assigning a physical directory to the synthetic group.
 *
 * Concrete file children retain native PSI actions and selection behavior.
 *
 * @param config the project and native view settings
 * @param label the display label
 * @param icon the group icon
 * @param files the physical files represented by this group
 * @param childrenProvider the provider of concrete file nodes
 */
class BuildFilesNode(
    config: ViewConfig,
    private val label: String,
    private val icon: Icon,
    private val files: List<VirtualFile>,
    private val childrenProvider: () -> Collection<AbstractTreeNode<*>>
) : ProjectViewNode<String>(config.project, label, config.viewSettings), RootsProvider {

    /**
     * Returns physical roots represented by this node for native selection and tree updates.
     *
     * @return valid represented roots used by native tree traversal
     */
    override fun getRoots(): Collection<VirtualFile> {

        return files.filter { it.isValid }

    }

    /**
     * Returns whether this node contains the supplied file in its represented roots.
     *
     * @param file the file being represented or tested
     *
     * @return true if the file belongs to the represented roots
     */
    override fun contains(file: VirtualFile): Boolean {

        return file in files

    }

    /**
     * Returns whether this node represents the supplied PSI element or virtual file.
     *
     * @param element the platform element being represented
     *
     * @return true if the element matches this node
     */
    override fun canRepresent(element: Any?): Boolean {

        return element == value

    }

    /**
     * Returns the current tree children without mutating build configuration.
     *
     * @return the current module or build-file children
     */
    override fun getChildren(): Collection<AbstractTreeNode<*>> {

        return childrenProvider()

    }

    /**
     * Updates native presentation while applying the node-specific label and category.
     *
     * @param presentation the native presentation updated in place
     */
    override fun update(presentation: PresentationData) {

        presentation.presentableText = label

        presentation.setIcon(icon)

    }

}
