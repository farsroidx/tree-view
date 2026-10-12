package ir.farsroidx.gradle

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import ir.farsroidx.ViewConfig
import ir.farsroidx.gradle.nodes.GradleBuildFileNode
import ir.farsroidx.view.SourceRootCollector
import ir.farsroidx.view.model.ModuleLayout
import ir.farsroidx.view.model.ModuleOrder
import ir.farsroidx.view.nodes.BuildFilesNode
import ir.farsroidx.view.nodes.BuildProjectNode
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Adapts linked Gradle projects and imported modules to the shared tree model.
 *
 * Deduplicates source-set modules by physical directory and preserves remapped module locations.
 * Module ordering reads farsroidx.order from each module-local gradle.properties file.
 *
 * @param config the project and native view settings
 */
class GradleProjectViewNode(config: ViewConfig) : BuildProjectNode(config) {

    /**
     * Returns distinct build-module layouts with imported roots, fallback roots, and hidden outputs.
     *
     * @return ordered, physically distinct module layouts
     */
    override fun collectLayouts(): List<ModuleLayout> {

        val modules = ModuleManager.getInstance(project).modules.filter { module ->

            ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)

        }

        val modulesByPath = modules.groupBy { module ->

            ExternalSystemApiUtil.getExternalProjectPath(module)

        }

        val projectNames = linkedMapOf<String, String>()

        GradleSettings.getInstance(project).linkedProjectsSettings.forEach { linkedProject ->

            val path = linkedProject.externalProjectPath

            val projectNode = ExternalSystemApiUtil.findProjectNode(project, GradleConstants.SYSTEM_ID, path)

            projectNames[path] = projectNode?.data?.externalName ?: project.name

            if (projectNode != null) {

                ExternalSystemApiUtil.findAllRecursively(projectNode, ProjectKeys.MODULE).forEach { node ->

                    projectNames.putIfAbsent(node.data.linkedExternalProjectPath, node.data.externalName)

                }

            }

        }

        modulesByPath.keys.filterNotNull().forEach { path ->

            projectNames.putIfAbsent(path, path.substringAfterLast('/').substringAfterLast('\\'))

        }

        if (projectNames.isEmpty()) {

            val directory = project.guessProjectDir() ?: return emptyList()

            if (PROJECT_FILE_NAMES.none { directory.findChild(it) != null }) return emptyList()

            projectNames[directory.path] = project.name

        }

        return projectNames.mapNotNull { (path, name) ->

            val directory = LocalFileSystem.getInstance().findFileByPath(path.replace('\\', '/'))
                ?.takeIf { it.isDirectory } ?: return@mapNotNull null

            val associatedModules = modulesByPath[path].orEmpty()

            val roots = SourceRootCollector.collect(associatedModules) +
                SourceRootCollector.conventional(directory)

            ModuleLayout(
                name,
                directory,
                roots.distinctBy { it.directory },
                BUILD_FILE_NAMES.mapNotNull { directory.findFileByRelativePath(it) }.toSet(),
                associatedModules.flatMap { module ->

                    ModuleRootManager.getInstance(module).excludeRoots.toList()

                }.toSet() + listOfNotNull(directory.findChild("build")),
                showAdditionalFiles = GradleSettings.getInstance(project).linkedProjectsSettings.none {

                    it.externalProjectPath.replace('\\', '/') == directory.path

                }
            )

        }.distinctBy { it.directory }.sortedBy { layout ->

            val properties = layout.directory.findChild("gradle.properties")
                ?.let { PsiManager.getInstance(project).findFile(it)?.text }.orEmpty()

            val order = MODULE_ORDER_PATTERN.find(properties)?.groupValues?.get(1)?.toLongOrNull()

            ModuleOrder(layout.name, order, !layout.showAdditionalFiles)

        }

    }

    /**
     * Creates the synthetic build-file group with concrete, selectable PSI-file children.
     *
     * @param layouts the ordered build-module layouts
     *
     * @return the synthetic group containing selectable build-file nodes
     */
    override fun buildFilesNode(layouts: List<ModuleLayout>): ProjectViewNode<*> {

        val settings = GradleSettings.getInstance(project)

        val userHome = settings.serviceDirectoryPath?.takeIf { it.isNotBlank() }
            ?: System.getenv("GRADLE_USER_HOME")?.takeIf { it.isNotBlank() }
            ?: "${System.getProperty("user.home")}/.gradle"

        val globalProperties = LocalFileSystem.getInstance()
            .findFileByPath("${userHome.replace('\\', '/')}/gradle.properties")

        val files = (layouts.flatMap { it.buildFiles } + listOfNotNull(globalProperties)).distinct()

        return BuildFilesNode(config, "Gradle Builds", AllIcons.Nodes.PpLibFolder, files) {

            val psiManager = PsiManager.getInstance(project)

            files.mapNotNull { file ->

                val psiFile = psiManager.findFile(file) ?: return@mapNotNull null

                val moduleName = if (file == globalProperties) {

                    "Gradle User Home"

                } else {

                    layouts.firstOrNull { file in it.buildFiles }?.name ?: project.name

                }

                GradleBuildFileNode(config, psiFile, moduleName)

            }

        }

    }

    companion object {

        val PROJECT_FILE_NAMES = listOf(
            "build.gradle",
            "build.gradle.kts",
            "settings.gradle",
            "settings.gradle.kts"
        )

        val BUILD_FILE_NAMES = PROJECT_FILE_NAMES + listOf("gradle.properties", "gradle/libs.versions.toml")

        private val MODULE_ORDER_PATTERN = Regex("(?m)^\\s*farsroidx\\.order\\s*[=:]\\s*([+-]?\\d+)\\s*$")

    }

}
