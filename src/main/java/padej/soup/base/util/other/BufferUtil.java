package padej.soup.base.util.other;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import padej.soup.base.util.logger.LoggerUtil;

public final class BufferUtil {
   private static final Map<String, Integer> dynamicIdCounters = Maps.newHashMap();

   public static NativeImageBackedTexture getHeadFromURL(String url) throws IOException {
      if (url != null && !url.isEmpty()) {
         try {
            NativeImage downloaded = NativeImage.read(new URL(url).openStream());
            NativeImage parsed = parseHead(downloaded);
            return parsed == null ? null : new NativeImageBackedTexture(parsed);
         } catch (Exception var3) {
            LoggerUtil.error("[BufferUtil] Error loading image: " + var3.getMessage());
            throw var3;
         }
      } else {
         System.err.println("[BufferUtil] URL is null or empty");
         return null;
      }
   }

   public static NativeImage parseHead(NativeImage image) {
      if (image == null) {
         return null;
      } else {
         int imageWidth = 22;
         int imageHeight = 22;
         int imageSrcWidth = image.getWidth();
         int srcHeight = image.getHeight();

         for (int imageSrcHeight = image.getHeight(); imageWidth < imageSrcWidth || imageHeight < imageSrcHeight; imageHeight *= 2) {
            imageWidth *= 2;
         }

         NativeImage imgNew = new NativeImage(imageWidth, imageHeight, true);

         for (int x = 0; x < imageSrcWidth; x++) {
            for (int y = 0; y < srcHeight; y++) {
               imgNew.setColorArgb(x, y, image.getColorArgb(x, y));
            }
         }

         image.close();
         return imgNew;
      }
   }

   public static Identifier registerDynamicTexture(String prefix, NativeImageBackedTexture texture) {
      if (texture == null) {
         System.err.println("[BufferUtil] Cannot register null texture with prefix: " + prefix);
         return null;
      } else {
         Integer integer = dynamicIdCounters.get(prefix);
         if (integer == null) {
            integer = 1;
         } else {
            integer = integer + 1;
         }

         dynamicIdCounters.put(prefix, integer);
         Identifier identifier = Identifier.ofVanilla(String.format(Locale.ROOT, "dynamic/%s_%d", prefix, integer));
         MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, texture);
         return identifier;
      }
   }

   private BufferUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
