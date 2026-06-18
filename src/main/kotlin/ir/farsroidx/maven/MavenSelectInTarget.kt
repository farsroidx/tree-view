package ir.farsroidx.maven

import com.intellij.ide.SelectInContext
import com.intellij.ide.SelectInManager
import com.intellij.ide.StandardTargetWeights
import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

class MavenSelectInTarget(project: Project, private val id: String) : ProjectViewSelectInTarget(project), DumbAware {

    override fun getMinorViewId(): String = id

    override fun isSubIdSelectable(subId: String, context: SelectInContext): Boolean = canSelect(context)

    override fun getWeight(): Float = StandardTargetWeights.PROJECT_WEIGHT

    override fun toString(): String {
        return SelectInManager.getProject()
    }
}