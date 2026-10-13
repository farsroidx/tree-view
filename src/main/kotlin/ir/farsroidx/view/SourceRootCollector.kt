package ir.farsroidx.view

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import ir.farsroidx.view.model.SourceRoot
import org.jetbrains.jps.model.java.JavaResourceRootType

/**
 * Reads imported source-root metadata and discovers existing conventional fallback roots.
 *
 * Collection does not modify module models or create directories.
 */
object SourceRootCollector {

    /**
     * Returns distinct imported roots from non-disposed modules, preserving test and resource metadata.
     *
     * @param modules imported IDE modules to inspect
     *
     * @return distinct imported source and resource roots
     */
    fun collect(modules: Collection<Module>): List<SourceRoot> {

        return modules.filterNot { it.isDisposed }.flatMap { module ->

            ModuleRootManager.getInstance(module).contentEntries.flatMap { entry ->

                entry.sourceFolders.mapNotNull { folder ->

                    val directory = folder.file ?: return@mapNotNull null

                    SourceRoot(
                        directory,
                        folder.isTestSource,
                        folder.rootType is JavaResourceRootType,
                        folder.packagePrefix,
                        ProjectFileIndex.getInstance(module.project).isInGeneratedSources(directory)
                    )

                }

            }

        }.distinctBy { it.directory }

    }

    /**
     * Returns existing src/main and src/test Java, Kotlin, Groovy, and resource directories.
     *
     * @param directory the physical directory represented by this node
     *
     * @return existing conventional roots, or an empty list for an invalid directory
     */
    fun conventional(directory: VirtualFile): List<SourceRoot> {

        if (!directory.isValid || !directory.isDirectory) return emptyList()

        return SOURCE_CATEGORIES.flatMap { category ->

            SOURCE_KINDS.mapNotNull { kind ->

                val sourceDirectory = directory.findFileByRelativePath("src/$category/$kind")
                    ?.takeIf { it.isDirectory } ?: return@mapNotNull null

                SourceRoot(
                    sourceDirectory,
                    category == TEST_CATEGORY,
                    kind == RESOURCE_KIND
                )

            }

        }

    }

    private const val TEST_CATEGORY = "test"

    private const val RESOURCE_KIND = "resources"

    private val SOURCE_CATEGORIES = listOf("main", TEST_CATEGORY)

    private val SOURCE_KINDS = listOf("java", "kotlin", "groovy", RESOURCE_KIND)

}
