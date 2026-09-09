package com.choppy.desktop.model;

import java.util.List;
import java.util.Map;

/** Values supplied by the builder API. No binary-format knowledge lives here. */
public final class BuilderData {
    private BuilderData() {}
    public record Choice(int value, String id, String label, Integer nationalDex, String kind) {
        @Override public String toString() {
            return label + (nationalDex == null ? "" : " (#%03d)".formatted(nationalDex));
        }
    }
    public record Card(int flagId, int iconSpecies, long idNumber, int type, int background,
                       int sendType, int maxStamps, String title, String subtitle,
                       List<String> body, List<String> footer) {
        public Card { body = List.copyOf(body); footer = List.copyOf(footer); }
    }
    public record Catalog(List<Choice> cardTypes, List<Choice> sendTypes, List<Choice> backgrounds,
                          List<Choice> receiveSlots, List<Choice> iconSpecies,
                          Map<String,Integer> limits, Card defaultCard, String defaultRamScriptBehavior) {}
    public record Validation(boolean cardCrcValid, boolean ramScriptChecksumValid) {}
    public record Inspection(Card card, Validation validation) {}
    public record Saved(Card card, Validation validation, boolean ramScriptPreserved, String ramScriptBehavior) {}
}
