package padej.soup.core.server.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.core.Main;
import padej.soup.core.server.ServerApi;
import padej.soup.core.server.ServerConfigManager;
import padej.soup.core.server.ServerEndpoints;

public class WebSocketClient implements Listener {
   private static final int MAX_RECONNECT_ATTEMPTS = 5;
   private static final int RECONNECT_DELAY_MINUTES = 5;
   private static final String BAN_CHECK_TOKEN_HEADER = "x-soup-ban-check-token";
   private volatile WebSocket webSocket;
   private final HttpClient httpClient;
   private final Gson gson;
   private final PlayerStore playerStore;
   private final ServerRedirect serverRedirect;
   private final ScheduledExecutorService executor;
   private final String clientHwid;
   private final Object frameLock = new Object();
   private final StringBuilder textFrameBuffer = new StringBuilder();
   private final ByteArrayOutputStream binaryFrameBuffer = new ByteArrayOutputStream(1024);
   private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
   private volatile boolean running = false;
   private volatile String currentUsername;
   private volatile String currentAccessToken;
   private volatile String currentServerUrl;
   private volatile ScheduledFuture<?> reconnectTask;
   private volatile boolean officialAccount = false;
   private static volatile WebSocketClient.ActiveBan activeBan;
   private static final ConcurrentHashMap<String, Boolean> OFFICIAL_CACHE = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<String, Boolean> WARN_ONCE = new ConcurrentHashMap<>();

   public WebSocketClient(PlayerStore playerStore) {
      this.playerStore = playerStore;
      this.httpClient = HttpClient.newHttpClient();
      this.gson = new Gson();
      this.serverRedirect = new ServerRedirect();
      this.executor = Executors.newScheduledThreadPool(1);
      this.clientHwid = UUID.nameUUIDFromBytes(this.buildHwidSeed().getBytes(StandardCharsets.UTF_8)).toString();
   }

   public void connect() {
      if (this.running) {
         LoggerUtil.warn("WebSocket client is already running");
      } else {
         MinecraftClient mc = Main.mc;
         if (mc.getSession() != null && mc.getSession().getUsername() != null) {
            this.currentUsername = mc.getSession().getUsername();
            this.currentAccessToken = mc.getSession().getAccessToken();
            this.running = true;
            this.reconnectAttempts.set(0);
            this.clearScheduledReconnect();
            this.clearFrameBuffers();
            this.fetchServerAndConnect();
         } else {
            LoggerUtil.error("Cannot connect WebSocket: no valid session");
         }
      }
   }

   public void disconnect() {
      this.running = false;
      this.clearScheduledReconnect();
      if (this.webSocket != null) {
         try {
            this.webSocket.sendClose(1000, "Client disconnect");
         } catch (Exception var5) {
         } finally {
            this.webSocket = null;
         }
      }

      this.playerStore.clear();
      this.executor.shutdownNow();
      LoggerUtil.info("WebSocket client disconnected");
   }

   public boolean isConnected() {
      return this.webSocket != null && !this.webSocket.isInputClosed() && !this.webSocket.isOutputClosed();
   }

   @Override
   public void onOpen(WebSocket webSocket) {
      LoggerUtil.info("WebSocket opened");
      this.reconnectAttempts.set(0);
      this.clearScheduledReconnect();
      webSocket.request(1L);
   }

   @Override
   public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      String payload = null;
      synchronized (this.frameLock) {
         this.textFrameBuffer.append(data);
         if (last) {
            payload = this.textFrameBuffer.toString();
            this.textFrameBuffer.setLength(0);
         }
      }

      if (payload != null) {
         this.handleMessage(payload);
      }

      webSocket.request(1L);
      return CompletableFuture.completedFuture(null);
   }

   @Override
   public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
      String payload = null;
      byte[] chunk = new byte[data.remaining()];
      data.get(chunk);
      synchronized (this.frameLock) {
         this.binaryFrameBuffer.write(chunk, 0, chunk.length);
         if (last) {
            payload = new String(this.binaryFrameBuffer.toByteArray(), StandardCharsets.UTF_8);
            this.binaryFrameBuffer.reset();
         }
      }

      if (payload != null) {
         this.handleMessage(payload);
      }

      webSocket.request(1L);
      return CompletableFuture.completedFuture(null);
   }

   @Override
   public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      LoggerUtil.warn("WebSocket closed: " + statusCode + " - " + reason);
      this.playerStore.clear();
      this.clearFrameBuffers();
      this.applyCloseReasonFallback(reason);
      if (this.running) {
         this.scheduleReconnect("websocket closed: " + statusCode + " - " + reason);
      }

      return CompletableFuture.completedFuture(null);
   }

   @Override
   public void onError(WebSocket webSocket, Throwable error) {
      LoggerUtil.error("WebSocket error: " + error.getMessage());
      if (this.running) {
         this.scheduleReconnect("websocket error: " + error.getMessage());
      }
   }

   public static void enforceMultiplayerBanOnJoin(String targetServerAddress) {
      WebSocketClient.ActiveBan ban = activeBan;
      if (ban != null && ban.level() == WebSocketClient.ActionLevel.MULTIPLAYER) {
         if (isBanExpired(ban.expiresAtEpochSeconds())) {
            clearActiveBan();
         } else {
            String serverAddress = normalizeServerAddress(targetServerAddress);
            if (serverAddress == null) {
               serverAddress = normalizeServerAddress(resolveCurrentServerAddress());
            }

            if (!isLocalOnlyServerAddress(serverAddress)) {
               String reason = ban.reason() == null ? "Multiplayer access is blocked by server ban." : ban.reason();
               String reasonWithTime = composeBanMessage(reason, ban.expiresAtEpochSeconds());
               String remaining = formatRemainingTime(ban.expiresAtEpochSeconds());
               MinecraftClient mc = Main.mc;
               if (mc != null) {
                  mc.execute(() -> {
                     try {
                        if (mc.getNetworkHandler() == null || mc.getNetworkHandler().getConnection() == null) {
                           return;
                        }

                        mc.getNetworkHandler().getConnection().disconnect(Text.literal(reasonWithTime));
                        LoggerUtil.warn("Disconnected from Minecraft server by active multiplayer ban. remaining=" + remaining);
                     } catch (Exception var4x) {
                        LoggerUtil.error("Failed to enforce multiplayer ban on server join: " + var4x.getMessage());
                     }
                  });
               }
            }
         }
      }
   }

   @Deprecated
   public static void enforceMcServerBanOnJoin() {
      enforceMultiplayerBanOnJoin(null);
   }

   private void fetchServerAndConnect() {
      if (this.running) {
         this.executor.submit(() -> {
            try {
               String server = this.serverRedirect.fetchServer();
               if (server == null) {
                  LoggerUtil.error("No servers available from redirect");
                  this.scheduleReconnect("redirect returned no available servers");
                  return;
               }

               ServerEndpoints.adoptHttpApiFromWsServer(server);
               ServerConfigManager.reloadServersAsync();
               WebSocketClient.BanCheckResult banCheck = this.checkClientBanStatus();
               if (banCheck.banned()) {
                  WebSocketClient.ActionLevel level = banCheck.level();
                  String reason = this.normalizeReason(banCheck.reason());
                  Long expiresAt = banCheck.expiresAtEpochSeconds();
                  String remaining = formatRemainingTime(expiresAt);
                  setActiveBan(level, reason, expiresAt);
                  if (level == WebSocketClient.ActionLevel.CLIENT) {
                     LoggerUtil.error("Client HWID is banned (client): " + reason + " | remaining=" + remaining);
                     this.applyServerAction("banned", WebSocketClient.ActionLevel.CLIENT, composeBanMessage(reason, expiresAt));
                     return;
                  }

                  if (level == WebSocketClient.ActionLevel.MULTIPLAYER) {
                     LoggerUtil.warn("Active multiplayer ban detected: " + reason + " | remaining=" + remaining);
                     enforceMultiplayerBanOnJoin(null);
                  }
               } else {
                  clearActiveBan();
               }

               this.officialAccount = this.resolveOfficialStatus(this.currentUsername, this.currentAccessToken);
               this.currentServerUrl = "ws://" + server + "/ws";
               LoggerUtil.info("Selected server: " + this.currentServerUrl);
               this.doConnect();
            } catch (Exception var7) {
               LoggerUtil.error("Error fetching server: " + var7.getMessage());
               this.scheduleReconnect("redirect fetch failed: " + var7.getMessage());
            }
         });
      }
   }

   private void doConnect() {
      if (this.running && this.currentServerUrl != null) {
         try {
            LoggerUtil.info("Connecting to WebSocket server...");
            CompletableFuture<WebSocket> wsFuture = this.httpClient.newWebSocketBuilder().buildAsync(URI.create(this.currentServerUrl), this);
            wsFuture.thenAccept(ws -> {
               this.webSocket = ws;
               LoggerUtil.info("WebSocket connected");
               this.sendJoinMessage();
            }).exceptionally(ex -> {
               LoggerUtil.error("Failed to connect WebSocket: " + ex.getMessage());
               this.scheduleReconnect("websocket connect failed: " + ex.getMessage());
               return null;
            });
         } catch (Exception var2) {
            LoggerUtil.error("Error connecting WebSocket: " + var2.getMessage());
            this.scheduleReconnect("connection error: " + var2.getMessage());
         }
      }
   }

   private void sendJoinMessage() {
      if (this.webSocket != null && this.currentUsername != null) {
         JsonObject joinMsg = new JsonObject();
         joinMsg.addProperty("type", "JOIN");
         joinMsg.addProperty("name", this.currentUsername);
         joinMsg.addProperty("official", this.officialAccount);
         joinMsg.addProperty("hwid", this.clientHwid);
         this.webSocket.sendText(this.gson.toJson(joinMsg), true);
         LoggerUtil.info("Sent JOIN message for: " + this.currentUsername + " official=" + this.officialAccount);
      }
   }

   private void handleMessage(String message) {
      try {
         JsonObject json = (JsonObject)this.gson.fromJson(message, JsonObject.class);
         if (json == null || !json.has("type")) {
            LoggerUtil.warn("Ignoring invalid WebSocket payload without type.");
            return;
         }

         String type = json.get("type").getAsString();
         switch (type) {
            case "SNAPSHOT":
               this.handleSnapshot(json);
               break;
            case "PLAYER_JOINED":
               this.handlePlayerJoined(json);
               break;
            case "PLAYER_UPDATED":
               this.handlePlayerUpdated(json);
               break;
            case "PLAYER_LEFT":
               this.handlePlayerLeft(json);
               break;
            case "KICKED":
               this.handleKicked(json);
               break;
            case "BANNED":
               this.handleBanned(json);
               break;
            default:
               LoggerUtil.warn("Unknown WebSocket event type: " + type);
         }
      } catch (Exception var6) {
         LoggerUtil.error("Error parsing WebSocket message: " + var6.getMessage());
      }
   }

   private void handleSnapshot(JsonObject json) {
      this.playerStore.clear();
      if (json.has("players")) {
         json.getAsJsonArray("players").forEach(element -> {
            PlayerInfo player = this.parsePlayer(element.getAsJsonObject());
            if (player != null) {
               this.playerStore.addPlayer(player);
            }
         });
      }
   }

   private void handlePlayerJoined(JsonObject json) {
      if (json.has("player")) {
         PlayerInfo player = this.parsePlayer(json.getAsJsonObject("player"));
         if (player != null) {
            this.playerStore.addPlayer(player);
         }
      }
   }

   private void handlePlayerUpdated(JsonObject json) {
      if (json.has("player")) {
         PlayerInfo player = this.parsePlayer(json.getAsJsonObject("player"));
         if (player != null) {
            this.playerStore.addPlayer(player);
         }
      }
   }

   private void handlePlayerLeft(JsonObject json) {
      Integer playerId = this.parsePlayerId(json);
      if (playerId == null) {
         LoggerUtil.warn("PLAYER_LEFT event missing id field: " + json);
      } else {
         this.playerStore.removePlayer(playerId);
      }
   }

   private void handleKicked(JsonObject json) {
      String reason = this.getString(json, "reason", "Kicked by server");
      WebSocketClient.ActionLevel level = WebSocketClient.ActionLevel.fromWire(this.getString(json, "level", "socket"));
      LoggerUtil.warn("Disconnected by server (KICKED, level=" + level.asWire() + "): " + reason);
      this.applyServerAction("kicked", level, reason);
   }

   private void handleBanned(JsonObject json) {
      String reason = this.normalizeReason(this.getString(json, "reason", "Banned by server"));
      WebSocketClient.ActionLevel level = WebSocketClient.ActionLevel.fromWire(this.getString(json, "level", "socket"));
      Long expiresAt = this.getLong(json, "expiresAt", this.getLong(json, "expires_at", null));
      String remaining = formatRemainingTime(expiresAt);
      setActiveBan(level, reason, expiresAt);
      LoggerUtil.error("Disconnected by server (BANNED, level=" + level.asWire() + "): " + reason + " | remaining=" + remaining);
      this.applyServerAction("banned", level, composeBanMessage(reason, expiresAt));
   }

   private PlayerInfo parsePlayer(JsonObject json) {
      try {
         Integer id = this.parsePlayerId(json);
         if (id == null) {
            LoggerUtil.warn("Player payload missing id field: " + json);
            return null;
         } else if (json.has("name") && !json.get("name").isJsonNull()) {
            String name = json.get("name").getAsString();
            if (name != null && !name.isBlank()) {
               String role = this.getString(json, "role", "user");
               boolean official = json.has("official") && json.get("official").getAsBoolean();
               return new PlayerInfo(id, name, role, official);
            } else {
               LoggerUtil.warn("Player payload has blank name field: " + json);
               return null;
            }
         } else {
            LoggerUtil.warn("Player payload missing name field: " + json);
            return null;
         }
      } catch (Exception var6) {
         LoggerUtil.error("Error parsing player payload: " + var6.getMessage());
         return null;
      }
   }

   private Integer parsePlayerId(JsonObject json) {
      if (json.has("id") && !json.get("id").isJsonNull()) {
         try {
            return json.get("id").getAsInt();
         } catch (Exception var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   private boolean resolveOfficialStatus(String username, String accessToken) {
      if (username != null && !username.isBlank()) {
         String normalizedName = username.trim().toLowerCase(Locale.ROOT);
         Boolean cached = OFFICIAL_CACHE.get(normalizedName);
         if (cached != null) {
            return cached;
         } else {
            Boolean resolved = this.queryOfficialStatusWithAccessToken(accessToken);
            if (resolved != null) {
               OFFICIAL_CACHE.put(normalizedName, resolved);
               return resolved;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private Boolean queryOfficialStatusWithAccessToken(String accessToken) {
      if (accessToken != null && !accessToken.isBlank()) {
         HttpURLConnection connection = null;

         Object var5;
         try {
            URL url = new URL("https://api.minecraftservices.com/minecraft/profile");
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestProperty("Authorization", "Bearer " + accessToken.trim());
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
               return true;
            }

            if (responseCode == 401 || responseCode == 403 || responseCode == 404) {
               return false;
            }

            LoggerUtil.warn("Minecraft official check failed: HTTP " + responseCode);
            var5 = null;
         } catch (Exception var9) {
            LoggerUtil.warn("Minecraft official check failed: " + var9.getMessage());
            return null;
         } finally {
            if (connection != null) {
               connection.disconnect();
            }
         }

         return (Boolean)var5;
      } else {
         return false;
      }
   }

   private WebSocketClient.BanCheckResult checkClientBanStatus() {
      HttpURLConnection connection = null;

      WebSocketClient.BanCheckResult response;
      try {
         String apiBase = ServerApi.getHttpApiUrl();
         String encodedHwid = URLEncoder.encode(this.clientHwid, StandardCharsets.UTF_8);
         URL url = new URL(apiBase + "/client/ban/check?hwid=" + encodedHwid);
         connection = (HttpURLConnection)url.openConnection();
         connection.setRequestMethod("GET");
         connection.setConnectTimeout(5000);
         connection.setReadTimeout(5000);
         String token = ServerEndpoints.getBanCheckToken();
         if (token != null) {
            connection.setRequestProperty("x-soup-ban-check-token", token);
         }

         int responseCode = connection.getResponseCode();
         if (responseCode == 200) {
            StringBuilder responsex = new StringBuilder();

            String line;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
               while ((line = in.readLine()) != null) {
                  responsex.append(line);
               }
            }

            JsonObject json = (JsonObject)this.gson.fromJson(responsex.toString(), JsonObject.class);
            boolean banned = json != null && json.has("banned") && json.get("banned").getAsBoolean();
            WebSocketClient.ActionLevel level = WebSocketClient.ActionLevel.fromWire(this.getString(json, "level", "socket"));
            String reason = this.getString(json, "reason", null);
            Long expiresAt = this.getLong(json, "expiresAt", this.getLong(json, "expires_at", null));
            return new WebSocketClient.BanCheckResult(banned, level, reason, expiresAt);
         }

         if (responseCode == 404) {
            warnOnce("ban-check-http-404", "Ban check endpoint is unavailable (HTTP 404). Configure SOUP_BAN_CHECK_TOKEN on server and client.");
         } else if (responseCode != 401 && responseCode != 403) {
            LoggerUtil.warn("Ban check request failed: HTTP " + responseCode);
         } else {
            warnOnce("ban-check-http-auth", "Ban check request is unauthorized. Verify ban-check token in server_endpoints.json (banCheckToken).");
         }

         response = new WebSocketClient.BanCheckResult(false, WebSocketClient.ActionLevel.SOCKET, null, null);
      } catch (Exception var21) {
         LoggerUtil.warn("Ban check request failed: " + var21.getMessage());
         return new WebSocketClient.BanCheckResult(false, WebSocketClient.ActionLevel.SOCKET, null, null);
      } finally {
         if (connection != null) {
            connection.disconnect();
         }
      }

      return response;
   }

   private void applyServerAction(String action, WebSocketClient.ActionLevel level, String reason) {
      MinecraftClient mc = Main.mc;
      switch (level) {
         case SOCKET:
            this.disconnectByServerAction(action + ":" + level.asWire());
            break;
         case MULTIPLAYER:
            if (mc != null) {
               this.disconnectFromMinecraftServer(mc, action, reason);
            }
            break;
         case CLIENT:
            this.disconnectByServerAction(action + ":" + level.asWire());
            if (mc != null) {
               this.closeMinecraftClient(mc, action);
            }
      }
   }

   private void disconnectFromMinecraftServer(MinecraftClient mc, String action, String reason) {
      mc.execute(() -> {
         try {
            if (mc.getNetworkHandler() == null || mc.getNetworkHandler().getConnection() == null) {
               LoggerUtil.warn("No active Minecraft server connection to close (" + action + ").");
               return;
            }

            String kickReason = reason != null && !reason.isBlank() ? reason : "Disconnected by server";
            mc.getNetworkHandler().getConnection().disconnect(Text.literal(kickReason));
            LoggerUtil.warn("Minecraft server connection closed by server action (" + action + "): " + kickReason);
         } catch (Exception var4) {
            LoggerUtil.error("Failed to disconnect from Minecraft server (" + action + "): " + var4.getMessage());
         }
      });
   }

   private void closeMinecraftClient(MinecraftClient mc, String action) {
      mc.execute(() -> {
         try {
            mc.scheduleStop();
            LoggerUtil.error("Client shutdown requested by server action (" + action + ").");
         } catch (Exception var3) {
            LoggerUtil.error("Failed to close Minecraft client (" + action + "): " + var3.getMessage());
         }
      });
   }

   private void disconnectByServerAction(String action) {
      this.running = false;
      this.clearScheduledReconnect();
      this.clearFrameBuffers();

      try {
         if (this.webSocket != null && !this.webSocket.isOutputClosed()) {
            this.webSocket.sendClose(1000, "Server " + action);
         }
      } catch (Exception var6) {
      } finally {
         this.webSocket = null;
      }

      this.playerStore.clear();
   }

   private synchronized void clearScheduledReconnect() {
      if (this.reconnectTask != null) {
         this.reconnectTask.cancel(false);
         this.reconnectTask = null;
      }
   }

   private synchronized void scheduleReconnect(String reason) {
      if (this.running && !this.executor.isShutdown()) {
         if (this.reconnectTask != null && !this.reconnectTask.isDone()) {
            LoggerUtil.info("Reconnect already scheduled, skip new one. Reason: " + reason);
         } else {
            int attempt = this.reconnectAttempts.incrementAndGet();
            if (attempt > 5) {
               this.running = false;
               LoggerUtil.error("Reconnect limit reached (5). Stopping WebSocket retries. Last reason: " + reason);
            } else {
               LoggerUtil.warn("Scheduling reconnect attempt " + attempt + "/5 in 5 minutes. Reason: " + reason);
               this.reconnectTask = this.executor.schedule(() -> {
                  synchronized (this) {
                     this.reconnectTask = null;
                  }

                  if (this.running) {
                     this.fetchServerAndConnect();
                  }
               }, 5L, TimeUnit.MINUTES);
            }
         }
      }
   }

   private void applyCloseReasonFallback(String closeReason) {
      if (closeReason != null && !closeReason.isBlank()) {
         String normalized = closeReason.trim().toLowerCase(Locale.ROOT);
         if (!normalized.startsWith("server:banned:")) {
            if (normalized.startsWith("server:kicked:")) {
               WebSocketClient.ActionLevel level = WebSocketClient.ActionLevel.fromWire(normalized.substring("server:kicked:".length()));
               if (level == WebSocketClient.ActionLevel.SOCKET) {
                  this.applyServerAction("kicked", WebSocketClient.ActionLevel.SOCKET, "Kicked by server");
               } else if (level == WebSocketClient.ActionLevel.MULTIPLAYER) {
                  this.applyServerAction("kicked", WebSocketClient.ActionLevel.MULTIPLAYER, "Kicked from multiplayer by server");
               } else if (level == WebSocketClient.ActionLevel.CLIENT) {
                  this.applyServerAction("kicked", WebSocketClient.ActionLevel.CLIENT, "Client was kicked by server");
               }
            }
         } else {
            WebSocketClient.ActionLevel level = WebSocketClient.ActionLevel.fromWire(normalized.substring("server:banned:".length()));
            if (level == WebSocketClient.ActionLevel.CLIENT && !isActiveBanLevel(WebSocketClient.ActionLevel.CLIENT)) {
               setActiveBan(WebSocketClient.ActionLevel.CLIENT, "Banned by server", null);
               this.applyServerAction("banned", WebSocketClient.ActionLevel.CLIENT, composeBanMessage("Banned by server", null));
            } else if (level == WebSocketClient.ActionLevel.MULTIPLAYER && !isActiveBanLevel(WebSocketClient.ActionLevel.MULTIPLAYER)) {
               String reason = "Multiplayer access is blocked by server ban.";
               setActiveBan(WebSocketClient.ActionLevel.MULTIPLAYER, reason, null);
               this.applyServerAction("banned", WebSocketClient.ActionLevel.MULTIPLAYER, composeBanMessage(reason, null));
            } else if (level == WebSocketClient.ActionLevel.SOCKET) {
               this.applyServerAction("banned", WebSocketClient.ActionLevel.SOCKET, "Banned by server");
            }
         }
      }
   }

   private static boolean isActiveBanLevel(WebSocketClient.ActionLevel level) {
      WebSocketClient.ActiveBan current = activeBan;
      return current != null && current.level() == level && !isBanExpired(current.expiresAtEpochSeconds());
   }

   private static void setActiveBan(WebSocketClient.ActionLevel level, String reason, Long expiresAtEpochSeconds) {
      activeBan = new WebSocketClient.ActiveBan(level, reason, expiresAtEpochSeconds);
   }

   private static void clearActiveBan() {
      activeBan = null;
   }

   private static boolean isBanExpired(Long expiresAtEpochSeconds) {
      return expiresAtEpochSeconds == null ? false : Instant.now().getEpochSecond() >= expiresAtEpochSeconds;
   }

   private static String formatRemainingTime(Long expiresAtEpochSeconds) {
      if (expiresAtEpochSeconds == null) {
         return "permanent";
      } else {
         long remaining = expiresAtEpochSeconds - Instant.now().getEpochSecond();
         if (remaining <= 0L) {
            return "expired";
         } else {
            long days = remaining / 86400L;
            long hours = remaining % 86400L / 3600L;
            long minutes = remaining % 3600L / 60L;
            long seconds = remaining % 60L;
            if (days > 0L) {
               return days + "d " + hours + "h";
            } else if (hours > 0L) {
               return hours + "h " + minutes + "m";
            } else {
               return minutes > 0L ? minutes + "m " + seconds + "s" : seconds + "s";
            }
         }
      }
   }

   private static String composeBanMessage(String reason, Long expiresAtEpochSeconds) {
      return reason + "\n" + formatRemainingTime(expiresAtEpochSeconds);
   }

   private static String normalizeServerAddress(String serverAddress) {
      if (serverAddress == null) {
         return null;
      } else {
         String trimmed = serverAddress.trim();
         return trimmed.isEmpty() ? null : trimmed;
      }
   }

   private static String resolveCurrentServerAddress() {
      MinecraftClient mc = Main.mc;
      if (mc == null) {
         return null;
      } else {
         ServerInfo info = mc.getCurrentServerEntry();
         return info == null ? null : info.address;
      }
   }

   private static boolean isLocalOnlyServerAddress(String rawAddress) {
      String value = normalizeServerAddress(rawAddress);
      if (value == null) {
         return false;
      } else {
         String host = value.toLowerCase(Locale.ROOT);
         if (host.startsWith("[")) {
            int end = host.indexOf(93);
            if (end > 1) {
               host = host.substring(1, end);
            }
         } else {
            int colonIndex = host.indexOf(58);
            if (colonIndex > 0) {
               host = host.substring(0, colonIndex);
            }
         }

         return host.equals("localhost")
            || host.equals("0.0.0.0")
            || host.equals("127.0.0.1")
            || host.startsWith("127.")
            || host.equals("::1")
            || host.equals("0:0:0:0:0:0:0:1");
      }
   }

   private String normalizeReason(String reason) {
      return reason != null && !reason.isBlank() ? reason : "Banned by server";
   }

   private String getString(JsonObject json, String key, String fallback) {
      if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
         try {
            return json.get(key).getAsString();
         } catch (Exception var5) {
         }
      }

      return fallback;
   }

   private Long getLong(JsonObject json, String key, Long fallback) {
      if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
         try {
            return json.get(key).getAsLong();
         } catch (Exception var5) {
         }
      }

      return fallback;
   }

   private String buildHwidSeed() {
      String osName = System.getProperty("os.name", "");
      String osArch = System.getProperty("os.arch", "");
      String osVersion = System.getProperty("os.version", "");
      String userName = System.getProperty("user.name", "");
      String computerName = System.getenv("COMPUTERNAME");
      String processor = System.getenv("PROCESSOR_IDENTIFIER");
      if (computerName == null) {
         computerName = "";
      }

      if (processor == null) {
         processor = "";
      }

      return osName + "|" + osArch + "|" + osVersion + "|" + userName + "|" + computerName + "|" + processor;
   }

   private void clearFrameBuffers() {
      synchronized (this.frameLock) {
         this.textFrameBuffer.setLength(0);
         this.binaryFrameBuffer.reset();
      }
   }

   private static void warnOnce(String key, String message) {
      if (WARN_ONCE.putIfAbsent(key, Boolean.TRUE) == null) {
         LoggerUtil.warn(message);
      }
   }

   private static enum ActionLevel {
      SOCKET("socket"),
      MULTIPLAYER("multiplayer"),
      CLIENT("client");

      private final String wire;

      private ActionLevel(String wire) {
         this.wire = wire;
      }

      String asWire() {
         return this.wire;
      }

      static WebSocketClient.ActionLevel fromWire(String rawLevel) {
         if (rawLevel == null) {
            return SOCKET;
         } else {
            String var1 = rawLevel.trim().toLowerCase(Locale.ROOT);

            return switch (var1) {
               case "multiplayer", "mc-server", "mcserver", "mc" -> MULTIPLAYER;
               case "client" -> CLIENT;
               default -> SOCKET;
            };
         }
      }
   }

   private record ActiveBan(WebSocketClient.ActionLevel level, String reason, Long expiresAtEpochSeconds) {
   }

   private record BanCheckResult(boolean banned, WebSocketClient.ActionLevel level, String reason, Long expiresAtEpochSeconds) {
   }
}
