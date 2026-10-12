package ir.farsroidx.maven

import com.intellij.ide.SelectInTarget
import com.intellij.ide.util.treeView.AbstractTreeStructureBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import icons.MavenIcons
import ir.farsroidx.view.BuildProjectViewPane
import org.jetbrains.idea.maven.project.MavenProjectsManager
import javax.swing.Icon

/**
 * Registers the Maven pane and refreshes it after Maven activation and imports.
 *
 * @param project the owning IntelliJ project
 */
class MavenProjectViewPane(project: Project) : BuildProjectViewPane(project) {

    init {

        MavenProjectsManager.getInstance(project).addManagerListener(
            object : MavenProjectsManager.Listener {

                /**
                 * Schedules a refresh when Maven support becomes active.
                 */
                override fun activated() {

                    scheduleRefresh()

                }

                /**
                 * Schedules a refresh after Maven module import completes.
                 */
                override fun projectImportCompleted() {

                    scheduleRefresh()

                }

            },
            this
        )

    }

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
    override fun getIcon(): Icon = MavenIcons.MavenProject

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
    override fun createSelectInTarget(): SelectInTarget = MavenSelectInTarget(myProject, PANE_ID)

    /**
     * Creates the build-system tree structure using native project-view settings.
     *
     * @return the build-system-specific native tree structure
     */
    override fun createStructure(): AbstractTreeStructureBase = MavenTreeStructure(myProject, PANE_ID)

    /**
     * Returns whether linked build metadata or conventional build files make this pane applicable.
     *
     * @return true when this project has applicable build metadata or files
     */
    override fun isInitiallyVisible(): Boolean {

        val manager = MavenProjectsManager.getInstance(myProject)

        return manager.isMavenizedProject || manager.hasProjects() ||
            myProject.guessProjectDir()?.findChild("pom.xml") != null

    }

    companion object {

        const val PANE_ID = "FARSROIDX_MAVEN_PROJECT_VIEW_ID"

        const val PANE_NAME = "Maven Project"

        const val PANE_WEIGHT = 50

    }

}
