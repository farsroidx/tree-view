package ir.farsroidx.maven.actions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import org.jetbrains.jps.model.java.JavaSourceRootType

class SetupSourceRootsActionTest : BasePlatformTestCase() {

    override fun createTempDirTestFixture(): TempDirTestFixture {

        return TempDirTestFixtureImpl()

    }

    override fun tearDown() {

        try {

            ApplicationManager.getApplication().runWriteAction {

                ExternalSystemModulePropertyManager.getInstance(module).setMavenized(false)

            }

        } finally {

            super.tearDown()

        }

    }

    fun testSynchronizationIsIdempotent() {

        val source = myFixture.addFileToProject("src/main/java/Main.txt", "main").virtualFile.parent

        val resource = myFixture.addFileToProject("src/test/resources/test.properties", "value=true")
            .virtualFile.parent

        prepareModule(true)

        syncSourceRoots(project)

        val firstRoots = ModuleRootManager.getInstance(module).sourceRoots.toSet()

        assertTrue(source in firstRoots)

        assertTrue(resource in firstRoots)

        syncSourceRoots(project)

        assertEquals(firstRoots, ModuleRootManager.getInstance(module).sourceRoots.toSet())

    }

    fun testExistingCustomSourceRootIsPreserved() {

        val customRoot = myFixture.addFileToProject("custom/Case.txt", "case").virtualFile.parent

        val directory = myFixture.tempDirFixture.getFile("")!!

        prepareModule(true)

        ApplicationManager.getApplication().runWriteAction {

            val model = ModuleRootManager.getInstance(module).modifiableModel

            try {

                model.contentEntries.single { it.file == directory }.addSourceFolder(
                    customRoot,
                    JavaSourceRootType.TEST_SOURCE
                ).packagePrefix = "custom.prefix"

                model.commit()

            } finally {

                if (!model.isDisposed) {

                    model.dispose()

                }

            }

        }

        syncSourceRoots(project)

        val folder = ModuleRootManager.getInstance(module).contentEntries
            .flatMap { it.sourceFolders.toList() }
            .single { it.file == customRoot }

        assertEquals(JavaSourceRootType.TEST_SOURCE, folder.rootType)

        assertEquals("custom.prefix", folder.packagePrefix)

    }

    fun testNonMavenModulesAreNotChanged() {

        myFixture.addFileToProject("src/main/java/Main.txt", "main")

        prepareModule(false)

        val before = ModuleRootManager.getInstance(module).sourceRoots.toSet()

        syncSourceRoots(project)

        assertEquals(before, ModuleRootManager.getInstance(module).sourceRoots.toSet())

    }

    private fun prepareModule(isMaven: Boolean) {

        val directory = myFixture.tempDirFixture.getFile("")!!

        ApplicationManager.getApplication().runWriteAction {

            ExternalSystemModulePropertyManager.getInstance(module).setMavenized(isMaven)

            val model = ModuleRootManager.getInstance(module).modifiableModel

            try {

                model.addContentEntry(directory)

                model.commit()

            } finally {

                if (!model.isDisposed) {

                    model.dispose()

                }

            }

        }

    }

}
