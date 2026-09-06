# FRLG Wondercard Toolkit

A Java 21 / JavaFX desktop application for FireRed and LeafGreen Wonder Card tools.

## Run

Install JDK 21 and Maven and add them to PATH. Place the compatible toolkit JAR at `lib/ramscript-tools-api-v1.jar`, then run from the repository root:

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
- Injector and Builder tabs remain placeholders.

This is still a pre-release. The app does not inject saves or modify ROMs. Toolkit gameplay support/validation is shown as reported; desktop tests do not establish hardware compatibility.

## Build and tests

```sh
mvn clean test
mvn package
```

Tests use the local toolkit JAR and synthetic temporary WC3 fixtures. JavaFX tests require a graphical environment and save a preview to `target/integration-ui.png`. Tests cover metadata, parameters, conflicts, real planning/builds, protocol errors and asynchronous UI state.

The generated app JAR is not a standalone installer and does not include Java. Launch with Maven until packaged distributions are available.

## Updating the toolkit

Close the app, replace `lib/ramscript-tools-api-v1.jar`, and rerun the tests. The app uses the Java executable from its own JDK installation, runs toolkit requests in the background, and handles missing/incompatible JARs with a visible error and reconnect action.

## Versioning

The project version is declared in `pom.xml`. Git tags such as `v0.1.0` mark releases; GitHub's pre-release flag identifies preview builds. Keep the POM and release tag aligned. Generated `target/` files and personal game files should not be committed.
