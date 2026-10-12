package ir.farsroidx.maven

import ir.farsroidx.view.model.ModuleOrder
import org.jetbrains.idea.maven.project.MavenProject

/**
 * Returns Maven projects sorted by root status, farsroidx.order, and module name.
 *
 * Unread or invalid numeric priorities are treated as absent.
 *
 * @param rootMavenProject optional root project placed before other modules
 *
 * @return a new list in root, priority, and name order
 */
fun List<MavenProject>.sortingByOrder(rootMavenProject: MavenProject? = null): List<MavenProject> {

    return sortedBy { project ->

        ModuleOrder(
            if (project.isNew) project.directoryFile.name else {

                project.mavenId.artifactId ?: project.name ?: project.directoryFile.name

            },
            if (project.lastReadStamp == UNREAD_PROJECT_STAMP || project.hasReadingErrors()) null else {

                project.properties.getProperty(MODULE_ORDER_PROPERTY)?.toLongOrNull()

            },
            project == rootMavenProject
        )

    }

}

private const val MODULE_ORDER_PROPERTY = "farsroidx.order"

internal const val UNREAD_PROJECT_STAMP = 0L
