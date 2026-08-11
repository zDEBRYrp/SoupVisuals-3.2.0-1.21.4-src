package padej.soup.api.system.discord;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import net.minecraft.util.Identifier;
import padej.protect.ProtIgnore;
import padej.soup.api.system.discord.utils.DiscordEventHandlers;
import padej.soup.api.system.discord.utils.DiscordRPC;
import padej.soup.api.system.discord.utils.DiscordRichPresence;
import padej.soup.api.system.discord.utils.RPCButton;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.core.Main;
import padej.soup.implement.features.modules.mctiers.Balancer;

@ProtIgnore
public class DiscordManager {
   private final DiscordManager.DiscordDaemonThread discordDaemonThread = new DiscordManager.DiscordDaemonThread();
   private boolean running = true;
   private DiscordManager.DiscordInfo info = new DiscordManager.DiscordInfo("Unknown", "", "");
   private Identifier avatarId;
   private long startTimestamp = System.currentTimeMillis() / 1000L;

   public void init() {
      LoggerUtil.info("[Discord] Initializing Discord RPC...");
      String osName = System.getProperty("os.name").toLowerCase();
      LoggerUtil.info("[Discord] Detected OS: " + osName);
      if (!osName.contains("win")) {
         LoggerUtil.info("[Discord] Discord RPC is disabled on non-Windows systems");
         this.running = false;
      } else {
         LoggerUtil.info("[Discord] Creating event handlers...");
         DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
            .ready(user -> {
               LoggerUtil.info("[Discord] RPC Ready! User: " + user.username + ", ID: " + user.userId);
               Main.getInstance().getDiscordManager().setInfo(new DiscordManager.DiscordInfo(user.username, "", user.userId));
               this.updatePresence();
            })
            .errored((errorCode, message) -> LoggerUtil.error("[Discord] RPC Error - Code: " + errorCode + ", Message: " + message))
            .disconnected((errorCode, message) -> LoggerUtil.info("[Discord] RPC Disconnected - Code: " + errorCode + ", Message: " + message))
            .build();
         LoggerUtil.info("[Discord] Initializing Discord RPC with app ID...");
         DiscordRPC.INSTANCE.Discord_Initialize("1363751101794619463", handlers, true, "");
         LoggerUtil.info("[Discord] Starting Discord daemon thread...");
         this.discordDaemonThread.start();
      }
   }

   public void stopRPC() {
      DiscordRPC.INSTANCE.Discord_Shutdown();
      this.running = false;
   }

   public void load() throws IOException {
   }

   private void updatePresence() {
      Month month = LocalDate.now().getMonth();
      String imageUrl = month != Month.DECEMBER && month != Month.JANUARY && month != Month.FEBRUARY
         ? "https://i.postimg.cc/4dSdyVcw/lv-0-20251005210442.gif"
         : "https://i.postimg.cc/gJdDrs0D/soup-winter.gif";
      DiscordRichPresence.Builder builder = new DiscordRichPresence.Builder()
         .setStartTimestamp(this.startTimestamp)
         .setLargeImage(imageUrl, "Soup Visuals")
         .setButtons(RPCButton.create("Download", "https://modrinth.com/mod/soup-api"), RPCButton.create("Discord Server", "https://discord.gg/gT35BwzMET"));
      if (Balancer.getInstance().isEnabled()) {
         String smallIcon = Balancer.getInstance().rpcIcon();
         if (!smallIcon.isEmpty()) {
            builder.setSmallImage(smallIcon, Balancer.getInstance().fixType.getSelected());
         }
      }

      DiscordRPC.INSTANCE.Discord_UpdatePresence(builder.build());
   }

   public void setRunning(boolean running) {
      this.running = running;
   }

   public void setInfo(DiscordManager.DiscordInfo info) {
      this.info = info;
   }

   public void setAvatarId(Identifier avatarId) {
      this.avatarId = avatarId;
   }

   public void setStartTimestamp(long startTimestamp) {
      this.startTimestamp = startTimestamp;
   }

   public DiscordManager.DiscordDaemonThread getDiscordDaemonThread() {
      return this.discordDaemonThread;
   }

   public boolean isRunning() {
      return this.running;
   }

   public DiscordManager.DiscordInfo getInfo() {
      return this.info;
   }

   public Identifier getAvatarId() {
      return this.avatarId;
   }

   public long getStartTimestamp() {
      return this.startTimestamp;
   }

   private class DiscordDaemonThread extends Thread {
      @Override
      public void run() {
         this.setName("Discord-RPC");
         LoggerUtil.info("[Discord] Daemon thread started");
         int tickCounter = 0;

         try {
            while (Main.getInstance().getDiscordManager().isRunning()) {
               DiscordRPC.INSTANCE.Discord_RunCallbacks();
               if (tickCounter % 8 == 0) {
                  DiscordManager.this.load();
                  DiscordManager.this.updatePresence();
               }

               tickCounter++;
               Thread.sleep(2000L);
            }

            LoggerUtil.info("[Discord] Daemon thread stopped normally");
         } catch (Exception var3) {
            LoggerUtil.error("[Discord] Daemon thread error: " + var3.getMessage());
            var3.printStackTrace();
            DiscordManager.this.stopRPC();
         }

         super.run();
      }
   }

   public record DiscordInfo(String userName, String avatarUrl, String userId) {
   }
}
