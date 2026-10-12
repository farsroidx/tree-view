package ir.farsroidx.maven.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import ir.farsroidx.view.SourceRootCollector
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType

/**
 * Exposes bounded, additive synchronization of conventional roots for Maven modules.
 */
class SetupSourceRootsAction : AnAction("Sync Maven Source Roots") {

    /**
     * Uses the background action-update thread for Maven availability checks.
     *
     * @return the background action-update thread
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Enables and shows the action only when the project has imported Maven projects.
     *
     * @param event the platform event to inspect
     */
    override fun update(event: AnActionEvent) {

        val project = event.project

        event.presentation.isEnabledAndVisible = project != null &&
            MavenProjectsManager.getInstance(project).hasProjects()

    }

    /**
     * Synchronizes missing conventional Maven roots for the event project.
     *
     * @param event the platform event to inspect
     */
    override fun actionPerformed(event: AnActionEvent) {

        val project = event.project ?: return

        syncSourceRoots(project)

    }

}

/**
 * Adds missing conventional roots to Maven modules in an undoable write command.
 *
 * Existing roots are preserved and repeated calls are idempotent. Model failures are logged
 * and summarized in a notification; cancellation propagates to the caller.
 *
 * @param project the owning IntelliJ project
 *
 * @throws com.intellij.openapi.progress.ProcessCanceledException if synchronization is canceled
 */
fun syncSourceRoots(project: Project) {

    val manager = MavenProjectsManager.getInstance(project)

    var failedModules = 0

    var addedRoots = 0

    WriteCommandAction.runWriteCommandAction(project, "Sync Maven Source Roots", null, Runnable {

        ModuleManager.getInstance(project).modules.filter { manager.isMavenizedModule(it) }.forEach { module ->

            val model = ModuleRootManager.getInstance(module).modifiableModel

            try {

                model.contentEntries.forEach entryLoop@ { entry ->

                    val directory = entry.file ?: return@entryLoop

                    SourceRootCollector.conventional(directory).forEach rootLoop@ { root ->

                        if (!VfsUtilCore.isAncestor(directory, root.directory, true)) return@rootLoop

                        if (model.sourceRoots.any { it == root.directory }) return@rootLoop

                        val type = when {

                            root.isResource && root.isTest -> JavaResourceRootType.TEST_RESOURCE

                            root.isResource -> JavaResourceRootType.RESOURCE

                            root.isTest -> JavaSourceRootType.TEST_SOURCE

                            else -> JavaSourceRootType.SOURCE

                        }

                        entry.addSourceFolder(root.directory, type)

                        addedRoots++

                    }

                }

                if (model.isChanged) {

                    model.commit()

                }

            } catch (exception: ProcessCanceledException) {

                throw exception

            } catch (exception: Exception) {

                LOG.warn("Failed to synchronize source roots for module ${module.name}.", exception)

                failedModules++

            } finally {

                if (!model.isDisposed) {

                    model.dispose()

                }

            }

        }

    })

    val type = if (failedModules == 0) NotificationType.INFORMATION else NotificationType.WARNING

    val content = if (failedModules == 0) {

        "Maven source roots synchronized. Added $addedRoots missing roots."

    } else {

        "Source root synchronization failed for $failedModules modules. See the IDE log for details."

    }

    NotificationGroupManager.getInstance()
        .getNotificationGroup(NOTIFICATION_GROUP)
        .createNotification("Source Root Sync", content, type)
        .notify(project)

}

private const val NOTIFICATION_GROUP = "MavenSourceRootsGroup"

private val LOG = Logger.getInstance(SetupSourceRootsAction::class.java)
