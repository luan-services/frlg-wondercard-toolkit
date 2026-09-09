# FR/LG Wonder Card preview assets

Primary source: [pret/pokefirered at c75f352304d529f6ba92d4f74b9cf8b5c3810788](https://github.com/pret/pokefirered/tree/c75f352304d529f6ba92d4f74b9cf8b5c3810788).
Prepared by `scripts/fetch-frlg-wondercard-assets.py`, using Python and Pillow 12.1.1.

`sources.json` records every fetched source path and its SHA-256. `pokemon-icons/manifest.json` records the exact source PNG, palette and frame for every runtime species value. All resources are bundled at build time; no runtime network or ROM dependency exists.

| Output | Source and preparation |
| --- | --- |
| `backgrounds/0.png`–`7.png` | `graphics/wonder_card/bg*.png`, `.bin` tilemaps and `.pal` palettes; assembled to 240×160 using 8×8 tiles, including map flips. Backgrounds 3–5 use bg2's tiles/map and their own palettes, as specified by `sCardGraphics`. |
| `stamps/0.png`–`7.png` | `graphics/wonder_card/stamp_shadow.png` and `stamp_shadow_0.pal`–`7.pal`; transparent index 0. |
| `pokemon-icons/<value>.png` | First 32×32 frame of the Gen III icon PNG selected by `gMonIconTable`, with `gMonIconPaletteIndices` and `graphics/pokemon/icon_palettes/icon_palette_*.pal`. Source symbols resolved through `src/data/graphics/pokemon.h` and `src/graphics.c`. Values and semantic kinds come from the local builder catalog. |
| `pokemon-icons/65535.png` | Explicit `SPECIAL`/Mystery Gift case: `graphics/pokemon/question_mark/icon.png` with icon palette 0. This is the generic stock question mark, not Unown ?. The editor retains 65535. |
| `fonts/normal-mask.png` | Color-index mask derived from `graphics/fonts/latin_normal.png`, preserving 16×16 glyph cells. Source indices 0 and 3 represent background; 1 and 2 are foreground and shadow. |
| `fonts/normal.json` | Display-only character mapping from `charmap.txt`, widths from `sFontNormalLatinGlyphWidths` in `src/text.c`, and the text palette from `graphics/text_window/stdpal_3.pal`. Never used for WC3 serialization. |

Palette channels are quantized to GBA RGB5 and expanded to 0–255 RGB for PNG display. There is no LCD color correction. Deoxys uses the first stock icon frame shared by the two versions; no animation or alternate frames are simulated.

The 0xFFFF semantic label/default comes from the builder API and the supplied handoff. The stock `MailSpeciesToIconSpecies` path normalizes out-of-range species to the generic question-mark graphic (`gMonIcon_QuestionMark`); this is a rendering fact, not an editor-model remapping.

Renderer coordinates, fonts and colors come from `src/mystery_gift_show_card.c`. The font palette load is traced through `src/mystery_gift_menu.c`, `src/text_window.c` and `src/text_window_graphics.c`. Font unpacking/spacing is checked against `tools/gbagfx/font.c`, `src/new_menu_helpers.c`, `src/menu2.c` and `src/text.c`. See `docs/WONDER_CARD_RENDERER.md` in the development checkout for layout notes and limitations.

These are upstream game graphics, not original project artwork. This provenance identifies their origin; it does not assert a new license for those assets.
