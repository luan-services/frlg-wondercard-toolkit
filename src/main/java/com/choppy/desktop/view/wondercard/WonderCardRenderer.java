package com.choppy.desktop.view.wondercard;

import com.choppy.desktop.model.BuilderData.*;
import com.google.gson.*;
import javafx.scene.image.*;

/** Stock 240x160 layout from the pinned mystery_gift_show_card.c. See renderer doc. */
public final class WonderCardRenderer {
    private final WonderCardAssets assets;
    private final WritableImage screen=new WritableImage(240,160);
    private boolean unsupportedText;
    public WonderCardRenderer(WonderCardAssets assets) { this.assets=assets; }
    public boolean hasUnsupportedText() { return unsupportedText; }
    public Image render(Card card,Catalog catalog) {
        unsupportedText=false;
        Image background=assets.image("backgrounds/"+card.background()+".png");
        screen.getPixelWriter().setPixels(0,0,240,160,background.getPixelReader(),0,0);
        String type=catalog.cardTypes().stream().filter(c -> c.value()==card.type()).map(Choice::id).findFirst().orElse("UNKNOWN");
        text(card.title(),8,9,208,40,true);
        text(card.subtitle(),8+Math.max(0,160-width(card.subtitle())),25,208,40,true);
        if (card.idNumber()!=0) text(Long.toString(Math.min(999999,card.idNumber())),174,25,208,40,true);
        for (int i=0;i<card.body().size();i++) text(card.body().get(i),8,50+i*16,232,112,false);
        int footerY=type.equals("STAMP") ? 116 : 119;
        text(card.footer().getFirst(),8,footerY,232,152,false);
        if (type.equals("GIFT")) text(card.footer().get(1),8,footerY+16,232,152,false);
        // A bare WC3 has no player's stamp or battle/trade history. Empty stamp shadows only.
        if (type.equals("STAMP")) for (int i=0;i<Math.min(card.maxStamps(),catalog.limits().get("maxStampsMax"));i++)
            blit(assets.image("stamps/"+card.background()+".png"),200-32*i,136);
        if (card.iconSpecies()!=0) blit(assets.image("pokemon-icons/"+card.iconSpecies()+".png"),204,4);
        return screen;
    }
    private int glyph(int codepoint) {
        JsonObject chars=assets.font().getAsJsonObject("characters");
        String ch=new String(Character.toChars(codepoint));
        if (chars.has(ch)) return chars.get(ch).getAsInt();
        unsupportedText=true;
        return chars.get("?").getAsInt();
    }
    private int width(String text) { return text.codePoints().map(c -> assets.font().getAsJsonArray("widths").get(glyph(c)).getAsInt()).sum(); }
    private void text(String text,int x,int y,int right,int bottom,boolean header) {
        PixelReader mask=assets.image("fonts/normal-mask.png").getPixelReader();
        JsonArray palette=assets.font().getAsJsonArray("palette"), widths=assets.font().getAsJsonArray("widths");
        for (int cp : text.codePoints().toArray()) {
            int id=glyph(cp), advance=widths.get(id).getAsInt();
            if (id!=0) for (int dy=0;dy<14 && y+dy<bottom;dy++) for (int dx=0;dx<advance && x+dx<right;dx++) {
                int index=mask.getArgb(id%16*16+dx,id/16*16+dy)&255;
                if (index==0 || index==3) continue;
                // The font image has foreground=1, shadow=2; palette 0 is transparent.
                int colorIndex=index==1 ? (header ? 1 : 2) : (header ? 2 : 3);
                JsonArray rgb=palette.get(colorIndex).getAsJsonArray();
                int argb=0xff000000 | rgb.get(0).getAsInt()<<16 | rgb.get(1).getAsInt()<<8 | rgb.get(2).getAsInt();
                if (x+dx>=0 && y+dy>=0) screen.getPixelWriter().setArgb(x+dx,y+dy,argb);
            }
            x+=advance;
            if (x>=right) break;
        }
    }
    private void blit(Image image,int x,int y) {
        PixelReader reader=image.getPixelReader();
        for (int dy=0;dy<image.getHeight();dy++) for (int dx=0;dx<image.getWidth();dx++) {
            int argb=reader.getArgb(dx,dy);
            if ((argb>>>24)!=0 && x+dx>=0 && x+dx<240 && y+dy>=0 && y+dy<160) screen.getPixelWriter().setArgb(x+dx,y+dy,argb);
        }
    }
}
