package ir.farsroidx.gradle

import com.intellij.icons.AllIcons
import com.intellij.ide.SelectInTarget
import com.intellij.ide.util.treeView.AbstractTreeStructureBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import ir.farsroidx.view.BuildProjectViewPane
import org.jetbrains.plugins.gradle.settings.GradleSettings
import javax.swing.Icon

/**
 * Registers the Gradle pane and detects linked or conventional Gradle projects.
 *
 * @param project the owning IntelliJ project
 */
class GradleProjectViewPane(project: Project) : BuildProjectViewPane(project) {

    /**
     * Returns the stable pane identifier used by persisted settings and Select In actions.
     *
     * @return the persisted pane identifier
     */
    override fun getId(): String = PANE_ID

    /**
     * Returns the pane title displayed in the project-view selector.
     *
     * @return the visible pane title
     */
    override fun getTitle(): String = PANE_NAME

    /**
     * Returns the build-system icon displayed for this pane.
     *
     * @return the build-system pane icon
     */
    override fun getIcon(): Icon = AllIcons.Nodes.PpLibFolder

    /**
     * Returns the native display priority for this pane or selection target.
     *
     * @return the platform display priority
     */
    override fun getWeight(): Int = PANE_WEIGHT

    /**
     * Creates the native Select In target bound to this pane identifier.
     *
     * @return the selection target for this pane
     */
    override fun createSelectInTarget(): SelectInTarget = GradleSelectInTarget(myProject, PANE_ID)

    /**
     * Creates the build-system tree structure using native project-view settings.
     *
     * @return the build-system-specific native tree structure
     */
    override fun createStructure(): AbstractTreeStructureBase = GradleTreeStructure(myProject, PANE_ID)

    /**
     * Returns whether linked build metadata or conventional build files make this pane applicable.
     *
     * @return true when this project has applicable build metadata or files
     */
    override fun isInitiallyVisible(): Boolean {

        if (GradleSettings.getInstance(myProject).linkedProjectsSettings.isNotEmpty()) return true

        val directory = myProject.guessProjectDir() ?: return false

        return GradleProjectViewNode.PROJECT_FILE_NAMES.any { directory.findChild(it) != null }

    }

    companion object {

        const val PANE_ID = "FARSROIDX_GRADLE_PROJECT_VIEW_ID"

        const val PANE_NAME = "Gradle Project"

        const val PANE_WEIGHT = 51

    }

}
