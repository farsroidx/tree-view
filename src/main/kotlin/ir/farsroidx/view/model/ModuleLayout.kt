package ir.farsroidx.view.model

import com.intellij.openapi.vfs.VirtualFile

/**
 * Describes one physical build module independently of its build-system adapter.
 *
 * @param name the module display name
 * @param directory the physical directory represented by this node
 * @param sourceRoots semantic source and resource roots
 * @param buildFiles configuration files represented in the build-file group
 * @param outputDirectories build outputs excluded from additional content
 * @param showAdditionalFiles whether unrepresented module content is displayed
 * @param isVisible whether the module itself is displayed
 */
data class ModuleLayout(
    val name: String,
    val directory: VirtualFile,
    val sourceRoots: List<SourceRoot>,
    val buildFiles: Set<VirtualFile>,
    val outputDirectories: Set<VirtualFile>,
    val showAdditionalFiles: Boolean = true,
    val isVisible: Boolean = true
)
