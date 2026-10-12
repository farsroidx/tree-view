package ir.farsroidx.files

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl

class FileEventScopeTest : BasePlatformTestCase() {

    override fun createTempDirTestFixture(): TempDirTestFixture {

        return TempDirTestFixtureImpl()

    }

    override fun setUp() {

        super.setUp()

        val directory = myFixture.tempDirFixture.getFile("")!!

        ApplicationManager.getApplication().runWriteAction {

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

    fun testCreateWithoutResolvedFileUsesItsParent() {

        val directory = myFixture.tempDirFixture.getFile("")!!

        val event = VFileCreateEvent(this, directory, "new.txt", false, null, null, null)

        assertNull(event.file)

        assertTrue(FileEventScope(project).shouldRefresh(event))

    }

    fun testDeletedFileUsesItsSurvivingParent() {

        val file = myFixture.addFileToProject("folder/file.txt", "content").virtualFile

        val event = VFileDeleteEvent(this, file)

        WriteCommandAction.runWriteCommandAction(project) {

            file.delete(this)

        }

        assertFalse(file.isValid)

        assertTrue(FileEventScope(project).shouldRefresh(event))

    }

    fun testContentChangesDoNotRebuildTheTree() {

        val file = myFixture.addFileToProject("file.txt", "content").virtualFile

        val event = VFileContentChangeEvent(
            this,
            file,
            file.modificationStamp,
            file.modificationStamp
        )

        assertFalse(FileEventScope(project).shouldRefresh(event))

    }

    fun testCopyIntoProjectUsesDestinationScope() {

        val file = myFixture.addFileToProject(".gradle/cache.txt", "cache").virtualFile

        val destination = myFixture.tempDirFixture.findOrCreateDir("destination")

        val event = VFileCopyEvent(this, file, destination, "file.txt")

        assertTrue(FileEventScope(project).shouldRefresh(event))

    }

    fun testMoveIntoIgnoredDirectoryStillRefreshesOriginalParent() {

        val file = myFixture.addFileToProject("folder/file.txt", "content").virtualFile

        val destination = myFixture.tempDirFixture.findOrCreateDir(".gradle")

        val event = VFileMoveEvent(this, file, destination)

        WriteCommandAction.runWriteCommandAction(project) {

            file.move(this, destination)

        }

        assertTrue(FileEventScope(project).shouldRefresh(event))

    }

    fun testIgnoredDirectoryCreationDoesNotRefreshTree() {

        val directory = myFixture.tempDirFixture.getFile("")!!

        val event = VFileCreateEvent(this, directory, ".gradle", true, null, null, null)

        assertFalse(FileEventScope(project).shouldRefresh(event))

    }

}
