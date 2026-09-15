package com.choppy.desktop.service;

import com.choppy.desktop.model.InjectorData.*;
import java.nio.file.Path;

/** Desktop boundary for Wonder Card save-file transport. */
public interface InjectorService {
    void verifyVersion();
    SaveInspection inspectSave(Path input);
    Wc3Verification verifyWonderCard(Path input);
    TransferResult inject(Path inputSave, Path wc3, Path output);
    default DistributionResult buildDistribution(Path baseRom, Path wc3, Path output) {
        throw new UnsupportedOperationException("Distribution ROM generation is unavailable");
    }
    TransferResult extract(Path inputSave, Path output);
}
