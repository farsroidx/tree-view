package ir.farsroidx.maven.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.psi.PsiFile
import com.intellij.ui.SimpleTextAttributes
import icons.MavenIcons
import ir.farsroidx.ViewConfig
import ir.farsroidx.withTooltip

class MavenPomNode(
    config: ViewConfig,
    private val psiFile: PsiFile,
    private val label: String,
    private val isRoot: Boolean
) : PsiFileNode(config.project, value = psiFile, config.viewSettings) {

    override fun update(presentation: PresentationData) {
        super.update(presentation)

        presentation.clearText()

        presentation.addText("pom.xml", SimpleTextAttributes.REGULAR_ATTRIBUTES)

        if (isRoot) {
            presentation.setIcon(MavenIcons.MavenProject.withTooltip("Root Pom"))
        } else if (label.isNotBlank()) {
            presentation.addText(" ($label)", SimpleTextAttributes.GRAY_ATTRIBUTES)
            presentation.setIcon(MavenIcons.MavenModule.withTooltip(label))
        }
    }

    override fun canRepresent(element: Any?): Boolean {
        return element == psiFile || super.canRepresent(element)
    }
}