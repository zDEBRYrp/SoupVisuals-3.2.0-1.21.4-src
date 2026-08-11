package padej.soup.core.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.MinecraftClient;
import padej.soup.api.file.SafeFileUtil;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.core.Main;

public final class ServerEndpoints {
   private static final String CONFIG_FILE_NAME = "server_endpoints.json";
   private static final String HTTP_API_KEY = "httpApiUrl";
   private static final String REDIRECT_KEY = "redirectUrl";
   private static final String BAN_CHECK_TOKEN_KEY = "banCheckToken";
   private static final String HTTP_API_PROP = "soupapi.httpApiUrl";
   private static final String REDIRECT_PROP = "soupapi.redirectUrl";
   private static final String BAN_CHECK_TOKEN_PROP = "soupapi.banCheckToken";
   private static final String HTTP_API_ENV = "SOUPAPI_HTTP_API_URL";
   private static final String REDIRECT_ENV = "SOUPAPI_REDIRECT_URL";
   private static final String BAN_CHECK_TOKEN_ENV = "SOUPAPI_BAN_CHECK_TOKEN";
   private static final String BAN_CHECK_TOKEN_ENV_COMPAT = "SOUP_BAN_CHECK_TOKEN";
   private static final String DEFAULT_HTTP_API_URL = "http://localhost:8080/api";
   private static final String DEFAULT_REDIRECT_URL = "http://77.110.111.251:8081/api/redirect";
   private static final String LEGACY_LOCAL_REDIRECT_URL = "http://localhost:8081/api/redirect";
   private static volatile boolean loaded = false;
   private static String httpApiUrl = "http://localhost:8080/api";
   private static String redirectUrl = "http://77.110.111.251:8081/api/redirect";
   private static String banCheckToken;
   private static volatile String runtimeHttpApiUrlOverride;

   private ServerEndpoints() {
   }

   public static String getHttpApiUrl() {
      ensureLoaded();
      String runtimeOverride = normalize(runtimeHttpApiUrlOverride);
      return runtimeOverride != null ? runtimeOverride : httpApiUrl;
   }

   public static String getRedirectUrl() {
      ensureLoaded();
      return redirectUrl;
   }

   public static String getBanCheckToken() {
      ensureLoaded();
      return banCheckToken;
   }

   public static synchronized void reload() {
      loaded = false;
      ensureLoaded();
   }

   public static synchronized void adoptHttpApiFromWsServer(String wsServerHostPort) {
      ensureLoaded();
      String normalizedServer = normalize(wsServerHostPort);
      if (normalizedServer != null) {
         String effectiveApi = getHttpApiUrl();
         if (isLocalAddressUrl(effectiveApi)) {
            String candidateApi = "http://" + normalizedServer + "/api";
            String currentOverride = normalize(runtimeHttpApiUrlOverride);
            if (!candidateApi.equalsIgnoreCase(currentOverride)) {
               runtimeHttpApiUrlOverride = candidateApi;
               LoggerUtil.info("Adopted runtime httpApiUrl from selected WS server: " + candidateApi);
            }
         }
      }
   }

   private static synchronized void ensureLoaded() {
      if (!loaded) {
         loaded = true;
         String fileHttpApi = null;
         String fileRedirect = null;
         String fileBanCheckToken = null;
         boolean shouldMigrateConfig = false;
         File configFile = getConfigFile();

         try {
            if (configFile.exists()) {
               if (!SafeFileUtil.isFileValid(configFile)) {
                  LoggerUtil.warn("Server endpoints config corrupted, attempting backup restore...");
                  if (!SafeFileUtil.restoreFromBackup(configFile)) {
                     LoggerUtil.warn("No valid backup, writing defaults");
                     writeDefaultConfig(configFile);
                  }
               }

               if (configFile.exists() && SafeFileUtil.isFileValid(configFile)) {
                  try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
                     JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                     fileHttpApi = readString(json, "httpApiUrl");
                     fileRedirect = readString(json, "redirectUrl");
                     fileBanCheckToken = readString(json, "banCheckToken");
                  }
               }
            } else {
               writeDefaultConfig(configFile);
            }
         } catch (Exception var10) {
            LoggerUtil.error("Failed to read server endpoints config: " + var10.getMessage());
         }

         if ("http://localhost:8081/api/redirect".equals(normalize(fileRedirect))) {
            fileRedirect = null;
            shouldMigrateConfig = true;
            LoggerUtil.warn("Legacy localhost redirect detected in server_endpoints.json; using primary redirect endpoint.");
         }

         httpApiUrl = resolve("soupapi.httpApiUrl", "SOUPAPI_HTTP_API_URL", fileHttpApi, "http://localhost:8080/api");
         redirectUrl = resolve("soupapi.redirectUrl", "SOUPAPI_REDIRECT_URL", fileRedirect, "http://77.110.111.251:8081/api/redirect");
         banCheckToken = resolveBanCheckToken(fileBanCheckToken);
         if (shouldMigrateConfig) {
            writeConfig(configFile, httpApiUrl, redirectUrl, banCheckToken);
         }

         LoggerUtil.info("Server endpoints loaded: httpApiUrl=" + httpApiUrl + ", redirectUrl=" + redirectUrl);
      }
   }

   private static String resolve(String propertyKey, String envKey, String fileValue, String fallback) {
      String propertyValue = normalize(System.getProperty(propertyKey));
      if (propertyValue != null) {
         return propertyValue;
      } else {
         String envValue = normalize(System.getenv(envKey));
         if (envValue != null) {
            return envValue;
         } else {
            String normalizedFileValue = normalize(fileValue);
            return normalizedFileValue != null ? normalizedFileValue : fallback;
         }
      }
   }

   private static String resolveBanCheckToken(String fileValue) {
      String propertyValue = normalize(System.getProperty("soupapi.banCheckToken"));
      if (propertyValue != null) {
         return propertyValue;
      } else {
         String envValue = normalize(System.getenv("SOUPAPI_BAN_CHECK_TOKEN"));
         if (envValue != null) {
            return envValue;
         } else {
            String compatEnvValue = normalize(System.getenv("SOUP_BAN_CHECK_TOKEN"));
            return compatEnvValue != null ? compatEnvValue : normalize(fileValue);
         }
      }
   }

   private static String readString(JsonObject json, String key) {
      if (json != null && json.has(key)) {
         try {
            return normalize(json.get(key).getAsString());
         } catch (Exception var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String normalize(String value) {
      if (value == null) {
         return null;
      } else {
         String trimmed = value.trim();
         return trimmed.isEmpty() ? null : trimmed;
      }
   }

   private static boolean isLocalAddressUrl(String url) {
      String normalized = normalize(url);
      if (normalized == null) {
         return false;
      } else {
         try {
            URI uri = URI.create(normalized);
            String host = normalize(uri.getHost());
            if (host == null) {
               String lower = normalized.toLowerCase();
               return lower.contains("localhost") || lower.contains("127.0.0.1");
            } else {
               String lowerHost = host.toLowerCase();
               return lowerHost.equals("localhost")
                  || lowerHost.equals("::1")
                  || lowerHost.equals("0:0:0:0:0:0:0:1")
                  || lowerHost.equals("127.0.0.1")
                  || lowerHost.startsWith("127.");
            }
         } catch (Exception var5) {
            String lower = normalized.toLowerCase();
            return lower.contains("localhost") || lower.contains("127.0.0.1");
         }
      }
   }

   private static File getConfigFile() {
      MinecraftClient mc = Main.mc;
      File runDir = mc != null ? mc.runDirectory : new File(".");
      File filesDir = new File(runDir, "SoupAPI/files");
      if (!filesDir.exists() && !filesDir.mkdirs()) {
         LoggerUtil.warn("Failed to create config directory: " + filesDir.getAbsolutePath());
      }

      return new File(filesDir, "server_endpoints.json");
   }

   private static void writeDefaultConfig(File configFile) {
      writeConfig(configFile, "http://localhost:8080/api", "http://77.110.111.251:8081/api/redirect", null);
   }

   private static void writeConfig(File configFile, String apiUrl, String redirectEndpoint, String token) {
      JsonObject json = new JsonObject();
      json.addProperty("httpApiUrl", apiUrl);
      json.addProperty("redirectUrl", redirectEndpoint);
      String normalizedToken = normalize(token);
      if (normalizedToken != null) {
         json.addProperty("banCheckToken", normalizedToken);
      }

      try {
         SafeFileUtil.atomicWrite(configFile, writer -> writer.write(json.toString()));
      } catch (Exception var7) {
         LoggerUtil.error("Failed to write server endpoints config: " + var7.getMessage());
      }
   }
}
