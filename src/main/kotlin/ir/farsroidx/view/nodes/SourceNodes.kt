package ir.farsroidx.view.nodes

import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import ir.farsroidx.ViewConfig
import ir.farsroidx.view.model.SourceRoot

/**
 * Creates source-package, direct-file, and resource nodes from semantic source roots.
 */
object SourceNodes {

    /**
     * Creates visible children from the supplied directory or semantic source root.
     *
     * @param config the project and native view settings
     * @param root the semantic source root
     *
     * @return the visible child nodes for the supplied root or directory
     */
    fun children(config: ViewConfig, root: SourceRoot): List<AbstractTreeNode<*>> {

        val directory = PsiManager.getInstance(config.project).findDirectory(root.directory)
            ?: return emptyList()

        val packages = directory.subdirectories.map { child ->

            packageNode(config, child, root)

        }

        val files = directory.files.map { file ->

            SourceFileNode(config, file, root.isTest)

        }

        return packages + files

    }

    /**
     * Creates a resource node, returning null when the root has no valid PSI directory.
     *
     * @param config the project and native view settings
     * @param root the semantic source root
     *
     * @return the resource node, or null if the PSI directory is unavailable
     */
    fun resourceNode(config: ViewConfig, root: SourceRoot): PsiDirectoryNode? {

        val directory = PsiManager.getInstance(config.project).findDirectory(root.directory)
            ?: return null

        return ResourceNode(config, directory, root.isTest)

    }

    /**
     * Compacts an unbranched directory chain and retains the first directory for selection matching.
     *
     * @param config the project and native view settings
     * @param directory the physical directory represented by this node
     * @param root the semantic source root
     *
     * @return a compact node covering the unbranched package chain
     */
    private fun packageNode(
        config: ViewConfig,
        directory: PsiDirectory,
        root: SourceRoot
    ): PackageNode {

        var current = directory

        val parts = mutableListOf<String>()

        if (root.packagePrefix.isNotEmpty()) {

            parts.add(root.packagePrefix)

        }

        while (true) {

            parts.add(current.name)

            val subdirectories = current.subdirectories

            if (subdirectories.size != 1 || current.files.isNotEmpty()) break

            current = subdirectories.single()

        }

        return PackageNode(config, current, parts.joinToString("."), root.isTest, directory)

    }

}

internal const val TEST_SUFFIX = " (test)"
