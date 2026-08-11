package padej.soup.base.util.texture;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.imageio.ImageIO;
import padej.protect.ProtIgnore;
import padej.soup.base.util.logger.LoggerUtil;

@ProtIgnore
public final class TextureObfuscator {
   private static final byte[] XOR_KEY = generateKey();

   private static byte[] generateKey() {
      try {
         String seed1 = deriveFromRuntime();
         String seed2 = deriveFromMath();
         String seed3 = deriveFromConstants();
         String combined = seed1 + seed2 + seed3;
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
         byte[] key = new byte[16];
         System.arraycopy(hash, 0, key, 0, 16);
         return key;
      } catch (NoSuchAlgorithmException var7) {
         return computeFallbackKey();
      }
   }

   private static String deriveFromRuntime() {
      char[] c1 = new char[]{'S', 'O', 'U', 'P'};
      char[] c2 = new char[]{'A', 'P', 'I'};
      String s1 = new String(c1);
      String s2 = new String(c2);
      long base = TextureObfuscator.class.getName().hashCode();
      base ^= new String(new char[]{'p', 'a', 'd', 'e', 'j', '.', 's', 'o', 'u', 'p'}).hashCode();
      base ^= (long)s1.hashCode() << 16;
      base ^= s2.hashCode();
      return Long.toHexString(base);
   }

   private static String deriveFromMath() {
      char[] m1 = new char[]{'A', 'P'};
      char[] m2 = new char[]{'I', '!'};
      char[] m3 = new char[]{'D', 'E', 'A', 'D'};
      char[] m4 = new char[]{'B', 'E', 'E', 'F'};
      String marker1 = new String(m1);
      String marker2 = new String(m2);
      String marker3 = new String(m3);
      String marker4 = new String(m4);
      long val1 = Math.abs(marker1.hashCode()) % 65536;
      long val2 = Math.abs(marker2.hashCode()) % 65536;
      long val3 = Math.abs(marker3.hashCode()) % 65536;
      long val4 = Math.abs(marker4.hashCode()) % 65536;
      long combined = val1 << 48 | val2 << 32 | val3 << 16 | val4;
      return Long.toHexString(combined);
   }

   private static String deriveFromConstants() {
      char[] h1 = new char[]{'C', 'A', 'F', 'E'};
      char[] h2 = new char[]{'B', 'A', 'B', 'E'};
      String hex1 = Integer.toHexString(new String(h1).hashCode()).substring(0, 2);
      String hex2 = Integer.toHexString(new String(h2).hashCode()).substring(0, 2);
      int c1 = hex1.charAt(0) + hex1.charAt(1) & 0xFF;
      int c2 = hex2.charAt(0) + hex2.charAt(1) & 0xFF;
      char[] s1 = new char[]{'s', 'o', 'u', 'p'};
      char[] s2 = new char[]{'a', 'p', 'i'};
      int c3 = new String(s1).hashCode() >>> 16 & 0xFF;
      int c4 = new String(s2).hashCode() >>> 8 & 0xFF;
      int result = c1 << 24 | c2 << 16 | c3 << 8 | c4;
      return Integer.toHexString(result);
   }

   private static byte[] computeFallbackKey() {
      String encodedKey = decodeObfuscatedString();

      try {
         return Base64.getDecoder().decode(encodedKey);
      } catch (Exception var2) {
         return deriveKeyFromStringOperations();
      }
   }

   private static String decodeObfuscatedString() {
      char[] part1 = decodeChars(new int[]{85, 48, 57, 86});
      char[] part2 = decodeChars(new int[]{85, 69, 70, 82});
      char[] part3 = decodeChars(new int[]{83, 83, 72, 101});
      char[] part4 = decodeChars(new int[]{114, 98, 55, 118});
      char[] part5 = decodeChars(new int[]{69, 122, 102, 75});
      char[] part6 = decodeChars(new int[]{47, 103, 61, 61});
      StringBuilder result = new StringBuilder();
      appendChars(result, part1);
      appendChars(result, part2);
      appendChars(result, part3);
      appendChars(result, part4);
      appendChars(result, part5);
      appendChars(result, part6);
      return result.toString();
   }

   private static char[] decodeChars(int[] encoded) {
      char[] result = new char[encoded.length];
      char[] tk = new char[]{'t', 'e', 'x', 't', 'u', 'r', 'e'};
      int xorKey = new String(tk).hashCode() & 0xFF;

      for (int i = 0; i < encoded.length; i++) {
         int val = encoded[i] ^ xorKey >>> i % 4;
         result[i] = (char)(val ^ xorKey >>> i % 4);
      }

      return result;
   }

   private static void appendChars(StringBuilder sb, char[] chars) {
      for (char c : chars) {
         sb.append(c);
      }
   }

   private static byte[] deriveKeyFromStringOperations() {
      char[] kc = new char[]{'S', 'O', 'U', 'P', 'A', 'P', 'I', '!'};
      String keyString = new String(kc);
      byte[] part1 = keyString.getBytes(StandardCharsets.UTF_8);
      char[] h1 = new char[]{'D', 'E', 'A', 'D', 'B', 'E', 'E', 'F'};
      byte[] part2 = hexStringToBytes(new String(h1));
      char[] h2 = new char[]{'1', '3', '3', '7', 'C', 'A', 'F', 'E'};
      byte[] part3 = hexStringToBytes(new String(h2));
      byte[] key = new byte[16];
      System.arraycopy(part1, 0, key, 0, Math.min(8, part1.length));
      System.arraycopy(part2, 0, key, 8, Math.min(4, part2.length));
      System.arraycopy(part3, 0, key, 12, Math.min(4, part3.length));
      return key;
   }

   private static byte[] hexStringToBytes(String hex) {
      int len = hex.length();
      byte[] data = new byte[len / 2];

      for (int i = 0; i < len; i += 2) {
         char c1 = hex.charAt(i);
         char c2 = hex.charAt(i + 1);
         int high = hexCharToInt(c1);
         int low = hexCharToInt(c2);
         data[i / 2] = (byte)((high << 4) + low);
      }

      return data;
   }

   private static int hexCharToInt(char c) {
      if (c >= '0' && c <= '9') {
         return c - 48;
      } else if (c >= 'A' && c <= 'F') {
         return c - 65 + 10;
      } else {
         return c >= 97 && c <= 102 ? c - 97 + 10 : 0;
      }
   }

   public static byte[] obfuscate(byte[] data) {
      byte[] result = new byte[data.length];

      for (int i = 0; i < data.length; i++) {
         result[i] = (byte)(data[i] ^ XOR_KEY[i % XOR_KEY.length]);
      }

      return result;
   }

   public static byte[] deobfuscate(byte[] data) {
      return obfuscate(data);
   }

   public static InputStream deobfuscateStream(InputStream obfuscatedStream) throws IOException {
      try {
         byte[] obfuscatedData = obfuscatedStream.readAllBytes();
         byte[] deobfuscatedData = deobfuscate(obfuscatedData);
         return new ByteArrayInputStream(deobfuscatedData);
      } catch (IOException var3) {
         LoggerUtil.error("Failed to deobfuscate texture stream: " + var3.getMessage());
         throw var3;
      }
   }

   public static boolean isObfuscatedHeader(byte[] header) {
      if (header.length < 8) {
         return false;
      } else {
         byte[] pngSignature = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10};
         boolean hasPngSignature = true;

         for (int i = 0; i < 8; i++) {
            if (header[i] != pngSignature[i]) {
               hasPngSignature = false;
               break;
            }
         }

         if (hasPngSignature) {
            return false;
         } else {
            byte[] deobfuscatedHeader = new byte[8];

            for (int ix = 0; ix < 8; ix++) {
               deobfuscatedHeader[ix] = (byte)(header[ix] ^ XOR_KEY[ix % XOR_KEY.length]);
            }

            for (int ix = 0; ix < 8; ix++) {
               if (deobfuscatedHeader[ix] != pngSignature[ix]) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public static boolean isObfuscated(byte[] data) {
      if (data.length < 8) {
         return false;
      } else {
         byte[] header = new byte[8];
         System.arraycopy(data, 0, header, 0, 8);
         return isObfuscatedHeader(header);
      }
   }

   public static byte[] imageToBytes(BufferedImage image) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "png", baos);
      return baos.toByteArray();
   }

   public static BufferedImage bytesToImage(byte[] data) throws IOException {
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      return ImageIO.read(bais);
   }

   public static String deobfuscateText(String text) {
      byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
      if (textBytes.length > 10) {
         String prefix = text.substring(0, Math.min(10, text.length()));
         if (!prefix.startsWith("#") && !prefix.startsWith("//") && !prefix.startsWith("/*")) {
            byte[] deobfuscated = deobfuscate(textBytes);
            String deobfuscatedText = new String(deobfuscated, StandardCharsets.UTF_8);
            if (deobfuscatedText.contains("#version") || deobfuscatedText.contains("uniform") || deobfuscatedText.contains("void main")) {
               return deobfuscatedText;
            }
         }
      }

      return text;
   }

   public static InputStream deobfuscateTextStream(InputStream stream) throws IOException {
      try {
         byte[] data = stream.readAllBytes();
         String text = new String(data, StandardCharsets.UTF_8);
         int nonAsciiCount = 0;

         for (int i = 0; i < Math.min(100, text.length()); i++) {
            char c = text.charAt(i);
            if ((c < ' ' || c > '~') && c != '\n' && c != '\r' && c != '\t') {
               nonAsciiCount++;
            }
         }

         if (nonAsciiCount > 30) {
            byte[] deobfuscated = deobfuscate(data);
            return new ByteArrayInputStream(deobfuscated);
         } else {
            return new ByteArrayInputStream(data);
         }
      } catch (Exception var6) {
         LoggerUtil.error("Failed to deobfuscate text stream: " + var6.getMessage());
         throw var6;
      }
   }

   private TextureObfuscator() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
