package ir.farsroidx

import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project

data class ViewConfig(val project: Project, val viewSettings: ViewSettings)