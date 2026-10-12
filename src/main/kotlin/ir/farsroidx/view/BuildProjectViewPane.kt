package ir.farsroidx.view

import com.intellij.ide.projectView.impl.AbstractProjectViewPaneWithAsyncSupport
import com.intellij.ide.projectView.impl.ProjectViewTree
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.project.Project
import com.intellij.util.Alarm
import ir.farsroidx.view.nodes.PackageNode
import ir.farsroidx.view.nodes.ResourceNode
import ir.farsroidx.view.nodes.SourceFileNode
import org.jdom.Element
import javax.swing.JComponent
import javax.swing.tree.DefaultTreeModel

/**
 * Shares the native asynchronous project-view lifecycle between build-system panes.
 *
 * Preserves native selection, move actions, drag and drop, status updates, and tree state.
 * Custom sorting orders source, test source, resources, and test resources before other content.
 *
 * @param project the owning IntelliJ project
 */
abstract class BuildProjectViewPane(project: Project) : AbstractProjectViewPaneWithAsyncSupport(project) {

    private val refreshAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    private var legacyState: TreeState? = null

    /**
     * Restores native pane settings and captures legacy tree state for migration.
     *
     * @param element the XML element containing persisted pane settings
     */
    override fun readExternal(element: Element) {

        super.readExternal(element)

        legacyState = element.getChild(LEGACY_STATE_ELEMENT)?.let { TreeState.createFrom(it) }

    }

    /**
     * Creates or reuses the native component and applies pending legacy state after tree loading.
     *
     * @return the native pane component
     */
    override fun createComponent(): JComponent {

        val component = super.createComponent()

        val state = legacyState

        if (state != null) {

            legacyState = null

            updateFromRoot(true).doWhenDone {

                if (!myProject.isDisposed) {

                    tree?.let { state.applyTo(it) }

                }

            }

        }

        return component

    }

    /**
     * Creates the native project-view tree for the supplied model.
     *
     * @param treeModel the native tree model
     *
     * @return the project-view tree bound to treeModel
     */
    override fun createTree(treeModel: DefaultTreeModel): ProjectViewTree {

        return ProjectViewTree(treeModel)

    }

    /**
     * Preserves adapter module order and enforces source, test source, resources, test resources ordering.
     *
     * Native comparison is retained within each section.
     *
     * @return the section-aware comparator with native ordering inside sections
     */
    override fun createComparator(): Comparator<NodeDescriptor<*>> {

        val defaultComparator = super.createComparator()

        return Comparator { first, second ->

            val firstNode = first as? AbstractTreeNode<*>
            val secondNode = second as? AbstractTreeNode<*>

            if (firstNode?.parent?.value is Project && secondNode?.parent?.value is Project) {

                0

            } else {

                val sectionOrder = section(firstNode).compareTo(section(secondNode))

                if (sectionOrder != 0) sectionOrder else defaultComparator.compare(first, second)

            }

        }

    }

    /**
     * Classifies a semantic root node for stable section ordering.
     *
     * @param node the tree node to classify or decorate
     *
     * @return the semantic display section, or OTHER for additional content
     */
    private fun section(node: AbstractTreeNode<*>?): Section {

        return when (node) {

            is PackageNode -> if (node.isTest) Section.SOURCE_TEST else Section.SOURCE

            is SourceFileNode -> if (node.isTest) Section.SOURCE_TEST else Section.SOURCE

            is ResourceNode -> if (node.isTest) Section.RESOURCES_TEST else Section.RESOURCES

            else -> Section.OTHER

        }

    }

    /**
     * Defines the required semantic section order before native sorting within each section.
     */
    private enum class Section {

        SOURCE,
        SOURCE_TEST,
        RESOURCES,
        RESOURCES_TEST,
        OTHER

    }

    /**
     * Coalesces pending structural refreshes and preserves native tree state.
     *
     * Disposed projects are ignored; refresh work is scheduled rather than performed immediately.
     */
    fun scheduleRefresh() {

        if (myProject.isDisposed) return

        refreshAlarm.cancelAllRequests()

        refreshAlarm.addRequest(
            {

                if (!myProject.isDisposed && tree != null) {

                    updateFromRoot(true)

                }

            },
            REFRESH_DELAY
        )

    }

    companion object {

        private const val REFRESH_DELAY = 150

        private const val LEGACY_STATE_ELEMENT = "TreeState"

    }

}
