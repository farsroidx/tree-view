package ir.farsroidx.maven

import com.intellij.ide.SelectInContext
import com.intellij.ide.StandardTargetWeights
import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

/**
 * Routes native Select In actions to the Maven pane, including during indexing.
 *
 * @param project the owning IntelliJ project
 * @param id the stable pane identifier
 */
class MavenSelectInTarget(project: Project, private val id: String) : ProjectViewSelectInTarget(project), DumbAware {

    /**
     * Returns the pane identifier targeted by native selection actions.
     *
     * @return the selection target pane identifier
     */
    override fun getMinorViewId(): String = id

    /**
     * Delegates selection eligibility to the native project-view target.
     *
     * @param subId the native view identifier being tested
     * @param context the native selection context
     *
     * @return true if native selection supports the context
     */
    override fun isSubIdSelectable(subId: String, context: SelectInContext): Boolean = canSelect(context)

    /**
     * Returns the native display priority for this pane or selection target.
     *
     * @return the platform display priority
     */
    override fun getWeight(): Float = StandardTargetWeights.PROJECT_WEIGHT

    /**
     * Returns the user-visible selection-target name.
     *
     * @return the user-visible pane name
     */
    override fun toString(): String {
        return MavenProjectViewPane.PANE_NAME
    }
}
