@file:Suppress("unused")

package ir.farsroidx.files

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiTreeChangeListener

class UniversalFileListener(
    private val project: Project,
    private val directory: PsiDirectory? = null,
    private val callback: (event: PsiTreeChangeEvent, type: EventType) -> Unit
) : PsiTreeChangeListener {

    enum class EventType {
        ADDED,
        REMOVED,
        REPLACED,
        MOVED,
        CHILDREN_CHANGED,
        PROPERTY_CHANGED,
        BEFORE_ADDITION,
        BEFORE_REMOVAL,
        BEFORE_REPLACEMENT,
        BEFORE_MOVEMENT,
        BEFORE_CHILDREN_CHANGE,
        BEFORE_PROPERTY_CHANGE
    }

    private fun insideDirectory(event: PsiTreeChangeEvent): Boolean {
        val dir = directory ?: return true
        val file = event.file ?: event.child?.containingFile ?: return false
        return file.virtualFile.path.startsWith(dir.virtualFile.path)
    }

    private fun fire(event: PsiTreeChangeEvent, type: EventType) {
        if (insideDirectory(event)) {
            callback(event, type)
        }
    }

    override fun childAdded(event: PsiTreeChangeEvent) =
        fire(event, EventType.ADDED)

    override fun childRemoved(event: PsiTreeChangeEvent) =
        fire(event, EventType.REMOVED)

    override fun childReplaced(event: PsiTreeChangeEvent) =
        fire(event, EventType.REPLACED)

    override fun childMoved(event: PsiTreeChangeEvent) =
        fire(event, EventType.MOVED)

    override fun childrenChanged(event: PsiTreeChangeEvent) =
        fire(event, EventType.CHILDREN_CHANGED)

    override fun propertyChanged(event: PsiTreeChangeEvent) =
        fire(event, EventType.PROPERTY_CHANGED)

    override fun beforeChildAddition(event: PsiTreeChangeEvent) =
        fire(event, EventType.BEFORE_ADDITION)

    override fun beforeChildRemoval(event: PsiTreeChangeEvent) =
        fire(event, EventType.BEFORE_REMOVAL)

    override fun beforeChildReplacement(event: PsiTreeChangeEvent) =
        fire(event, EventType.BEFORE_REPLACEMENT)

    override fun beforeChildMovement(event: PsiTreeChangeEvent) =
        fire(event, EventType.BEFORE_MOVEMENT)

    override fun beforeChildrenChange(event: PsiTreeChangeEvent) =
        fire(event, EventType.BEFORE_CHILDREN_CHANGE)

    override fun beforePropertyChange(event: PsiTreeChangeEvent) =
        fire(event, EventType.BEFORE_PROPERTY_CHANGE)
}