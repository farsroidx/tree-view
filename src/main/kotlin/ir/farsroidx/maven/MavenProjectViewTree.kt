package ir.farsroidx.maven

import com.intellij.ide.projectView.impl.ProjectViewTree
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.ui.tree.AsyncTreeModel
import ir.farsroidx.maven.nodes.MavenVirtualFolderNode
import javax.swing.tree.DefaultMutableTreeNode

class MavenProjectViewTree(
    private val project: Project,
    asyncTreeModel: AsyncTreeModel
) : ProjectViewTree(asyncTreeModel), UiDataProvider {

    init {
        isRootVisible = false
    }

    override fun uiDataSnapshot(sink: DataSink) {

        val path = selectionPath ?: return

        val node = when (val lastComponent = path.lastPathComponent) {
            is DefaultMutableTreeNode -> lastComponent.userObject
            else -> lastComponent
        }

        when (node) {

            is PsiFileNode -> {
                sink[CommonDataKeys.VIRTUAL_FILE] = node.value?.virtualFile
                sink.lazy(CommonDataKeys.PSI_FILE) { node.value }
                sink.lazy(CommonDataKeys.PSI_ELEMENT) { node.value }
            }

            is PsiDirectoryNode -> {
                sink[CommonDataKeys.VIRTUAL_FILE] = node.value?.virtualFile
                sink.lazy(CommonDataKeys.PSI_ELEMENT) { node.value }
            }

            is MavenVirtualFolderNode -> {
                sink[CommonDataKeys.VIRTUAL_FILE] = project.baseDir
            }

            else -> {

                (node as? AbstractTreeNode<*>)?.value?.let { value ->

                    when (value) {

                        is VirtualFile -> sink[CommonDataKeys.VIRTUAL_FILE] = value

                        is PsiFileSystemItem -> {
                            sink[CommonDataKeys.VIRTUAL_FILE] = value.virtualFile
                            sink.lazy(CommonDataKeys.PSI_ELEMENT) { value }
                        }
                    }
                }
            }
        }

        sink[CommonDataKeys.PROJECT] = project
    }
}