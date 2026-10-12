package ir.farsroidx.gradle

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import ir.farsroidx.ViewConfig

/**
 * Creates the Gradle root through the native background project-tree structure.
 *
 * @param project the owning IntelliJ project
 * @param id the stable pane identifier
 */
class GradleTreeStructure(project: Project, id: String) : ProjectTreeStructure(project, id) {

    /**
     * Creates the build-system root node with the supplied project and view settings.
     *
     * @param project the owning IntelliJ project
     * @param viewSettings native project-view display settings
     *
     * @return the build-system root node
     */
    override fun createRoot(project: Project, viewSettings: ViewSettings): AbstractTreeNode<*> {

        return GradleProjectViewNode(ViewConfig(project, viewSettings))

    }

    /**
     * Enables background child construction through the native tree infrastructure.
     *
     * @param element the platform element being represented
     *
     * @return true to enable background tree construction
     */
    override fun isToBuildChildrenInBackground(element: Any): Boolean = true

}
