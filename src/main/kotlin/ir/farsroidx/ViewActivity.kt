package ir.farsroidx

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import ir.farsroidx.files.UniversalFileWatcher

/**
 * Initializes the project-scoped structural watcher when the IDE opens a project.
 */
class ViewActivity : ProjectActivity {

    /**
     * Initializes the disposable project watcher. No project files are changed.
     *
     * @param project the owning IntelliJ project
     */
    override suspend fun execute(project: Project) {
        project.service<UniversalFileWatcher>()
    }
}