package ir.farsroidx.maven.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.ui.SimpleTextAttributes
import ir.farsroidx.ViewConfig
import javax.swing.Icon

class MavenPackageNode(
    config: ViewConfig,
    private val icon: Icon,
    private val suffix: String,
    private val directory: PsiDirectory,
    private val packageLabel: String? = null
) : PsiDirectoryNode(config.project, directory, config.viewSettings) {

    override fun update(presentation: PresentationData) {
        super.update(presentation)

        val finalName = packageLabel ?: directory.name

        presentation.clearText()
        presentation.addText(finalName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        presentation.setIcon(icon)

        if (suffix.isNotEmpty()) {
            presentation.addText(" $suffix", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }

    override fun canRepresent(element: Any?): Boolean {
        return when (element) {
            is PsiDirectory -> element == directory
            is VirtualFile -> element == directory.virtualFile
            else -> super.canRepresent(element)
        }
    }
}
