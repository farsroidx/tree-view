# Architecture

## Responsibilities

| Package | Responsibility |
| --- | --- |
| `ir.farsroidx` | Startup, view configuration, and icon helpers. |
| `files` | Project-scoped event filtering and merged structural refresh scheduling. |
| `view.model` | Build-independent module layouts, semantic roots, and ordering keys. |
| `view` | Native asynchronous pane lifecycle and shared root collection. |
| `view.nodes` | Concrete PSI nodes and synthetic build-file groups. |
| `view.decorator` | Test presentation derived from semantic node ancestry. |
| `maven` | Maven metadata adapter, pane, POM presentation, and root synchronization. |
| `gradle` | Gradle external-model adapter, pane, and configuration-file presentation. |

## Native integration

`BuildProjectViewPane` extends `AbstractProjectViewPaneWithAsyncSupport`. IntelliJ owns asynchronous loading, selection data, Move and drag-and-drop integration, problem/VCS listeners, and native state persistence. The plugin customizes root construction, module order, section order, and presentation. Legacy `TreeState` is applied after native root loading.

Concrete nodes extend `PsiDirectoryNode` and `PsiFileNode`. Synthetic build-file groups use string values and expose physical file roots through `RootsProvider`; they do not represent physical directories. Global Gradle properties are included in root containment for native selection.

`PackageNode` holds both ends of a compact directory chain. Validity checks the physical endpoints rather than the synthetic module parent: physical ancestry validation would reject valid packages when intermediate source-root directories are hidden.

The comparator preserves adapter-provided root order. Other nodes are grouped as source, test source, resources, and test resources before additional content, with native comparison inside each section.

## Adapter boundary

Maven and Gradle adapters produce `ModuleLayout` values consumed by shared nodes. This adapter boundary isolates build-specific metadata from presentation. `ModuleOrder` supplies a shared ordering policy.

Maven combines imported module roots, Maven metadata, and existing conventional directories. Unread metadata is guarded before accessing output paths and ordering properties. Empty aggregators are omitted from the source list while their POMs remain represented.

Gradle combines linked settings, imported external `MODULE` data, and associated IDE modules. Physical-directory deduplication avoids duplicate main/test source-set modules. Imported paths preserve remapped module directories. The plugin does not evaluate Gradle scripts to reconstruct the build graph.

`SourceRootCollector` preserves imported root categories and package prefixes before applying conventional fallbacks. The separate Maven synchronization action runs in an undoable write command, preserves existing roots, disposes modifiable models, propagates cancellation, and logs failures before notification.

## Refresh and lifetime

`ViewActivity` initializes the project-scoped watcher. Message-bus listeners and the merging queue are bound to its disposable lifetime. Structural VFS batches, root-model changes, and external imports schedule merged refreshes.

Event filtering considers create parents, copy destinations, both move parents, and valid ancestors of deleted files. Excluded/internal paths do not trigger structural rebuilds. Native listeners handle content-only status updates. Pane refreshes preserve native expansion and selection state.

## Verification

Platform tests cover semantic nodes, compact-package visibility in the native tree, section ordering, selection, pane reuse, and problem presentation on unsaved documents. Adapter tests cover Maven aggregation and Gradle source sets, remapped directories, priorities, global properties, and version catalogs. Other tests cover event scope and idempotent root registration.

The build targets JVM 17 and declares IDE build 261 as its minimum. Tests on a newer local SDK do not establish compatibility with every supported build. Run the platform suite and Plugin Verifier against the minimum and intended release targets before claiming complete compatibility coverage.
