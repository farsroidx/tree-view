package ir.farsroidx

import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project

/**
 * Carries the project and native view settings used to create tree nodes.
 *
 * @param project the owning IntelliJ project
 * @param viewSettings native project-view display settings
 */
data class ViewConfig(val project: Project, val viewSettings: ViewSettings)