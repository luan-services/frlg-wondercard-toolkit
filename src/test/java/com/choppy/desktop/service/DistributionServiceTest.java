package com.choppy.desktop.service;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class DistributionServiceTest {
    @TempDir Path temporary;
    private final Wc3InjectorService service = new Wc3InjectorService();

    @Test void requiresInputsAndProtectsSourceFiles() throws Exception {
        Path base = Files.write(temporary.resolve("base.gba"),new byte[1]);
        Path card = Files.write(temporary.resolve("card.wc3"),new byte[1]);
        Path output = temporary.resolve("output.gba");
        assertThrows(IllegalArgumentException.class,() -> service.buildDistribution(null,card,output));
        assertThrows(IllegalArgumentException.class,() -> service.buildDistribution(base,null,output));
        assertThrows(IllegalArgumentException.class,() -> service.buildDistribution(base,card,null));
        assertThrows(IllegalArgumentException.class,() -> service.buildDistribution(temporary.resolve("missing.gba"),card,output));
        assertThrows(IllegalArgumentException.class,() -> service.buildDistribution(base,temporary.resolve("missing.wc3"),output));
        assertThrows(IllegalArgumentException.class,() -> service.buildDistribution(base,card,base));
        assertThrows(IllegalArgumentException.class,() -> service.buildDistribution(base,card,temporary.resolve("output.sav")));
        assertArrayEquals(new byte[1],Files.readAllBytes(base));
    }

    @Test void backendRejectsInvalidCardAndWrongBaseWithoutCreatingOutput() throws Exception {
        Path base = Files.write(temporary.resolve("wrong.gba"),new byte[1]);
        Path card = Files.write(temporary.resolve("card.wc3"),new byte[1]);
        Path output = temporary.resolve("output.gba");
        var invalidCard = assertThrows(Wc3InjectorService.InjectorException.class,
            () -> service.buildDistribution(base,card,output));
        assertEquals("INVALID_WC3",invalidCard.code());
        Files.write(card,new byte[1420]);
        var wrongBase = assertThrows(Wc3InjectorService.InjectorException.class,
            () -> service.buildDistribution(base,card,output));
        assertEquals("INVALID_BASE_ROM",wrongBase.code());
        assertTrue(wrongBase.getMessage().contains("build-distribution"));
        assertFalse(Files.exists(output));
    }

    @Test void preservesResultPathsHashesAndWarnings() {
        var result = Wc3InjectorService.distributionResult(JsonParser.parseString("""
            {"output":{"path":"event-distribution.gba","sizeBytes":100},
             "target":"FRLG_WESTERN","baseSha1":"base-hash","outputSha1":"output-hash",
             "warnings":[{"severity":"WARNING","code":"EXAMPLE","message":"Keep this warning"}]}
            """).getAsJsonObject());
        assertEquals(Path.of("event-distribution.gba"),result.output().path());
        assertEquals("output-hash",result.outputSha1());
        assertEquals("FRLG_WESTERN",result.target());
        assertEquals("Keep this warning",result.warnings().getFirst().message());
    }
}
