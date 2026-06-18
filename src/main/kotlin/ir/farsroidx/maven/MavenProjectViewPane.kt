package ir.farsroidx.maven

import com.intellij.ide.SelectInTarget
import com.intellij.ide.projectView.impl.AbstractProjectViewPane
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.util.ui.UIUtil
import icons.MavenIcons
import ir.farsroidx.files.UniversalFileListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jdom.Element
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.awt.BorderLayout
import javax.swing.*

class MavenProjectViewPane(private val project: Project) : AbstractProjectViewPane(project) {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var structureModel: StructureTreeModel<*>? = null

    private var restoredStateFromXml: TreeState? = null

    private var isInitialRestorationDone = false

    private val cardLayout = java.awt.CardLayout()

    private val containerPanel = JPanel(cardLayout)

    init {

        myTreeStructure = MavenTreeStructure(project, PANE_ID)

        PsiManager.getInstance(project)
            .addPsiTreeChangeListener(UniversalFileListener(project) { _, _ -> refreshTree() }, this)
    }

    override fun getId(): String = PANE_ID

    override fun getTitle(): String = PANE_NAME

    override fun getIcon(): Icon = MavenIcons.MavenProject

    override fun createSelectInTarget(): SelectInTarget = MavenSelectInTarget(project, PANE_ID)

    override fun getWeight(): Int = PANE_WEIGHT

    override fun isInitiallyVisible(): Boolean {
        return try { isMavenProjectSmart(project) } catch (_: Exception) { true }
    }

    override fun createComponent(): JComponent {

        val treeStructure = myTreeStructure

        val structureModel = StructureTreeModel(treeStructure, Comparator { node1, node2 ->

            if (node1 is AbstractTreeNode<*> && node2 is AbstractTreeNode<*>) {

                val isRootLevel1 = node1.parent == null || node1.parent?.value is Project
                val isRootLevel2 = node2.parent == null || node2.parent?.value is Project

                if (isRootLevel1 || isRootLevel2) return@Comparator 0

                val isDir1 = node1 is PsiDirectoryNode
                val isDir2 = node2 is PsiDirectoryNode

                if (isDir1 != isDir2) return@Comparator if (isDir1) -1 else 1

                val name1 = node1.name?.lowercase() ?: ""
                val name2 = node2.name?.lowercase() ?: ""

                return@Comparator name1.compareTo(name2)
            }

            0

        }, this)

        this.structureModel = structureModel

        val asyncTreeModel = AsyncTreeModel(structureModel, this)

        val tree = MavenProjectViewTree(project, asyncTreeModel)

        val actionGroup = ActionManager.getInstance()
            .getAction("ProjectViewPopupMenu") as? DefaultActionGroup

        if (actionGroup != null) {

            PopupHandler.installPopupMenu(tree, actionGroup, ActionPlaces.PROJECT_VIEW_POPUP)
        }

        this.myTree = tree

        val loadingPanel = JPanel(BorderLayout()).apply {

            val loadingLabel = JLabel("Loading Maven Tree...", SwingConstants.CENTER).apply {
                icon = AnimatedIcon.Default.INSTANCE
                foreground = UIUtil.getInactiveTextColor()
            }

            add(loadingLabel, BorderLayout.CENTER)
        }

        val scrollPane = ScrollPaneFactory.createScrollPane(tree)

        containerPanel.removeAll()

        containerPanel.add(loadingPanel, CARD_LOADING)
        containerPanel.add(scrollPane, CARD_TREE)

        cardLayout.show(containerPanel, CARD_LOADING)

        ApplicationManager.getApplication().invokeLater {

            if (!project.isDisposed) {

                refreshTree()
            }
        }

        return containerPanel
    }

    override fun updateFromRoot(restoreExpandedPaths: Boolean): ActionCallback {

        val callback = ActionCallback()

        val model = structureModel

        if (model != null) {

            model.invalidateAsync()

            callback.setDone()

        } else callback.setDone()

        return callback
    }

    override fun select(element: Any?, file: VirtualFile?, requestFocus: Boolean) {

    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)

        val state = saveState()

        if (state != null) {

            val stateElement = Element("TreeState")

            state.writeExternal(stateElement)

            element.addContent(stateElement)
        }
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)

        val stateElement = element.getChild("TreeState")

        if (stateElement != null) {
            restoredStateFromXml = TreeState.createFrom(stateElement)
        }
    }

    private fun isMavenProjectSmart(project: Project): Boolean {

        val mavenManager = MavenProjectsManager.getInstance(project)

        if (mavenManager.isMavenizedProject || mavenManager.hasProjects()) return true

        return project.guessProjectDir()?.findChild("pom.xml")?.exists() == true
    }

    fun refreshTree() {

        val model = structureModel ?: return

        val tree = myTree ?: return

        val state = if (!isInitialRestorationDone && restoredStateFromXml != null) {
            restoredStateFromXml
        } else {
            saveState()
        }

        model.invalidateAsync()

        coroutineScope.launch(Dispatchers.EDT) {

            ApplicationManager.getApplication().invokeLater {

                if (project.isDisposed) return@invokeLater

                if (state != null) restoreState(state)

                val treeModel = tree.model

                val root = treeModel.root

                val hasNodes = root != null && treeModel.getChildCount(root) > 0

                val mavenManager = MavenProjectsManager.getInstance(project)

                val isMavenReady = mavenManager.isInitialized && (hasNodes || !mavenManager.hasProjects())

                if (!isInitialRestorationDone && isMavenReady) {

                    isInitialRestorationDone = true

                    restoredStateFromXml = null

                    cardLayout.show(containerPanel, CARD_TREE)
                }

                tree.revalidate()

                tree.repaint()
            }
        }
    }

    fun saveState(): TreeState? {
        val tree = myTree ?: return null
        return TreeState.createOn(tree)
    }

    fun restoreState(state: TreeState?) {
        val tree = myTree ?: return
        state?.applyTo(tree)
    }

    companion object {

        const val PANE_ID = "FARSROIDX_MAVEN_PROJECT_VIEW_ID"

        const val PANE_NAME = "Maven Project"

        const val PANE_WEIGHT = 50

        private const val CARD_LOADING = "LOADING"

        private const val CARD_TREE = "TREE"

    }
}