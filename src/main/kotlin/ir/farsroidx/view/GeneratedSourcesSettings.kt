package ir.farsroidx.view

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@Service(Service.Level.PROJECT)
@State(name = "TreeViewGeneratedSources", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class GeneratedSourcesSettings : PersistentStateComponent<GeneratedSourcesSettings.SettingsState> {

    private var settings = SettingsState()

    var showGeneratedSources: Boolean
        get() = settings.showGeneratedSources
        set(value) {

            settings.showGeneratedSources = value

        }

    override fun getState(): SettingsState {

        return settings

    }

    override fun loadState(state: SettingsState) {

        settings = state

    }

    class SettingsState {

        @Volatile
        var showGeneratedSources = false

    }

}
