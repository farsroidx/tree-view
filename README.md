# Tree View

Tree View adds separate **Maven Project** and **Gradle Project** panes to IntelliJ IDEA. Both panes present compact source packages and grouped build files through the native asynchronous project view.

## Supported projects

- Maven standalone, multi-module, and multiple imported root projects.
- Gradle standalone and multi-module builds using Groovy or Kotlin DSL.
- Imported custom source roots, test source sets, resources, and package prefixes.
- Gradle modules with remapped physical directories.

Gradle module discovery uses IntelliJ's imported model. Import or sync the build to populate subprojects and custom source sets. Before import, existing conventional directories provide a limited fallback.

## Layout and navigation

Choose **Maven Project** or **Gradle Project** in the Project tool window selector. Available source sections always appear in this order:

```text
Source
SourceTest
Resources
ResourcesTest
```

These names describe section ordering; packages remain compact nodes. Missing sections are omitted. Native sorting applies within each section. Test labels and backgrounds follow semantic root classification.

Native PSI nodes support selection, Select In, Move/refactoring, drag and drop, VCS colors, problem presentation, and persisted expansion state. Error presentation follows background analysis without a manual save. Native IDE rules determine allowed move and drop targets.

Upgrading to 1.2.0 preserves plugin and pane identifiers. Legacy tree state is migrated when the pane opens; existing Maven ordering properties remain supported.

Root modules retain a source-focused layout. Additional submodule content excludes represented build files, outputs, internal IDE directories, and redundant source wrappers. Maven aggregators without roots are omitted from the source list; their POMs remain available.

## Build files

**Maven Poms** groups imported POMs with module labels on non-root files.

**Gradle Builds** groups existing module configuration:

- `build.gradle` and `build.gradle.kts`.
- `settings.gradle` and `settings.gradle.kts`.
- Module-local `gradle.properties`.
- Conventional `gradle/libs.versions.toml`.
- Global `gradle.properties`, labeled **Gradle User Home**.

Gradle files show their owning module. User Home resolution prefers the IDE's configured service directory, then `GRADLE_USER_HOME`, then `${user.home}/.gradle`. Files are displayed, not created. Custom catalog paths are not discovered automatically.

## Module ordering

Roots come first, followed by modules with valid `farsroidx.order` values in ascending numeric order. Unordered modules follow ordered modules. Equal priorities use case-insensitive names.

### Maven

Set the property in the module POM. Imported Maven property inheritance applies.

```xml
<properties>
    <farsroidx.order>10</farsroidx.order>
</properties>
```

### Gradle

Set a priority in each module's own `gradle.properties`; Groovy and Kotlin DSL use the same setting. Global/root properties and script assignments do not assign child-module priorities.

`domain/gradle.properties`:

```properties
farsroidx.order=10
```

`data/gradle.properties`:

```properties
farsroidx.order=20
```

`presentation/gradle.properties`:

```properties
farsroidx.order=30
```

The resulting order is root, domain, data, presentation, then unordered modules. Refresh the pane or sync the build after changing priorities.

## Missing Maven roots

Use **Sync Maven Source Roots** in the project-view context menu to register missing conventional Maven roots. It only affects Maven modules and existing `src/main` or `src/test` Java, Kotlin, Groovy, and resource directories. Existing configuration is preserved; repeated calls are safe.

For custom roots, update build configuration and reimport. Gradle root configuration remains owned by its importer.

## Development

The default build uses the local IntelliJ path in `build.gradle.kts`. To select another SDK, pass `-PintellijPlatformVersion=<version>`; uncached distributions require repository access.

```powershell
.\gradlew.bat clean test buildPlugin verifyPluginStructure
```

ZIPs are written to `build/distributions`. The `export` task also signs the plugin using configured environment variables. Publishing is a separate explicit operation.

See [architecture](docs/ARCHITECTURE.md) for implementation boundaries and [changelog](CHANGELOG.md) for release history.
