# Tree View 1.2.0

This minor release adds Gradle support and moves both panes to IntelliJ's native asynchronous project-view infrastructure. Maven and Gradle keep separate adapters while sharing source layout and node behavior.

## Features

- Groovy/Kotlin DSL Gradle builds, imported multi-module layouts, and custom source sets.
- Grouped build/settings files, module properties, conventional version catalogs, and global Gradle properties.
- Owning-module labels and `farsroidx.order` priorities in module-local properties files.
- Source, SourceTest, Resources, ResourcesTest ordering in both panes.

## Fixes

- Native PSI selection, Move/refactoring, and drag-and-drop integration.
- Error presentation follows background analysis without a manual save.
- Compact packages remain visible with empty-package hiding enabled.
- Reduced duplicate modules, wrapper folders, label paths, and structural refreshes.
- Additive Maven root synchronization with model cleanup and failure reporting.

## Upgrade and configuration

Plugin and pane identifiers remain unchanged. Older custom tree state is migrated when a pane is created. Existing Maven ordering properties remain supported.

Gradle ordering is read from each module's own `gradle.properties`, not inherited from global/root properties or evaluated from scripts. Sync the build to populate its imported module graph. Version-catalog discovery covers `gradle/libs.versions.toml` only.

## Validation boundaries

Checks include platform tests, IDE compilation, ZIP packaging, and descriptor structure validation. Native tree tests cover visibility, ordering, selection, pane reuse, and unsaved-document problem presentation. These checks do not replace manual mouse-based drag-and-drop testing or Plugin Verifier coverage across the declared IDE range.
