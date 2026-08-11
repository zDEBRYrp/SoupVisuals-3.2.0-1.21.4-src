package padej.soup.api.system.font.msdf;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public final class MsdfFont {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final String name;
   private final AbstractTexture texture;
   private final FontData.AtlasData atlas;
   private final FontData.MetricsData metrics;
   private final MsdfGlyph[] glyphsLow;
   private final Map<Integer, MsdfGlyph> glyphsHigh;
   private final Map<Integer, Map<Integer, Float>> kernings;

   private MsdfFont(
      String name,
      AbstractTexture texture,
      FontData.AtlasData atlas,
      FontData.MetricsData metrics,
      MsdfGlyph[] glyphsLow,
      Map<Integer, MsdfGlyph> glyphsHigh,
      Map<Integer, Map<Integer, Float>> kernings
   ) {
      this.name = name;
      this.texture = texture;
      this.atlas = atlas;
      this.metrics = metrics;
      this.glyphsLow = glyphsLow;
      this.glyphsHigh = glyphsHigh;
      this.kernings = kernings;
   }

   public String getName() {
      return this.name;
   }

   public FontData.AtlasData getAtlas() {
      return this.atlas;
   }

   public FontData.MetricsData getMetrics() {
      return this.metrics;
   }

   public int getTextureId() {
      return this.texture.getGlId();
   }

   public MsdfGlyph getGlyph(int unicode) {
      if (unicode >= 0 && unicode < this.glyphsLow.length) {
         return this.glyphsLow[unicode];
      } else {
         return this.glyphsHigh != null ? this.glyphsHigh.get(unicode) : null;
      }
   }

   public void applyGlyphs(
      Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color
   ) {
      this.texture.setFilter(true, false);
      int prevChar = -1;
      boolean skipNext = false;

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (skipNext) {
            skipNext = false;
         } else if (c == 167) {
            skipNext = true;
         } else {
            MsdfGlyph glyph = this.getGlyph(c);
            if (glyph != null) {
               Map<Integer, Float> kerning = this.kernings.get(prevChar);
               if (kerning != null) {
                  x += kerning.getOrDefault(Integer.valueOf(c), 0.0F) * size;
               }

               x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
               prevChar = c;
            }
         }
      }
   }

   public float getWidth(String text, float size) {
      int prevChar = -1;
      float width = 0.0F;
      boolean skipNext = false;

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (skipNext) {
            skipNext = false;
         } else if (c == 167) {
            skipNext = true;
         } else {
            MsdfGlyph glyph = this.getGlyph(c);
            if (glyph != null) {
               Map<Integer, Float> kerning = this.kernings.get(prevChar);
               if (kerning != null) {
                  width += kerning.getOrDefault(Integer.valueOf(c), 0.0F) * size;
               }

               width += glyph.getWidth(size);
               prevChar = c;
            }
         }
      }

      return width;
   }

   public float getKerning(int leftChar, int rightChar) {
      Map<Integer, Float> map = this.kernings.get(leftChar);
      return map == null ? 0.0F : map.getOrDefault(rightChar, 0.0F);
   }

   public static MsdfFont.Builder builder() {
      return new MsdfFont.Builder();
   }

   public static final class Builder {
      private String name = "?";
      private Identifier dataIdentifier;
      private Identifier atlasIdentifier;

      private Builder() {
      }

      public MsdfFont.Builder name(String name) {
         this.name = name;
         return this;
      }

      public MsdfFont.Builder data(String namespace, String dataFileName) {
         this.dataIdentifier = Identifier.of(namespace, "fonts/" + dataFileName + ".json");
         return this;
      }

      public MsdfFont.Builder atlas(String namespace, String atlasFileName) {
         this.atlasIdentifier = Identifier.of(namespace, "fonts/" + atlasFileName + ".png");
         return this;
      }

      public MsdfFont build() {
         if (this.dataIdentifier == null) {
            throw new IllegalStateException("MsdfFont.Builder: data() identifier not set");
         } else if (this.atlasIdentifier == null) {
            throw new IllegalStateException("MsdfFont.Builder: atlas() identifier not set");
         } else {
            FontData data = ResourceProvider.fromJsonToInstance(this.dataIdentifier, FontData.class);
            if (data == null) {
               throw new RuntimeException("Failed to read font data file: " + this.dataIdentifier);
            } else {
               TextureManager textureManager = MsdfFont.mc.getTextureManager();
               AbstractTexture texture = textureManager.getTexture(this.atlasIdentifier);
               if (texture == null || texture.getGlId() == -1) {
                  texture = new ResourceTexture(this.atlasIdentifier);
                  textureManager.registerTexture(this.atlasIdentifier, texture);
               }

               AbstractTexture finalTexture = texture;
               RenderSystem.recordRenderCall(() -> finalTexture.setFilter(true, false));
               float aWidth = data.atlas().width();
               float aHeight = data.atlas().height();
               int ARRAY_THRESHOLD = 4096;
               int maxLow = -1;

               for (FontData.GlyphData g : data.glyphs()) {
                  int cp = g.unicode();
                  if (cp >= 0 && cp < 4096 && cp > maxLow) {
                     maxLow = cp;
                  }
               }

               MsdfGlyph[] glyphsLow = new MsdfGlyph[Math.max(0, maxLow + 1)];
               Map<Integer, MsdfGlyph> glyphsHigh = null;

               for (FontData.GlyphData gx : data.glyphs()) {
                  MsdfGlyph mg = new MsdfGlyph(gx, aWidth, aHeight);
                  int cp = gx.unicode();
                  if (cp >= 0 && cp < 4096) {
                     glyphsLow[cp] = mg;
                  } else {
                     if (glyphsHigh == null) {
                        glyphsHigh = new HashMap<>();
                     }

                     glyphsHigh.put(cp, mg);
                  }
               }

               Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
               data.kernings().forEach(kerning -> {
                  Map<Integer, Float> map = kernings.computeIfAbsent(kerning.leftChar(), k -> new HashMap<>());
                  map.put(kerning.rightChar(), kerning.advance());
               });
               return new MsdfFont(this.name, texture, data.atlas(), data.metrics(), glyphsLow, glyphsHigh, kernings);
            }
         }
      }
   }
}
