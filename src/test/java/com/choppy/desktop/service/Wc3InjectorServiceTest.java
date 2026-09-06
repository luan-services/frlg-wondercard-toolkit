package com.choppy.desktop.service;

import com.choppy.desktop.model.InjectorData.Wc3Verification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class Wc3InjectorServiceTest {
    @TempDir Path temporary;
    private final Wc3InjectorService service = new Wc3InjectorService();

    @Test void handshakeUsesExpectedProtocol() { assertDoesNotThrow(service::verifyVersion); }

    @Test void verifiesWc3AndKeepsContentProblemsAsWarnings() throws Exception {
        Path wc3 = temporary.resolve("research.wc3");
        Files.write(wc3,new byte[0x58C]);
        Wc3Verification result = service.verifyWonderCard(wc3);
        assertEquals(0,result.flagId());
        assertTrue(result.warnings().stream().anyMatch(w -> w.code().equals("ZERO_FLAG_ID")));
        assertTrue(result.warnings().stream().anyMatch(w -> w.code().equals("CARD_CRC_MISMATCH")));
    }

    @Test void exposesStructuredApiErrors() {
        var error = assertThrows(Wc3InjectorService.InjectorException.class,
            () -> service.inspectSave(temporary.resolve("missing.sav")));
        assertEquals("INPUT_FILE_NOT_FOUND",error.code());
    }

    @Test void protectsInputFromBeingItsOwnOutput() throws Exception {
        Path save = Files.write(temporary.resolve("game.sav"),new byte[1]);
        assertThrows(IllegalArgumentException.class,() -> service.extract(save,save));
    }

    @Test void rejectsMalformedAndIncompatibleResponses() {
        for (String body : List.of("not json","[]","{}",
                "{\"protocol\":\"wc3-injector-json\",\"apiVersion\":2,\"ok\":true,\"result\":{}}",
                "{\"protocol\":\"wc3-injector-json\",\"apiVersion\":1.5,\"ok\":true,\"result\":{}}"))
            assertThrows(RuntimeException.class,() -> Wc3InjectorService.decodeResponse(body,0));
    }

    @Test void rejectsMissingJarAndLaunchFailure() {
        assertThrows(IllegalStateException.class,() -> new Wc3InjectorService(temporary.resolve("missing.jar")).verifyVersion());
        assertThrows(IllegalStateException.class,() -> new Wc3InjectorService(Path.of("lib/wc3-injector-api-v1.jar"),"missing-java").verifyVersion());
    }
}
