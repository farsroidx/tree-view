package ir.farsroidx.gradle.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.psi.PsiFile
import com.intellij.ui.SimpleTextAttributes
import ir.farsroidx.ViewConfig

/**
 * Presents a Gradle configuration file with its owning module or Gradle User Home label.
 *
 * @param config the project and native view settings
 * @param file the file being represented or tested
 * @param moduleName the owning module or global configuration label
 */
class GradleBuildFileNode(
    config: ViewConfig,
    private val file: PsiFile,
    private val moduleName: String
) : PsiFileNode(config.project, file, config.viewSettings) {

    /**
     * Updates native presentation while applying the node-specific label and category.
     *
     * @param presentation the native presentation updated in place
     */
    override fun update(presentation: PresentationData) {

        super.update(presentation)

        presentation.clearText()

        presentation.addText(file.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)

        presentation.addText(" ($moduleName)", SimpleTextAttributes.GRAYED_ATTRIBUTES)

    }

}
