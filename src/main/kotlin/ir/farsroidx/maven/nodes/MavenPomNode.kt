package ir.farsroidx.maven.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.psi.PsiFile
import com.intellij.ui.SimpleTextAttributes
import icons.MavenIcons
import ir.farsroidx.ViewConfig
import ir.farsroidx.withTooltip

/**
 * Presents a Maven POM with root or module-specific labels and icons.
 *
 * @param config the project and native view settings
 * @param psiFile the POM PSI file
 * @param label the display label
 * @param isRoot whether this is a root build module
 */
class MavenPomNode(
    config: ViewConfig,
    private val psiFile: PsiFile,
    private val label: String,
    private val isRoot: Boolean
) : PsiFileNode(config.project, value = psiFile, config.viewSettings) {

    /**
     * Updates native presentation while applying the node-specific label and category.
     *
     * @param presentation the native presentation updated in place
     */
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

    /**
     * Returns whether this node represents the supplied PSI element or virtual file.
     *
     * @param element the platform element being represented
     *
     * @return true if the element matches this node
     */
    override fun canRepresent(element: Any?): Boolean {
        return element == psiFile || super.canRepresent(element)
    }
}