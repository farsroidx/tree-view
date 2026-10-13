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

/**
 * Compacts a source package chain while retaining native PSI-directory behavior.
 *
 * Validity depends on the represented directories, not synthetic parent ancestry.
 *
 * @param config the project and native view settings
 * @param directory the physical directory represented by this node
 * @param packageLabel the compacted package display name
 * @param isTest whether the root belongs to test content
 * @param firstDirectory the first physical directory in the compact chain
 */
class PackageNode(
    private val config: ViewConfig,
    private val directory: PsiDirectory,
    private val packageLabel: String,
    val isTest: Boolean,
    private val firstDirectory: PsiDirectory,
    val isGenerated: Boolean = false,
    private val hiddenDirectories: Set<VirtualFile> = emptySet()
) : PsiDirectoryNode(config.project, directory, config.viewSettings) {

    /**
     * Returns whether both ends of the compact package chain remain valid.
     *
     * Synthetic module ancestry must not invalidate a source package.
     *
     * @return true if both compact-chain directories remain valid
     */
    override fun isValid(): Boolean {

        return directory.isValid && firstDirectory.isValid

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

        presentation.addText(packageLabel, SimpleTextAttributes.REGULAR_ATTRIBUTES)

        presentation.setIcon(AllIcons.Nodes.ModuleGroup)

        if (isTest) {

            presentation.addText(TEST_SUFFIX, SimpleTextAttributes.GRAYED_ATTRIBUTES)

        }

        if (isGenerated) {

            presentation.addText(GENERATED_SUFFIX, SimpleTextAttributes.GRAYED_ATTRIBUTES)

        }

    }

    override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> {

        if (hiddenDirectories.any { VfsUtilCore.isAncestor(directory.virtualFile, it, true) }) {

            return ContentDirectoryNode.children(config, directory, hiddenDirectories)

        }

        return super.getChildrenImpl().orEmpty()

    }

    /**
     * Returns whether this node contains the supplied file in its represented roots.
     *
     * @param file the file being represented or tested
     *
     * @return true if the file belongs to the represented roots
     */
    override fun contains(file: VirtualFile): Boolean {

        return VfsUtilCore.isAncestor(firstDirectory.virtualFile, file, false)

    }

    /**
     * Returns whether this node represents the supplied PSI element or virtual file.
     *
     * @param element the platform element being represented
     *
     * @return true if the element matches this node
     */
    override fun canRepresent(element: Any?): Boolean {

        return when (element) {

            is PsiDirectory -> representsDirectory(element.virtualFile)

            is VirtualFile -> representsDirectory(element)

            else -> super.canRepresent(element)

        }

    }

    /**
     * Returns whether the directory lies within the compacted package chain.
     *
     * @param file the file being represented or tested
     *
     * @return true if the path belongs to the compacted directory chain
     */
    private fun representsDirectory(file: VirtualFile): Boolean {

        return VfsUtilCore.isAncestor(firstDirectory.virtualFile, file, false) &&
            VfsUtilCore.isAncestor(file, directory.virtualFile, false)

    }

}
