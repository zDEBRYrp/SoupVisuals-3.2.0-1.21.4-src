package padej.soup.base.util.logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggerUtil {
   private static final Logger log = LogManager.getLogger("Soup");
   private static final String TEXT_COLOR = "\u001b[38;2;110;158;248m";
   private static final String TEXT_WITH_BG = "\u001b[38;2;255;255;255m";
   private static final String BG_SUCCESS = "\u001b[48;2;34;92;43m";
   private static final String BG_ERROR = "\u001b[48;2;122;41;54m";
   private static final String BG_WARNING = "\u001b[48;2;188;165;49m";
   private static final String PROTECTED_PREFIX = "[PROTECTED]";
   private static final AtomicReference<String> lastProtectedHash = new AtomicReference<>();
   private static final long INSTANCE_SEED = System.nanoTime() ^ System.currentTimeMillis();
   private static volatile boolean isInitialized = false;

   public static void info(Object message) {
      String formattedMessage = "\u001b[38;2;255;255;255m\u001b[48;2;34;92;43m" + message + "\u001b[0m";
      log.info(formattedMessage);
   }

   public static void info(Object message, Throwable exception) {
      String formattedMessage = "\u001b[38;2;255;255;255m\u001b[48;2;34;92;43m" + message + "\u001b[0m";
      log.info(formattedMessage, exception);
   }

   public static void info(String message, Object... args) {
      String formattedMessage = "\u001b[38;2;255;255;255m\u001b[48;2;34;92;43m" + message + "\u001b[0m";
      log.info(formattedMessage, args);
   }

   public static void warn(Object message) {
      String formattedMessage = "\u001b[38;2;255;255;255m\u001b[48;2;188;165;49m" + message + "\u001b[0m";
      log.warn(formattedMessage);
   }

   public static void warn(String message, Object... args) {
      String formattedMessage = "\u001b[38;2;255;255;255m\u001b[48;2;188;165;49m" + message + "\u001b[0m";
      log.warn(formattedMessage, args);
   }

   public static void error(Object message) {
      String formattedMessage = "\u001b[38;2;255;255;255m\u001b[48;2;122;41;54m" + message + "\u001b[0m";
      log.error(formattedMessage);
   }

   public static void error(Object message, Throwable exception) {
      String formattedMessage = "\u001b[38;2;255;255;255m\u001b[48;2;122;41;54m" + message + "\u001b[0m";
      log.error(formattedMessage, exception);
   }

   public static void error(String message, Object... args) {
      String formattedMessage = "\u001b[38;2;255;255;255m\u001b[48;2;122;41;54m" + message + "\u001b[0m";
      log.error(formattedMessage, args);
   }

   public static void log(Object message) {
      String formattedMessage = "\u001b[38;2;110;158;248m" + message + "\u001b[0m";
      log.info(formattedMessage);
   }

   private static String computeHash(String input) {
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
         StringBuilder hexString = new StringBuilder();

         for (byte b : hash) {
            String hex = Integer.toHexString(255 & b);
            if (hex.length() == 1) {
               hexString.append('0');
            }

            hexString.append(hex);
         }

         return hexString.toString().substring(0, 16);
      } catch (NoSuchAlgorithmException var9) {
         return String.valueOf(input.hashCode() & 2147483647);
      }
   }

   private static String generateInitialHash() {
      String systemInfo = System.getProperty("java.version", "")
         + System.getProperty("os.name", "")
         + INSTANCE_SEED
         + Runtime.getRuntime().availableProcessors();
      return computeHash(systemInfo);
   }

   private static synchronized void initializeProtectedLogging() {
      if (!isInitialized) {
         lastProtectedHash.set(generateInitialHash());
         isInitialized = true;
      }
   }

   public static void protectedInfo(Object message) {
      initializeProtectedLogging();
      String messageStr = String.valueOf(message);
      String currentHash = lastProtectedHash.get();
      long timestamp = System.currentTimeMillis();
      String newHash = computeHash(currentHash + messageStr + timestamp);
      String protectedMessage = String.format("%s %s [HASH:%s][PREV:%s][TIME:%d]", "[PROTECTED]", messageStr, newHash, currentHash, timestamp);
      String formattedMessage = "\u001b[38;2;43;43;43m\u001b[48;2;255;138;76m" + protectedMessage + "\u001b[0m";
      lastProtectedHash.set(newHash);
      log.info(formattedMessage);
   }

   public static void protectedWarn(Object message) {
      initializeProtectedLogging();
      String messageStr = String.valueOf(message);
      String currentHash = lastProtectedHash.get();
      long timestamp = System.currentTimeMillis();
      String newHash = computeHash(currentHash + messageStr + timestamp);
      String protectedMessage = String.format("%s %s [HASH:%s][PREV:%s][TIME:%d]", "[PROTECTED]", messageStr, newHash, currentHash, timestamp);
      String formattedMessage = "\u001b[38;2;43;43;43m\u001b[48;2;255;138;76m" + protectedMessage + "\u001b[0m";
      lastProtectedHash.set(newHash);
      log.warn(formattedMessage);
   }

   public static void protectedError(Object message) {
      initializeProtectedLogging();
      String messageStr = String.valueOf(message);
      String currentHash = lastProtectedHash.get();
      long timestamp = System.currentTimeMillis();
      String newHash = computeHash(currentHash + messageStr + timestamp);
      String protectedMessage = String.format("%s %s [HASH:%s][PREV:%s][TIME:%d]", "[PROTECTED]", messageStr, newHash, currentHash, timestamp);
      String formattedMessage = "\u001b[38;2;43;43;43m\u001b[48;2;255;138;76m" + protectedMessage + "\u001b[0m";
      lastProtectedHash.set(newHash);
      log.error(formattedMessage);
   }
}
