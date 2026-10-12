package ir.farsroidx.view.decorator

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ui.JBColor
import ir.farsroidx.view.nodes.PackageNode
import ir.farsroidx.view.nodes.ResourceNode
import ir.farsroidx.view.nodes.SourceFileNode
import java.awt.Color

/**
 * Applies test backgrounds using semantic node ancestry rather than physical folder names.
 */
class BuildViewDecorator : ProjectViewNodeDecorator {

    /**
     * Applies the test background when this node or a semantic ancestor is classified as test content.
     *
     * @param node the tree node to classify or decorate
     * @param data the native presentation decorated in place
     */
    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {

        val isTest = generateSequence<AbstractTreeNode<*>>(node) { it.parent }.any { ancestor ->

            (ancestor is PackageNode && ancestor.isTest) ||
                (ancestor is ResourceNode && ancestor.isTest) ||
                (ancestor is SourceFileNode && ancestor.isTest)

        }

        if (isTest) {

            data.background = TEST_BACKGROUND

        }

    }

    companion object {

        private val TEST_BACKGROUND = JBColor(
            Color(210, 248, 195),
            Color(35, 70, 35, 40)
        )

    }

}
