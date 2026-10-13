package ir.farsroidx.view.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import ir.farsroidx.view.BuildProjectViewPane
import ir.farsroidx.view.GeneratedSourcesSettings

class ShowGeneratedSourcesAction : ToggleAction("Generated Sources"), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {

        super.update(event)

        val project = event.project

        event.presentation.isEnabledAndVisible = project != null &&
            ProjectView.getInstance(project).currentProjectViewPane is BuildProjectViewPane

    }

    override fun isSelected(event: AnActionEvent): Boolean {

        return event.project?.service<GeneratedSourcesSettings>()?.showGeneratedSources == true

    }

    override fun setSelected(event: AnActionEvent, state: Boolean) {

        val project = event.project ?: return

        project.service<GeneratedSourcesSettings>().showGeneratedSources = state

        val projectView = ProjectView.getInstance(project)

        projectView.paneIds.mapNotNull { projectView.getProjectViewPaneById(it) }
            .filterIsInstance<BuildProjectViewPane>()
            .forEach { it.scheduleRefresh() }

    }

}
