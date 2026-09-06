package com.choppy.desktop.service;

import com.choppy.desktop.model.*;
import java.util.List;
import java.util.Set;

/** UI boundary. Real planning and validation belong to the toolkit integration. */
public interface ToolkitService {
    default void verifyVersion() {}
    default com.choppy.desktop.model.ToolkitData.Plan plan(com.choppy.desktop.model.ToolkitData.Request request) {
        throw new UnsupportedOperationException("Real planning is unavailable");
    }
    default com.choppy.desktop.model.ToolkitData.BuildResult build(com.choppy.desktop.model.ToolkitData.Request request,
            java.nio.file.Path input, java.nio.file.Path output) {
        throw new UnsupportedOperationException("Real generation is unavailable");
    }
    List<Preset> listPresets(Rom rom);
    default Composition planComposition(Rom rom, Set<String> presetIds) { throw new UnsupportedOperationException(); }
}
