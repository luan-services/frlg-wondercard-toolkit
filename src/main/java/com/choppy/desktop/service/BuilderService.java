package com.choppy.desktop.service;

import com.choppy.desktop.model.BuilderData.*;
import java.nio.file.Path;

public interface BuilderService {
    void verifyVersion();
    Catalog catalog();
    Inspection inspect(Path input);
    Saved save(Path source, Path output, Card card);
}
