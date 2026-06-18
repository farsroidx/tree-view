package ir.farsroidx.maven.decorator

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import java.awt.Color

class MavenViewDecorator : ProjectViewNodeDecorator {

    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {

        val virtualFile = node.virtualFile ?: return

        val isInTest = isFolder(virtualFile, "test")

        if (isInTest) {

            if (virtualFile.name.contains("(test)", ignoreCase = true)) {

                data.background = JBColor(
                    Color(175, 235, 155),
                    Color(35, 70, 35)
                )

            } else {

                data.background = JBColor(
                    Color(210, 248, 195),
                    Color(35, 70, 35, 100)
                )
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun isFolder(file: VirtualFile, key: String): Boolean {
        return generateSequence(file) { it.parent }.any {
            it.name.contains(key, ignoreCase = true)
        }
    }
}