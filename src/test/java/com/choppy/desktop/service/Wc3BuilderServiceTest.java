package com.choppy.desktop.service;

import com.choppy.desktop.model.BuilderData.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class Wc3BuilderServiceTest {
    @TempDir Path temporary;
    private final Wc3BuilderService service=new Wc3BuilderService();
    @Test void handshakeAndCatalogUseBackendDomainValues() {
        service.verifyVersion();
        Catalog c=service.catalog();
        assertEquals(387,c.iconSpecies().size()); assertEquals(8,c.backgrounds().size());
        Choice treecko=c.iconSpecies().stream().filter(s -> s.id().equals("TREECKO")).findFirst().orElseThrow();
        assertEquals(277,treecko.value()); assertEquals(252,treecko.nationalDex());
        assertEquals(65535,c.defaultCard().iconSpecies());
        assertEquals("SPECIAL",c.iconSpecies().getFirst().kind());
        assertEquals(20,c.receiveSlots().size());
        assertTrue(c.receiveSlots().stream().anyMatch(s -> s.value()==1003 && s.label().equals("Custom Slot 1 (1003)")));
    }
    @Test void rejectsBadProtocolAndMalformedEnvelopes() {
        for (String body : List.of("not json","[]","{}",
                "{\"protocol\":\"other\",\"apiVersion\":1,\"ok\":true}",
                "{\"protocol\":\"wc3-builder-json\",\"apiVersion\":2,\"ok\":true}",
                "{\"protocol\":\"wc3-builder-json\",\"apiVersion\":1.5,\"ok\":true}",
                "{\"protocol\":\"wc3-builder-json\",\"apiVersion\":1,\"ok\":\"true\"}"))
            assertThrows(RuntimeException.class,() -> Wc3BuilderService.decodeResponse(body,0));
        assertDoesNotThrow(() -> Wc3BuilderService.decodeResponse("{\"protocol\":\"wc3-builder-json\",\"apiVersion\":1,\"ok\":true}",0));
    }
    @Test void createsInspectsAndAtomicallyEditsTheSamePath() throws Exception {
        Card defaults=service.catalog().defaultCard(); Path file=temporary.resolve("spaces and ' quotes.wc3");
        Saved created=service.save(null,file,defaults);
        assertEquals("DEFAULT_INFORMATIONAL_PLACEHOLDER",created.ramScriptBehavior());
        assertTrue(created.validation().cardCrcValid()); assertTrue(created.validation().ramScriptChecksumValid());
        assertEquals(defaults,service.inspect(file).card());
        byte[] original=Files.readAllBytes(file);
        Card edited=withTitle(defaults,"A custom title");
        Saved saved=service.save(file,file,edited);
        assertTrue(saved.ramScriptPreserved()); assertEquals(edited,service.inspect(file).card());
        service.save(file,file,defaults);
        assertArrayEquals(original,Files.readAllBytes(file));
        try (var entries=Files.list(temporary)) { assertEquals(1,entries.count()); }
    }
    @Test void failedSaveLeavesExistingFileUntouchedAndReportsStructuredError() throws Exception {
        Card defaults=service.catalog().defaultCard(); Path file=temporary.resolve("keep.wc3");
        service.save(null,file,defaults); byte[] original=Files.readAllBytes(file);
        assertThrows(Wc3BuilderService.BuilderException.class,() -> service.save(file,file,withTitle(defaults,"X".repeat(100))));
        assertArrayEquals(original,Files.readAllBytes(file));
        var error=assertThrows(Wc3BuilderService.BuilderException.class,() -> service.inspect(temporary.resolve("missing.wc3")));
        assertEquals("INPUT_FILE_NOT_FOUND",error.code());
    }
    @Test void rawIconSurvivesInspectionAndEditing() {
        Card d=service.catalog().defaultCard();
        Card raw=new Card(d.flagId(),65000,d.idNumber(),d.type(),d.background(),d.sendType(),d.maxStamps(),d.title(),d.subtitle(),d.body(),d.footer());
        Path source=temporary.resolve("raw.wc3"),copy=temporary.resolve("copy.wc3");
        service.save(null,source,raw);
        Card inspected=service.inspect(source).card(); assertEquals(65000,inspected.iconSpecies());
        assertTrue(service.save(source,copy,inspected).ramScriptPreserved());
        assertEquals(65000,service.inspect(copy).card().iconSpecies());
    }
    @Test void editingACustomPresetPreservesItsRamScriptByteForByte() throws Exception {
        Path base=temporary.resolve("base.wc3");
        service.save(null,base,service.catalog().defaultCard());
        Path directory=Files.createDirectory(temporary.resolve("preset"));
        var request=new com.choppy.desktop.model.ToolkitData.Request(com.choppy.desktop.model.Rom.LEAF_GREEN_10,List.of("repel"),Map.of());
        var built=new RamscriptToolkitService().build(request,base,directory.resolve("custom"));
        assertTrue(built.success());
        Path custom=Path.of(built.artifacts().getFirst().path());
        byte[] original=Files.readAllBytes(custom);
        assertFalse(Arrays.equals(original,Files.readAllBytes(base)));
        Card before=service.inspect(custom).card();
        assertTrue(service.save(custom,custom,withTitle(before,"Edited preset")).ramScriptPreserved());
        assertTrue(service.inspect(custom).validation().ramScriptChecksumValid());
        // Restoring only the card fields must recover the complete custom artifact, including its script.
        service.save(custom,custom,before);
        assertArrayEquals(original,Files.readAllBytes(custom));
    }
    @Test void missingJarAndLaunchFailuresAreRecoverable() {
        assertThrows(IllegalStateException.class,() -> new Wc3BuilderService(temporary.resolve("missing.jar")).verifyVersion());
        assertThrows(IllegalStateException.class,() -> new Wc3BuilderService(Path.of("lib/wc3-builder-api-v1.jar"),"missing-java").verifyVersion());
    }
    private static Card withTitle(Card c,String title) {
        return new Card(c.flagId(),c.iconSpecies(),c.idNumber(),c.type(),c.background(),c.sendType(),c.maxStamps(),title,c.subtitle(),c.body(),c.footer());
    }
}
