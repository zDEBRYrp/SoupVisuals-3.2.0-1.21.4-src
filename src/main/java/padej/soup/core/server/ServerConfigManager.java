package padej.soup.core.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.core.Main;
import padej.soup.core.server.websocket.WebSocketClient;

public class ServerConfigManager {
   private static final Map<String, ServerConfigManager.ServerConfig> SERVER_CONFIGS = new ConcurrentHashMap<>();
   private static final Map<String, ServerConfigManager.CachedConfig> configCache = new ConcurrentHashMap<>();
   private static final Map<String, Boolean> fetchingStatus = new ConcurrentHashMap<>();
   private static volatile boolean initialLoadSuccessful = false;
   private static volatile boolean needsForceFetch = false;

   private static void loadServersFromBackend() {
      CompletableFuture.runAsync(() -> {
         try {
            String apiBase = ServerApi.getHttpApiUrl();
            URL url = new URL(apiBase + "/servers");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
               LoggerUtil.warn("Failed to load server configurations: HTTP " + responseCode);
               initialLoadSuccessful = false;
               needsForceFetch = true;
            } else {
               BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
               StringBuilder response = new StringBuilder();

               String line;
               while ((line = in.readLine()) != null) {
                  response.append(line);
               }

               in.close();
               JsonObject serversJson = JsonParser.parseString(response.toString()).getAsJsonObject();
               SERVER_CONFIGS.clear();
               SERVER_CONFIGS.putAll(parseServerConfigs(serversJson, apiBase));
               LoggerUtil.info("Loaded " + SERVER_CONFIGS.size() + " server configurations from backend");
               initialLoadSuccessful = true;
               needsForceFetch = false;
            }

            connection.disconnect();
         } catch (Exception var8) {
            LoggerUtil.error("Error loading server configurations: " + var8.getMessage());
            initialLoadSuccessful = false;
            needsForceFetch = true;
         }
      });
   }

   public static void reloadServersAsync() {
      loadServersFromBackend();
   }

   public static String getCurrentServerAddress() {
      MinecraftClient mc = Main.mc;
      if (mc.getCurrentServerEntry() != null) {
         ServerInfo serverInfo = mc.getCurrentServerEntry();
         String address = serverInfo.address.toLowerCase();
         int colonIndex = address.indexOf(58);
         if (colonIndex > 0) {
            address = address.substring(0, colonIndex);
         }

         return address;
      } else {
         return null;
      }
   }

   private static boolean matchesAddress(String currentAddress, String pattern) {
      if (currentAddress != null && pattern != null) {
         currentAddress = currentAddress.toLowerCase();
         pattern = pattern.toLowerCase();
         if (currentAddress.equals(pattern)) {
            return true;
         } else if (pattern.contains("*")) {
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            if (!regex.startsWith(".*")) {
               regex = "^" + regex;
            }

            if (!regex.endsWith(".*")) {
               regex = regex + "$";
            }

            try {
               return currentAddress.matches(regex);
            } catch (Exception var4) {
               LoggerUtil.warn("Invalid wildcard pattern: " + pattern + " -> " + var4.getMessage());
               return false;
            }
         } else {
            return currentAddress.contains(pattern);
         }
      } else {
         return false;
      }
   }

   public static boolean isOnManagedServer() {
      String address = getCurrentServerAddress();
      if (address == null) {
         return false;
      } else {
         for (String serverKey : SERVER_CONFIGS.keySet()) {
            if (matchesAddress(address, serverKey)) {
               return true;
            }
         }

         return false;
      }
   }

   private static ServerConfigManager.ServerConfig getCurrentServerConfig() {
      String address = getCurrentServerAddress();
      if (address == null) {
         return null;
      } else {
         for (Entry<String, ServerConfigManager.ServerConfig> entry : SERVER_CONFIGS.entrySet()) {
            if (matchesAddress(address, entry.getKey())) {
               return entry.getValue();
            }
         }

         return null;
      }
   }

   private static void fetchConfigAsync(ServerConfigManager.ServerConfig serverConfig) {
      String cacheKey = serverConfig.configEndpoint;
      if (fetchingStatus.putIfAbsent(cacheKey, true) == null) {
         CompletableFuture.runAsync(() -> {
            try {
               URL url = new URL(serverConfig.configEndpoint);
               HttpURLConnection connection = (HttpURLConnection)url.openConnection();
               connection.setRequestMethod("GET");
               connection.setConnectTimeout(3000);
               connection.setReadTimeout(3000);
               int responseCode = connection.getResponseCode();
               if (responseCode != 200) {
                  LoggerUtil.warn("Failed to fetch " + serverConfig.displayName + " config: HTTP " + responseCode);
               } else {
                  BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                  StringBuilder response = new StringBuilder();

                  String line;
                  while ((line = in.readLine()) != null) {
                     response.append(line);
                  }

                  in.close();
                  JsonObject config = JsonParser.parseString(response.toString()).getAsJsonObject();
                  configCache.put(cacheKey, new ServerConfigManager.CachedConfig(config));
                  LoggerUtil.info(serverConfig.displayName + " config fetched successfully");
               }

               connection.disconnect();
            } catch (Exception var12) {
               LoggerUtil.error("Error fetching " + serverConfig.displayName + " config: " + var12.getMessage());
            } finally {
               fetchingStatus.remove(cacheKey);
            }
         });
      }
   }

   public static boolean getConfigValue(String key, boolean defaultValue) {
      ServerConfigManager.ServerConfig serverConfig = getCurrentServerConfig();
      if (serverConfig == null) {
         return defaultValue;
      } else {
         String cacheKey = serverConfig.configEndpoint;
         ServerConfigManager.CachedConfig cached = configCache.get(cacheKey);
         if (cached == null) {
            fetchConfigAsync(serverConfig);
            return defaultValue;
         } else {
            try {
               if (cached.config.has(key)) {
                  return cached.config.get(key).getAsBoolean();
               }
            } catch (Exception var6) {
               LoggerUtil.error("Error reading config key " + key + " for " + serverConfig.displayName + ": " + var6.getMessage());
            }

            return defaultValue;
         }
      }
   }

   public static void refresh() {
      ServerConfigManager.ServerConfig serverConfig = getCurrentServerConfig();
      if (serverConfig != null) {
         configCache.remove(serverConfig.configEndpoint);
         fetchConfigAsync(serverConfig);
      }
   }

   public static void refreshAll() {
      configCache.clear();

      for (ServerConfigManager.ServerConfig config : SERVER_CONFIGS.values()) {
         fetchConfigAsync(config);
      }
   }

   public static String getCurrentServerDisplayName() {
      ServerConfigManager.ServerConfig config = getCurrentServerConfig();
      return config != null ? config.displayName : "Unknown";
   }

   public static Map<String, String> getRegisteredServers() {
      Map<String, String> servers = new HashMap<>();

      for (Entry<String, ServerConfigManager.ServerConfig> entry : SERVER_CONFIGS.entrySet()) {
         servers.put(entry.getKey(), entry.getValue().displayName);
      }

      return servers;
   }

   public static boolean getVisibilitySetting(String visibilityLevel, String property, boolean defaultValue) {
      ServerConfigManager.ServerConfig serverConfig = getCurrentServerConfig();
      if (serverConfig == null) {
         return defaultValue;
      } else {
         String cacheKey = serverConfig.configEndpoint;
         ServerConfigManager.CachedConfig cached = configCache.get(cacheKey);
         if (cached == null) {
            fetchConfigAsync(serverConfig);
            return defaultValue;
         } else {
            try {
               if (cached.config.has("visibilitySettings")) {
                  JsonObject visibilitySettings = cached.config.getAsJsonObject("visibilitySettings");
                  String key = getVisibilityKey(visibilityLevel, property);
                  if (visibilitySettings.has(key)) {
                     return visibilitySettings.get(key).getAsBoolean();
                  }
               }
            } catch (Exception var8) {
               LoggerUtil.error("Error reading visibility setting " + visibilityLevel + "." + property + ": " + var8.getMessage());
            }

            return defaultValue;
         }
      }
   }

   private static String getVisibilityKey(String visibilityLevel, String property) {
      String prefix = switch (visibilityLevel) {
         case "FULLY_VISIBLE" -> "fullyVisible";
         case "PARTIALLY_VISIBLE" -> "partiallyVisible";
         case "FULLY_INVISIBLE" -> "fullyInvisible";
         default -> throw new IllegalArgumentException("Unknown visibility level: " + visibilityLevel);
      };

      String suffix = switch (property) {
         case "canBeTargeted" -> "CanBeTargeted";
         case "showSkin" -> "ShowSkin";
         case "showName" -> "ShowName";
         case "showChinaHat" -> "ShowChinaHat";
         case "showTrails" -> "ShowTrails";
         case "showHp" -> "ShowHp";
         default -> throw new IllegalArgumentException("Unknown property: " + property);
      };
      return prefix + suffix;
   }

   public static boolean canBeTargeted(String visibilityLevel) {
      boolean defaultValue = switch (visibilityLevel) {
         case "FULLY_VISIBLE" -> true;
         case "PARTIALLY_VISIBLE" -> true;
         case "FULLY_INVISIBLE" -> false;
         default -> false;
      };
      return getVisibilitySetting(visibilityLevel, "canBeTargeted", defaultValue);
   }

   public static boolean showSkin(String visibilityLevel) {
      boolean defaultValue = switch (visibilityLevel) {
         case "FULLY_VISIBLE" -> true;
         case "PARTIALLY_VISIBLE" -> false;
         case "FULLY_INVISIBLE" -> false;
         default -> false;
      };
      return getVisibilitySetting(visibilityLevel, "showSkin", defaultValue);
   }

   public static boolean showName(String visibilityLevel) {
      boolean defaultValue = switch (visibilityLevel) {
         case "FULLY_VISIBLE" -> true;
         case "PARTIALLY_VISIBLE" -> false;
         case "FULLY_INVISIBLE" -> false;
         default -> false;
      };
      return getVisibilitySetting(visibilityLevel, "showName", defaultValue);
   }

   public static boolean showChinaHat(String visibilityLevel) {
      boolean defaultValue = switch (visibilityLevel) {
         case "FULLY_VISIBLE" -> true;
         case "PARTIALLY_VISIBLE" -> true;
         case "FULLY_INVISIBLE" -> false;
         default -> false;
      };
      return getVisibilitySetting(visibilityLevel, "showChinaHat", defaultValue);
   }

   public static boolean showTrails(String visibilityLevel) {
      boolean defaultValue = switch (visibilityLevel) {
         case "FULLY_VISIBLE" -> true;
         case "PARTIALLY_VISIBLE" -> true;
         case "FULLY_INVISIBLE" -> false;
         default -> false;
      };
      return getVisibilitySetting(visibilityLevel, "showTrails", defaultValue);
   }

   public static boolean showHp(String visibilityLevel) {
      boolean defaultValue = switch (visibilityLevel) {
         case "FULLY_VISIBLE" -> true;
         case "PARTIALLY_VISIBLE" -> false;
         case "FULLY_INVISIBLE" -> false;
         default -> false;
      };
      return getVisibilitySetting(visibilityLevel, "showHp", defaultValue);
   }

   private static boolean fetchConfigSync(ServerConfigManager.ServerConfig serverConfig, int timeoutMs) {
      if (serverConfig == null) {
         return false;
      } else {
         String cacheKey = serverConfig.configEndpoint;

         try {
            URL url = new URL(serverConfig.configEndpoint);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
               connection.disconnect();
               return false;
            } else {
               BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
               StringBuilder response = new StringBuilder();

               String line;
               while ((line = in.readLine()) != null) {
                  response.append(line);
               }

               in.close();
               JsonObject config = JsonParser.parseString(response.toString()).getAsJsonObject();
               configCache.put(cacheKey, new ServerConfigManager.CachedConfig(config));
               connection.disconnect();
               return true;
            }
         } catch (Exception var10) {
            return false;
         }
      }
   }

   public static void onServerJoin(String serverAddress) {
      if (serverAddress != null) {
         WebSocketClient.enforceMultiplayerBanOnJoin(serverAddress);
         if (needsForceFetch || !initialLoadSuccessful) {
            reloadServersSync();
         }

         ServerConfigManager.ServerConfig serverConfig = getCurrentServerConfig();
         if (serverConfig != null) {
            configCache.remove(serverConfig.configEndpoint);
            boolean success = fetchConfigSync(serverConfig, 5000);
            if (!success) {
               LoggerUtil.warn("Failed to load config for " + serverConfig.displayName + " - using restrictive defaults");
            }
         }
      }
   }

   private static void reloadServersSync() {
      try {
         String apiBase = ServerApi.getHttpApiUrl();
         URL url = new URL(apiBase + "/servers");
         HttpURLConnection connection = (HttpURLConnection)url.openConnection();
         connection.setRequestMethod("GET");
         connection.setConnectTimeout(5000);
         connection.setReadTimeout(5000);
         int responseCode = connection.getResponseCode();
         if (responseCode != 200) {
            LoggerUtil.error("Failed to reload server configurations: HTTP " + responseCode);
         } else {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();

            String line;
            while ((line = in.readLine()) != null) {
               response.append(line);
            }

            in.close();
            JsonObject serversJson = JsonParser.parseString(response.toString()).getAsJsonObject();
            SERVER_CONFIGS.clear();
            SERVER_CONFIGS.putAll(parseServerConfigs(serversJson, apiBase));
            LoggerUtil.info("Reloaded " + SERVER_CONFIGS.size() + " server configurations (sync)");
            initialLoadSuccessful = true;
            needsForceFetch = false;
         }

         connection.disconnect();
      } catch (Exception var8) {
         LoggerUtil.error("Error reloading server configurations (sync): " + var8.getMessage());
      }
   }

   private static Map<String, ServerConfigManager.ServerConfig> parseServerConfigs(JsonObject serversJson, String apiBase) {
      Map<String, ServerConfigManager.ServerConfig> parsed = new ConcurrentHashMap<>();
      if (serversJson != null && serversJson.has("servers")) {
         JsonObject servers = serversJson.getAsJsonObject("servers");

         for (String serverId : servers.keySet()) {
            JsonObject serverData = servers.getAsJsonObject(serverId);
            if (serverData != null && serverData.has("name")) {
               String displayName = serverData.get("name").getAsString();
               String endpoint = apiBase + "/server/config/public?server=" + serverId;
               if (serverData.has("addresses")) {
                  JsonArray addressArray = serverData.getAsJsonArray("addresses");

                  for (int i = 0; i < addressArray.size(); i++) {
                     String serverAddress = addressArray.get(i).getAsString();
                     parsed.put(serverAddress, new ServerConfigManager.ServerConfig(serverAddress, endpoint, displayName));
                  }
               } else if (serverData.has("address")) {
                  String serverAddress = serverData.get("address").getAsString();
                  parsed.put(serverAddress, new ServerConfigManager.ServerConfig(serverAddress, endpoint, displayName));
               }
            }
         }

         return parsed;
      } else {
         return parsed;
      }
   }

   public static boolean isInitialLoadSuccessful() {
      return initialLoadSuccessful;
   }

   public static boolean needsForceFetch() {
      return needsForceFetch;
   }

   static {
      loadServersFromBackend();
   }

   private static class CachedConfig {
      final JsonObject config;

      CachedConfig(JsonObject config) {
         this.config = config;
      }
   }

   private record ServerConfig(String serverAddress, String configEndpoint, String displayName) {
   }
}
