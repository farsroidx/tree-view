package ir.farsroidx.maven

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiManager
import ir.farsroidx.ViewConfig

class MavenTreeStructure(project: Project, id: String) : ProjectTreeStructure(project, id) {

    override fun createRoot(project: Project, viewSettings: ViewSettings): AbstractTreeNode<*> {

        val baseDir = project.guessProjectDir()
            ?: throw IllegalStateException("Project dir not found")

        val psiDirectory = PsiManager.getInstance(project).findDirectory(baseDir)
            ?: throw IllegalStateException("PSI Directory for project root not found")

        return MavenProjectViewNode(
            config = ViewConfig(project, viewSettings), psiDirectory
        )
    }

    override fun isToBuildChildrenInBackground(element: Any): Boolean = true
}
