package ir.farsroidx.files

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent

/**
 * Filters structural VFS events to relevant, non-excluded project content.
 *
 * @param project the owning IntelliJ project
 */
class FileEventScope(private val project: Project) {

    /**
     * Returns whether a structural event affects visible project content.
     *
     * Content-only changes are handled by native status listeners and do not rebuild the root.
     *
     * @param event the platform event to inspect
     *
     * @return true if the batch event changes relevant project structure
     */
    fun shouldRefresh(event: VFileEvent): Boolean {

        if (event is VFileContentChangeEvent) return false

        val candidates = when (event) {

            is VFileCreateEvent -> {

                if (event.childName in INTERNAL_DIRECTORIES) return false

                listOfNotNull(event.file, event.parent)

            }

            is VFileMoveEvent -> listOf(event.file, event.oldParent, event.newParent)

            is VFileCopyEvent -> {

                if (event.newChildName in INTERNAL_DIRECTORIES) return false

                listOf(event.newParent)

            }

            else -> listOfNotNull(event.file)

        }

        return candidates.any { isProjectFile(it) }

    }

    /**
     * Checks valid ancestors, content membership, exclusions, and internal directory boundaries.
     *
     * @param file the file being represented or tested
     *
     * @return true if the path belongs to relevant non-excluded project content
     */
    private fun isProjectFile(file: VirtualFile): Boolean {

        if (generateSequence(file) { it.parent }.any { it.name in INTERNAL_DIRECTORIES }) return false

        val validFile = generateSequence(file) { it.parent }.firstOrNull { it.isValid } ?: return false

        val index = ProjectFileIndex.getInstance(project)

        if (index.isExcluded(validFile)) return false

        if (index.isInContent(validFile)) return true

        val baseDirectory = project.guessProjectDir() ?: return false

        return VfsUtilCore.isAncestor(baseDirectory, validFile, false)

    }

    companion object {

        private val INTERNAL_DIRECTORIES = setOf(".git", ".idea", ".gradle")

    }

}
