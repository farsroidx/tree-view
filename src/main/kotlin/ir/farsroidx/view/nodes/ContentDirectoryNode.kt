package ir.farsroidx.view.nodes

import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import ir.farsroidx.ViewConfig

/**
 * Shows additional module content while pruning represented roots and internal directories.
 *
 * @param config the project and native view settings
 * @param directory the physical directory represented by this node
 * @param hiddenFiles paths already represented elsewhere or excluded
 */
class ContentDirectoryNode(
    private val config: ViewConfig,
    directory: PsiDirectory,
    private val hiddenFiles: Set<VirtualFile>
) : PsiDirectoryNode(config.project, directory, config.viewSettings) {

    /**
     * Returns current physical or synthetic children while excluding separately represented content.
     *
     * @return visible children excluding separately represented and hidden paths
     */
    override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> {

        val directory = value ?: return emptyList()

        return children(config, directory, hiddenFiles)

    }

    companion object {

        /**
         * Creates visible children from the supplied directory or semantic source root.
         *
         * @param config the project and native view settings
         * @param directory the physical directory represented by this node
         * @param hiddenFiles paths already represented elsewhere or excluded
         *
         * @return the visible child nodes for the supplied root or directory
         */
        fun children(
            config: ViewConfig,
            directory: PsiDirectory,
            hiddenFiles: Set<VirtualFile>
        ): List<AbstractTreeNode<*>> {

            val directories = directory.subdirectories.mapNotNull { child ->

                val file = child.virtualFile

                if (file in hiddenFiles || file.name in INTERNAL_DIRECTORIES) {

                    return@mapNotNull null

                }

                if (hiddenFiles.any { VfsUtilCore.isAncestor(file, it, true) }) {

                    val node = ContentDirectoryNode(config, child, hiddenFiles)

                    node.takeIf { it.getChildrenImpl().isNotEmpty() }

                } else {

                    PsiDirectoryNode(config.project, child, config.viewSettings)

                }

            }

            val files = directory.files.filterNot { it.virtualFile in hiddenFiles }.map { file ->

                PsiFileNode(config.project, file, config.viewSettings)

            }

            return directories + files

        }

        private val INTERNAL_DIRECTORIES = setOf(".git", ".idea", ".gradle")

    }

}
