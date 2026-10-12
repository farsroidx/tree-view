# Changelog

## 1.2.0 - 2026-10-12

### Added

- Separate Gradle pane for Groovy and Kotlin DSL, imported multi-module builds, and custom source sets.
- Grouped Gradle build/settings files, module properties, conventional version catalogs, and global Gradle User Home properties.
- Owning-module labels on Gradle files and module-local `farsroidx.order` priorities.
- Shared semantic source-root model and platform regression tests.
- Source KDoc, architecture documentation, and updated configuration examples.

### Fixed

- Restored native selection, Move/refactoring, and drag-and-drop integration through PSI nodes and the asynchronous project-view infrastructure.
- Problem indicators follow native analysis without requiring a manual save.
- Compacted packages remain visible with native empty-package hiding enabled.
- Enforced Source, SourceTest, Resources, ResourcesTest ordering; absent sections are omitted.
- Removed duplicate source-set modules, redundant source wrappers, and absolute paths beside module labels.
- Reduced structural refresh pressure and retained native state across refreshes and pane switches.
- Limited Maven root synchronization to existing conventional roots in Maven modules, preserving configured roots and reporting failures.

### Changed

- Replaced custom tree plumbing with a shared native asynchronous pane and separate Maven/Gradle adapters.
- Updated the Marketplace description and README to reflect current features and their boundaries.
- Updated the Kotlin compiler configuration to 2.3.20 while retaining JVM 17 bytecode.

## 1.1.2

- Addressed persistent file-error presentation and document-commit synchronization.

## 1.1.1

- Addressed loading-state restoration, pane switching, and refresh storms.

## 1.1.0

- Refined test highlighting and project-tree presentation.

## 1.0.0

- Initial Maven view with compact packages, grouped POMs, module ordering, VCS colors, and tree-state persistence.
