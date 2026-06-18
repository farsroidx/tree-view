@file:Suppress("DialogTitleCapitalization", "UnsafeVfsRecursion")

package ir.farsroidx.maven.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType

class SetupSourceRootsAction : AnAction("Sync Maven Source Roots") {

    override fun actionPerformed(e: AnActionEvent) {

        val project = e.project ?: return

        syncSourceRoots(project)
    }
}

fun syncSourceRoots(project: Project) {

    ApplicationManager.getApplication().runWriteAction {

        val modules = ModuleManager.getInstance(project).modules

        var crashed = false

        for (module in modules) {

            val model = ModuleRootManager.getInstance(module).modifiableModel

            try {

                val contentEntries = model.contentEntries

                if (contentEntries.isEmpty()) continue

                for (contentEntry in contentEntries) {

                    val rootFile = contentEntry.file ?: continue

                    configureDirectory(contentEntry, rootFile)
                }

                model.commit()

                crashed = false

            } catch (_: Throwable) {

                model.dispose()

                crashed = true
            }
        }

        ApplicationManager.getApplication().invokeLater {
            if (crashed) showErrorNotification(project) else showSuccessNotification(project)
        }
    }
}

private fun configureDirectory(contentEntry: ContentEntry, file: VirtualFile) {

    if (file.isDirectory && file.name == "src") {
        findAndRegisterRoots(contentEntry, file)
        return
    }

    if (file.isDirectory) {
        for (child in file.children) {
            configureDirectory(contentEntry, child)
        }
    }
}

private fun findAndRegisterRoots(contentEntry: ContentEntry, srcFolder: VirtualFile) {

    for (categoryFolder in srcFolder.children) {

        val isTest = categoryFolder.name.equals("test", ignoreCase = true)
        val isMain = categoryFolder.name.equals("main", ignoreCase = true)

        if (isMain || isTest) {

            for (folder in categoryFolder.children) {

                when (folder.name.lowercase()) {

                    "java", "kotlin" -> {

                        val rootType = if (isTest) JavaSourceRootType.TEST_SOURCE else JavaSourceRootType.SOURCE

                        if (!contentEntry.sourceFolders.any { it.file == folder }) {
                            contentEntry.addSourceFolder(folder, rootType)
                        }
                    }

                    "resources" -> {

                        val rootType = if (isTest) JavaResourceRootType.TEST_RESOURCE else JavaResourceRootType.RESOURCE

                        if (!contentEntry.sourceFolders.any { it.file == folder }) {
                            contentEntry.addSourceFolder(folder, rootType)
                        }
                    }
                }
            }
        }
    }
}

private fun showSuccessNotification(project: Project) {

    val title = "Sync Completed"
    val content = "Maven source and resource roots synchronized <b>Successfully</b>! 🚀"

    NotificationGroupManager.getInstance()
        .getNotificationGroup("MavenSourceRootsGroup")
        .createNotification(title, content, NotificationType.INFORMATION)
        .notify(project)
}

private fun showErrorNotification(project: Project) {

    val title = "Sync Failed"
    val content = "An error occurred during synchronization. 🛠️❌"

    NotificationGroupManager.getInstance()
        .getNotificationGroup("MavenSourceRootsGroup")
        .createNotification(title, content, NotificationType.ERROR)
        .notify(project)
}