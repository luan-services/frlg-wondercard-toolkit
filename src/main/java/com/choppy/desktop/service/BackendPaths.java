package com.choppy.desktop.service;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Packaged backends live beside the application JAR, independent of the working directory. */
final class BackendPaths {
    private BackendPaths() {}
    static Path resolve(String name) {
        try {
            Path code = Path.of(BackendPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return resolve(name, code, Path.of(System.getProperty("user.dir")));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot locate the application directory", e);
        }
    }
    static Path resolve(String name, Path code, Path workingDirectory) {
        Path base = Files.isDirectory(code) ? workingDirectory : code.toAbsolutePath().getParent();
        return base.resolve("lib").resolve(name).toAbsolutePath().normalize();
    }
}
