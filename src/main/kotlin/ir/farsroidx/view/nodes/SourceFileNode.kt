package ir.farsroidx.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.psi.PsiFile
import com.intellij.ui.SimpleTextAttributes
import ir.farsroidx.ViewConfig

/**
 * Displays a file directly under a source root and preserves its test classification.
 *
 * @param config the project and native view settings
 * @param file the file being represented or tested
 * @param isTest whether the root belongs to test content
 */
class SourceFileNode(
    config: ViewConfig,
    file: PsiFile,
    val isTest: Boolean,
    val isGenerated: Boolean = false
) : PsiFileNode(config.project, file, config.viewSettings) {

    /**
     * Updates native presentation while applying the node-specific label and category.
     *
     * @param presentation the native presentation updated in place
     */
    override fun update(presentation: PresentationData) {

        super.update(presentation)

        if (isTest || isGenerated) {

            presentation.clearText()

            presentation.addText(value?.name.orEmpty(), SimpleTextAttributes.REGULAR_ATTRIBUTES)

            if (isTest) {

                presentation.addText(TEST_SUFFIX, SimpleTextAttributes.GRAYED_ATTRIBUTES)

            }

            if (isGenerated) {

                presentation.addText(GENERATED_SUFFIX, SimpleTextAttributes.GRAYED_ATTRIBUTES)

            }

        }

    }

}
