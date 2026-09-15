package com.choppy.desktop.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class BackendPathsTest {
    @TempDir Path directory;
    @Test void packagedBackendDoesNotDependOnWorkingDirectory() throws Exception {
        Path app = Files.createDirectory(directory.resolve("app with spaces"));
        Path jar = Files.createFile(app.resolve("toolkit.jar"));
        Path elsewhere = Files.createDirectory(directory.resolve("unrelated"));
        assertEquals(app.resolve("lib/wc3-builder-api-v1.jar"),
            BackendPaths.resolve("wc3-builder-api-v1.jar", jar, elsewhere));
    }
    @Test void developmentClassesKeepTheRepositoryLibConvention() throws Exception {
        Path classes = Files.createDirectories(directory.resolve("target/classes"));
        assertEquals(directory.resolve("lib/wc3-builder-api-v1.jar"),
            BackendPaths.resolve("wc3-builder-api-v1.jar", classes, directory));
    }
}
