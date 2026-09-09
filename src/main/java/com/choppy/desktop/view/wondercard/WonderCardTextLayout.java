package com.choppy.desktop.view.wondercard;

import com.google.gson.JsonObject;

/** Display-only input guidance from the prepared stock font; never serializes card text. */
public final class WonderCardTextLayout {
    private final WonderCardAssets assets=new WonderCardAssets();
    public int width(String text) {
        JsonObject font=assets.font();
        return text.codePoints().map(cp -> {
            var chars=font.getAsJsonObject("characters"); String ch=new String(Character.toChars(cp));
            int id=chars.get(chars.has(ch) ? ch : "?").getAsInt();
            return font.getAsJsonArray("widths").get(id).getAsInt();
        }).sum();
    }
    public boolean supported(String text) {
        var chars=assets.font().getAsJsonObject("characters");
        return text.codePoints().allMatch(cp -> chars.has(new String(Character.toChars(cp))));
    }
    public int maxWidth(String field) {
        // Header window: 200px; subtitle's right-aligned region: 160px; body/footer: 224px.
        return field.equals("title") ? 200 : field.equals("subtitle") ? 160 : 224;
    }
    public boolean fits(String field,String text,int characterLimit) {
        return text.codePointCount(0,text.length())<=characterLimit && width(text)<=maxWidth(field);
    }
}
