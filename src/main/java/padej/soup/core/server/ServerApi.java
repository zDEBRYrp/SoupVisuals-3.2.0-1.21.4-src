package padej.soup.core.server;

import net.minecraft.client.MinecraftClient;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.core.Main;
import padej.soup.core.server.websocket.PlayerStore;
import padej.soup.core.server.websocket.WebSocketClient;

public class ServerApi {
   private WebSocketClient webSocketClient;
   private PlayerStore playerStore;
   private static volatile int currentOnlineCount = 0;
   private boolean running = false;

   public static String getHttpApiUrl() {
      return ServerEndpoints.getHttpApiUrl();
   }

   public void start() {
      if (this.running) {
         LoggerUtil.warn("ServerApi is already running");
      } else {
         MinecraftClient mc = Main.mc;
         if (mc.getSession() != null && mc.getSession().getUsername() != null) {
            this.running = true;
            this.playerStore = new PlayerStore();
            this.webSocketClient = new WebSocketClient(this.playerStore);
            this.webSocketClient.connect();
            LoggerUtil.info("ServerApi started with WebSocket support");
         } else {
            LoggerUtil.error("Cannot start ServerApi: No valid session");
         }
      }
   }

   public void stop() {
      this.running = false;
      if (this.webSocketClient != null) {
         this.webSocketClient.disconnect();
      }

      currentOnlineCount = 0;
      LoggerUtil.info("ServerApi stopped");
   }

   public static void updateOnlineCount(int count) {
      currentOnlineCount = count;
   }

   public PlayerStore getPlayerStore() {
      return this.playerStore;
   }

   public static int getCurrentOnlineCount() {
      return currentOnlineCount;
   }

   public boolean isRunning() {
      return this.running;
   }
}
