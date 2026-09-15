# FRLG Wondercard Toolkit

A Java 21 / JavaFX desktop application for FireRed and LeafGreen Wonder Card tools.

## Run from source

Install JDK 21 and Maven and add them to PATH. Place the compatible API JARs at `lib/ramscript-tools-api-v1.jar`, `lib/wc3-injector-api-v1.jar`, and `lib/wc3-builder-api-v1.jar`, then run from the repository root:

```sh
mvn javafx:run
```

The first Maven run needs internet to download JavaFX, Gson and build dependencies. No separate JavaFX installation is needed. The toolkit uses JSON protocol v1; the JAR must support generic `--param` options and structured resource diagnostics.

## Current features

- Preset Composition uses the real toolkit catalog, parameters, validation and planner.
- Choose a ROM, WC3 input and hotkey presets; required parameter fields appear from toolkit metadata.
- Review actual memory usage, effective bindings/deployment and diagnostics.
- Generate creates WC3s via the toolkit and shows the ordered artifacts and installation instructions.
- Output selection specifies a destination/prefix. Each build creates a fresh subdirectory to preserve the base and existing outputs.
- The Transport tab inspects FR/LG saves, verifies WC3 files, injects into a selected save output, and extracts complete WC3 files.
- Injector warnings for custom/research cards remain visible and non-blocking.
- Builder opens with the backend's default card and supports New, Open and Build. Build asks where to write the `.wc3` file and remembers the last selected destination during the session, including after New or Open.
- Edit card text and metadata with catalog-driven selectors and searchable Pokémon icons. The official Mystery Gift question-mark icon is the default; Pokémon use their FR/LG internal species IDs.
- The live Builder preview uses bundled FR/LG backgrounds, icons, stamp shadows and the stock Latin font. No ROM or runtime asset download is required.
- Celio-GB and distribution-ROM transports are visible as future options.

First release is here. Save injection always targets the output selected by the user; the source save is protected from being used as its own output. The app does not modify ROMs. Toolkit gameplay support/validation is shown as reported; desktop tests do not establish hardware compatibility.

## Build and tests

```sh
mvn clean test
mvn package
```

Tests use all three local API JARs and temporary fixtures. JavaFX tests require a graphical environment and save previews to `target/integration-ui.png`, `target/builder-ui.png`, `target/builder-ui-compact.png` and `target/builder-backgrounds.png`. Tests cover metadata, parameters, conflicts, real planning/builds, injector verification/errors, Builder defaults, raw icon preservation, safe saves, custom RamScript preservation, protocol errors and UI state.

## Builder workflow

Use Builder to save a base `.wc3`, then use it in Preset Composition to attach custom features, or inject it through Transport. A new card includes the builder backend's informational deliveryman script. Opening an existing card uses `inspect`; saving it uses `edit` so its existing RamScript survives. Save writes a temporary file next to the destination and atomically replaces the target only after a successful response. If the filesystem cannot replace atomically, saving reports an error and keeps the destination intact.

New, Open and window close prompt before discarding unsaved changes. Backend text validation remains authoritative. The preview clips text at the stock window boundaries and supports the stock Latin glyphs; unsupported characters are marked in the preview. Player stamps and dynamic battle/trade statistics are not reconstructed from a WC3. Preview failures do not prevent editing or saving.

## Updating the toolkit

Close the app, replace the relevant versioned JAR under `lib/`, and rerun the tests. The app uses the Java executable from its own JDK installation, runs API requests in the background, and handles missing/incompatible JARs with a visible error and reconnect action.

## Versioning

The project version is declared in `pom.xml`. The current preview line is `1.1.0`.
