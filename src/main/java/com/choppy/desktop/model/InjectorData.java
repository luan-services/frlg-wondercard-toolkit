package com.choppy.desktop.model;

import java.nio.file.Path;
import java.util.List;

/** Typed, presentation-neutral data returned by the wc3-injector JSON API. */
public final class InjectorData {
    private InjectorData() {}

    public record Warning(String severity, String code, String message) {}
    public record Slot(int index, boolean valid, Long counter, String status) {}
    public record SaveInspection(Path input, List<Slot> slots, Slot activeSlot) {}
    public record Wc3Verification(Path input, int flagId, int iconSpecies,
            String storedCardCrc, String calculatedCardCrc, boolean cardCrcValid,
            String storedRamScriptChecksum, String calculatedRamScriptChecksum,
            boolean ramScriptChecksumValid, List<Warning> warnings) {}
    public record DistributionResult(Artifact output, String target, String baseSha1,
            String outputSha1, List<Warning> warnings) {}
    public record Artifact(Path path, long size) {}
    public record TransferResult(boolean injection, Artifact output, int slotIndex,
            long saveCounter, int physicalSector, Integer flagId, String sectorChecksum,
            Boolean cardCrcValid, Boolean ramScriptChecksumValid, List<Warning> warnings) {}
}
