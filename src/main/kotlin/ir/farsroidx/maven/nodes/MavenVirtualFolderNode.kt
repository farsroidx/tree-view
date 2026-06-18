package ir.farsroidx.maven.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import ir.farsroidx.ViewConfig
import javax.swing.Icon

class MavenVirtualFolderNode(
    config: ViewConfig,
    private val name: String,
    private val icon: Icon,
    private val childrenProvider: () -> Collection<AbstractTreeNode<*>>
) : ProjectViewNode<String>(config.project, name, config.viewSettings) {

    override fun contains(file: VirtualFile): Boolean {
        return VfsUtilCore.isAncestor(project.baseDir, file, true)
    }

    override fun canRepresent(element: Any?): Boolean {

        if (super.canRepresent(element)) return true

        return element is VirtualFile && element == project.baseDir
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        return childrenProvider()
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = name
        presentation.setIcon(icon)
    }
}