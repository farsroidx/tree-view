package ir.farsroidx.files

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import ir.farsroidx.view.BuildProjectViewPane

/**
 * Coalesces structural, module-root, and import events into disposable pane refreshes.
 *
 * Document analysis and file colors remain managed by the native project view.
 *
 * @param project the owning IntelliJ project
 */
@Service(Service.Level.PROJECT)
class UniversalFileWatcher(private val project: Project) : Disposable {

    private val eventScope = FileEventScope(project)

    private val refreshQueue = MergingUpdateQueue(
        "TreeView.Structure",
        REFRESH_DELAY,
        true,
        null,
        this
    )

    init {

        val connection = project.messageBus.connect(this)

        connection.subscribe(
            ModuleRootListener.TOPIC,
            object : ModuleRootListener {

                /**
                 * Schedules a structural refresh after the module-root model changes.
                 *
                 * @param event the platform event to inspect
                 */
                override fun rootsChanged(event: ModuleRootEvent) {

                    scheduleRefresh()

                }

            }
        )

        connection.subscribe(
            ProjectDataImportListener.TOPIC,
            object : ProjectDataImportListener {

                /**
                 * Schedules a structural refresh after external project import completes.
                 *
                 * @param projectPath the imported external project path
                 */
                override fun onImportFinished(projectPath: String?) {

                    scheduleRefresh()

                }

            }
        )

        connection.subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {

                /**
                 * Schedules one merged refresh when a VFS batch contains relevant structural changes.
                 *
                 * @param events the completed VFS event batch
                 */
                override fun after(events: List<VFileEvent>) {

                    if (events.any { eventScope.shouldRefresh(it) }) {

                        scheduleRefresh()

                    }

                }

            }
        )

    }

    /**
     * Coalesces pending structural refreshes and preserves native tree state.
     *
     * Disposed projects are ignored; refresh work is scheduled rather than performed immediately.
     */
    private fun scheduleRefresh() {

        if (project.isDisposed) return

        refreshQueue.queue(
            Update.create(REFRESH_ID) {

                if (!project.isDisposed) {

                    val projectView = ProjectView.getInstance(project)

                    projectView.paneIds.mapNotNull { projectView.getProjectViewPaneById(it) }
                        .filterIsInstance<BuildProjectViewPane>()
                        .forEach { it.scheduleRefresh() }

                }

            }
        )

    }

    /**
     * Ends the service lifetime; registered queues and message-bus connections dispose with this owner.
     */
    override fun dispose() {

    }

    companion object {

        private const val REFRESH_DELAY = 150

        private const val REFRESH_ID = "TreeView.Structure"

    }

}
