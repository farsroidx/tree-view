package ir.farsroidx.view.nodes

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.psi.PsiDirectory
import com.intellij.ui.SimpleTextAttributes
import ir.farsroidx.ViewConfig

/**
 * Displays a resource root with its production or test category.
 *
 * @param config the project and native view settings
 * @param directory the physical directory represented by this node
 * @param isTest whether the root belongs to test content
 */
class ResourceNode(
    config: ViewConfig,
    directory: PsiDirectory,
    val isTest: Boolean
) : PsiDirectoryNode(config.project, directory, config.viewSettings) {

    /**
     * Updates native presentation while applying the node-specific label and category.
     *
     * @param presentation the native presentation updated in place
     */
    override fun update(presentation: PresentationData) {

        super.update(presentation)

        presentation.clearText()

        presentation.addText(value?.name.orEmpty(), SimpleTextAttributes.REGULAR_ATTRIBUTES)

        presentation.setIcon(
            if (isTest) AllIcons.Modules.TestResourcesRoot else AllIcons.Modules.ResourcesRoot
        )

        if (isTest) {

            presentation.addText(TEST_SUFFIX, SimpleTextAttributes.GRAYED_ATTRIBUTES)

        }

    }

}
