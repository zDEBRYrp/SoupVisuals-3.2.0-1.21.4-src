package padej.soup.implement.features.modules.particles;

import java.util.concurrent.ThreadLocalRandom;

public class ParticleData {
   public static final String[] GLYPH_TEXTURES = new String[]{
      "abs.png", "arrow.png", "arrow_line.png", "circle.png", "cross.png", "inf.png", "line.png", "quad.png", "star.png", "triangle.png", "zigzag.png"
   };
   public static final String[] GLYPH_ALT_TEXTURES = new String[]{
      "abs.png",
      "arrow.png",
      "arrow_line.png",
      "blink_outline.png",
      "circle.png",
      "cross.png",
      "flower.png",
      "inf.png",
      "line.png",
      "quad.png",
      "soupapi.png",
      "soupapi_2.png",
      "triangle.png",
      "zigzag.png"
   };

   public static String getRandomGlyphTexture() {
      return GLYPH_TEXTURES[ThreadLocalRandom.current().nextInt(GLYPH_TEXTURES.length)];
   }

   public static String getRandomGlyphAltTexture() {
      return GLYPH_ALT_TEXTURES[ThreadLocalRandom.current().nextInt(GLYPH_ALT_TEXTURES.length)];
   }

   private ParticleData() {
   }
}
