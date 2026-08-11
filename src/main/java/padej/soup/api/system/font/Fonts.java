package padej.soup.api.system.font;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import padej.soup.api.system.font.msdf.MsdfFont;

public class Fonts {
   private static final String FONT_NAMESPACE = "minecraft";
   private static final Map<Fonts.Type, MsdfFont> MSDF_FONTS = new EnumMap<>(Fonts.Type.class);
   private static final Map<Fonts.FontKey, FontRenderer> fontCache = new HashMap<>();
   private static boolean initialized = false;

   public static void init() {
      if (!initialized) {
         for (Fonts.Type type : Fonts.Type.values()) {
            try {
               MsdfFont font = MsdfFont.builder().name(type.name()).data("minecraft", type.getAsset()).atlas("minecraft", type.getAsset()).build();
               MSDF_FONTS.put(type, font);
            } catch (Exception var5) {
               System.err.println("[SoupVisuals] Failed to load MSDF font '" + type.getAsset() + "': " + var5.getMessage());
            }
         }

         initialized = true;
      }
   }

   public static FontRenderer create(int size, Fonts.Type type) {
      if (!initialized) {
         init();
      }

      MsdfFont msdf = MSDF_FONTS.get(type);
      if (msdf == null) {
         msdf = MSDF_FONTS.values().stream().findFirst().orElseThrow(() -> new IllegalStateException("No MSDF fonts loaded — check assets/minecraft/fonts/"));
      }

      float pixelSize = type == Fonts.Type.ICONS ? size : size / 2.0F;
      return new FontRenderer(msdf, pixelSize);
   }

   public static FontRenderer getSize(int size) {
      return getSize(size, Fonts.Type.INTER_BOLD);
   }

   public static FontRenderer getSize(int size, Fonts.Type type) {
      return fontCache.computeIfAbsent(new Fonts.FontKey(size, type), k -> create(k.size(), k.type()));
   }

   private record FontKey(int size, Fonts.Type type) {
   }

   public static enum Type {
      SF_BOLD("sfpro_semibold"),
      INTER_DEFAULT("inter"),
      INTER_BOLD("inter_bold"),
      ICONS("soup_icons");

      private final String asset;

      public String getType() {
         return this.asset;
      }

      public String getAsset() {
         return this.asset;
      }

      private Type(final String asset) {
         this.asset = asset;
      }
   }
}
