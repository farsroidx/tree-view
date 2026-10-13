package ir.farsroidx.view.model

import com.intellij.openapi.vfs.VirtualFile

/**
 * Describes the semantic category and package prefix of a source or resource directory.
 *
 * @param directory the physical directory represented by this node
 * @param isTest whether the root belongs to test content
 * @param isResource whether the root contains resources
 * @param packagePrefix imported package prefix for the root
 */
data class SourceRoot(
    val directory: VirtualFile,
    val isTest: Boolean,
    val isResource: Boolean,
    val packagePrefix: String = "",
    val isGenerated: Boolean = false
)
