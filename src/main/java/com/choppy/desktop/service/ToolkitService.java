package com.choppy.desktop.service;

import com.choppy.desktop.model.*;
import java.util.List;
import java.util.Set;

/** UI boundary. Real planning and validation belong to the toolkit integration. */
public interface ToolkitService {
    List<Preset> listPresets(Rom rom);
    Composition planComposition(Rom rom, Set<String> presetIds);
}
