package ir.farsroidx

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import ir.farsroidx.files.UniversalFileWatcher

class ViewActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        project.service<UniversalFileWatcher>()
    }
}