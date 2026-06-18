package ir.farsroidx.files

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FileStatusListener
import com.intellij.openapi.vcs.FileStatusManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import ir.farsroidx.maven.MavenProjectViewPane
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

@Service(Service.Level.PROJECT)
class UniversalFileWatcher(private val project: Project) : Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var refreshJob: Job? = null

    init {

        FileStatusManager.getInstance(project).addFileStatusListener(
            object : FileStatusListener {
                override fun fileStatusesChanged() { scheduleRefresh() }
                override fun fileStatusChanged(vFile: VirtualFile) { scheduleRefresh() }
            }, this
        )

        VirtualFileManager.getInstance().addAsyncFileListener(object : AsyncFileListener {

            override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {

                val shouldRefresh = events.any { event ->

                    val path = event.path

                    path.endsWith("pom.xml") ||
                    path.contains("/src/")   ||
                    path.endsWith(".java")   ||
                    path.endsWith(".kt")     ||
                    path.endsWith(".kts")    ||
                    path.endsWith(".xml")    ||
                    path.endsWith(".css")    ||
                    path.endsWith(".js")     ||
                    path.endsWith(".html")   ||
                    path.endsWith(".txt")    ||
                    path.endsWith(".md")     ||
                    path.endsWith(".properties")
                }

                if (!shouldRefresh) return null

                return object : AsyncFileListener.ChangeApplier {

                    override fun afterVfsChange() {
                        scheduleRefresh()
                    }
                }

            }}, this
        )
    }

    private fun scheduleRefresh() {

        refreshJob?.cancel()

        refreshJob = scope.launch {

            delay(150.milliseconds)

            if (project.isDisposed) return@launch

            withContext(Dispatchers.EDT) {

                if (project.isDisposed) return@withContext

                FileStatusManager.getInstance(project).fileStatusesChanged()

                val pane = ProjectView.getInstance(project)
                    .getProjectViewPaneById(MavenProjectViewPane.PANE_ID) as? MavenProjectViewPane

                pane?.refreshTree()

                pane?.tree?.updateUI()
            }
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}