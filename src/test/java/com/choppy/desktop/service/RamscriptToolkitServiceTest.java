package com.choppy.desktop.service;

import com.choppy.desktop.model.*;
import com.choppy.desktop.model.ToolkitData.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RamscriptToolkitServiceTest {
    @TempDir Path temporary;
    private final RamscriptToolkitService service = new RamscriptToolkitService();
    private Request request(String... ids) { return new Request(Rom.LEAF_GREEN_10,List.of(ids),Map.of()); }
    private void diagnostic(Request request, String code) {
        Plan plan = service.plan(request);
        assertFalse(plan.valid());
        assertTrue(plan.diagnostics().stream().anyMatch(d -> d.code().equals(code)),plan.diagnostics().toString());
    }
    @Test void handshake() { assertDoesNotThrow(service::verifyVersion); }
    @Test void rejectsUnsupportedVersion() {
        assertThrows(IllegalStateException.class,() -> RamscriptToolkitService.decodeResponse("{\"apiVersion\":2}",0));
        assertThrows(IllegalStateException.class,() -> RamscriptToolkitService.decodeResponse("{\"apiVersion\":1.5}",0));
    }
    @Test void rejectsMalformedOrUnexpectedResponse() {
        for (String body : List.of("not json", "[]", "{}", "{\"apiVersion\":1} trailing"))
            assertThrows(RuntimeException.class,() -> RamscriptToolkitService.decodeResponse(body,0));
        assertThrows(IllegalStateException.class,() -> RamscriptToolkitService.decodeResponse("{\"apiVersion\":1}",1));
    }
    @Test void catalogAndParameterMetadata() {
        var catalog = service.listPresets(Rom.LEAF_GREEN_10);
        assertFalse(catalog.isEmpty());
        assertTrue(catalog.stream().noneMatch(p -> p.id().equals("trade-evolution")));
        var seed = catalog.stream().filter(p -> p.id().equals("seed-modifier")).findFirst().orElseThrow();
        assertTrue(seed.parameters().stream().anyMatch(p -> p.id().equals("seed") && p.required() && p.type().equals("HEX_U32")));
        assertTrue(catalog.stream().filter(p -> p.id().equals("seed-modifier-box14")).findFirst().orElseThrow().parameters().isEmpty());
    }
    @Test void emptySelection() { diagnostic(request(),"EMPTY_SELECTION"); }
    @Test void missingParameter() { diagnostic(request("seed-modifier"),"MISSING_PARAMETER"); }
    @Test void invalidParameter() { diagnostic(new Request(Rom.LEAF_GREEN_10,List.of("seed-modifier"),Map.of("seed","ZZZ")),"INVALID_PARAMETER"); }
    @Test void unknownParameter() { diagnostic(new Request(Rom.LEAF_GREEN_10,List.of("repel"),Map.of("unknown","1")),"UNKNOWN_PARAMETER"); }
    @Test void genericParameterWorks() { assertTrue(service.plan(new Request(Rom.LEAF_GREEN_10,List.of("seed-modifier"),Map.of("seed","B5B1E7AD"))).valid()); }
    @Test void resourceConflict() { diagnostic(request("run-anywhere","run-bike-anywhere"),"RESOURCE_CONFLICT"); }
    @Test void hotkeyConflict() { diagnostic(request("lead-ev-viewer","run-bike-anywhere"),"HOTKEY_CONFLICT"); }
    @Test void actualCapacities() {
        Plan plan = service.plan(request("seed-modifier-box14","repel","party-iv-viewer","run-bike-anywhere"));
        assertTrue(plan.valid());
        assertEquals(new Memory(878,995,117),plan.ramScript());
        assertEquals(new Memory(40,400,360),plan.sb1());
        assertEquals(new Memory(991,1024,33),plan.sb2());
        assertEquals(4,plan.hotkeys());
    }
    @Test void buildsVariableArtifactCountAndPreservesInput() throws Exception {
        Path input = temporary.resolve("base.wc3");
        byte[] fixture = new byte[0x58C];
        Files.write(input,fixture);
        Path single = Files.createDirectory(temporary.resolve("single"));
        BuildResult local = service.build(request("repel"),input,single.resolve("result.wc3"));
        assertTrue(local.success(),local.diagnostics().toString());
        assertEquals(1,local.artifacts().size());
        assertEquals("LOCAL_RUNTIME",local.artifacts().getFirst().role());
        Path multi = Files.createDirectory(temporary.resolve("multi"));
        BuildResult shared = service.build(request("seed-modifier-box14","repel","party-iv-viewer","run-bike-anywhere"),input,multi.resolve("result"));
        assertTrue(shared.success(),shared.diagnostics().toString());
        assertEquals(List.of("INSTALL_STAGE","INSTALL_STAGE","RUNTIME"),shared.artifacts().stream().map(Artifact::role).toList());
        assertFalse(shared.instructions().isEmpty());
        for (Artifact artifact : shared.artifacts()) assertEquals(artifact.size(),Files.size(Path.of(artifact.path())));
        assertArrayEquals(fixture,Files.readAllBytes(input));
        assertThrows(IllegalArgumentException.class,() -> service.build(request("repel"),input,single.resolve("result.wc3")));
    }
    @Test void missingJarAndLaunchFailure() {
        assertThrows(IllegalStateException.class,() -> new RamscriptToolkitService(temporary.resolve("missing.jar")).verifyVersion());
        assertThrows(IllegalStateException.class,() -> new RamscriptToolkitService(Path.of("lib/ramscript-tools-api-v1.jar"),"missing-java-executable").verifyVersion());
    }
}
