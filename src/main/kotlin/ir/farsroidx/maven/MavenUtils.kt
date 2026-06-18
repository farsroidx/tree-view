package ir.farsroidx.maven

import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import ir.farsroidx.ViewConfig
import org.jetbrains.idea.maven.project.MavenProject

fun List<MavenProject>.sortingByOrder(rootMavenProject: MavenProject? = null): List<MavenProject> {

    return this.sortedWith { a, b ->

        if (rootMavenProject != null) {
            if (a == rootMavenProject) return@sortedWith -1
            if (b == rootMavenProject) return@sortedWith 1
        }

        val orderA = a.properties.getProperty("farsroidx.order")?.toLongOrNull()
        val orderB = b.properties.getProperty("farsroidx.order")?.toLongOrNull()

        val nameA = a.mavenId.artifactId ?: a.name ?: ""
        val nameB = b.mavenId.artifactId ?: b.name ?: ""

        when {

            orderA != null && orderB != null -> {

                val orderCompare = orderA.compareTo(orderB)

                if (orderCompare != 0) orderCompare else {
                    nameA.compareTo(nameB, ignoreCase = true)
                }
            }

            orderA != null -> -1

            orderB != null -> 1

            else -> nameA.compareTo(nameB, ignoreCase = true)
        }
    }
}

fun PsiElement.notNullChildren(config: ViewConfig, subProjects: Set<String> = setOf()): List<AbstractTreeNode<*>> {

    return this.children.mapNotNull { psiElem ->

        val name = (psiElem as? PsiNamedElement)?.name ?: return@mapNotNull null

        if (
            name == "src" ||
            name == "target" ||
            name == "pom.xml" ||
            name.startsWith(".") ||
            name.endsWith(".iml") ||
            subProjects.contains(name)
        ) return@mapNotNull null

        val node: AbstractTreeNode<*>? = when (psiElem) {
            is PsiDirectory -> PsiDirectoryNode(config.project, psiElem, config.viewSettings)
            is PsiFile -> PsiFileNode(config.project, psiElem, config.viewSettings)
            else -> null
        }

        node
    }
}