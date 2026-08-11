package padej.soup.api.repository.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import padej.soup.core.Main;
import padej.soup.core.server.ServerApi;
import padej.soup.core.server.websocket.PlayerInfo;
import padej.soup.core.server.websocket.PlayerStore;

public class OnlinePlayersManager {
   public static List<String> getOnlinePlayers() {
      ServerApi api = Main.getInstance().getServerApi();
      if (api != null && api.getPlayerStore() != null) {
         PlayerStore store = api.getPlayerStore();
         Collection<PlayerInfo> players = store.getAllPlayers();
         List<String> nicknames = new ArrayList<>();

         for (PlayerInfo player : players) {
            nicknames.add(player.getNickname());
         }

         return nicknames;
      } else {
         return new ArrayList<>();
      }
   }

   public static boolean isPlayerOnline(String nickname) {
      ServerApi api = Main.getInstance().getServerApi();
      return api != null && api.getPlayerStore() != null ? api.getPlayerStore().hasPlayerByNickname(nickname) : false;
   }

   public static int getOnlineCount() {
      ServerApi api = Main.getInstance().getServerApi();
      return api != null && api.getPlayerStore() != null ? api.getPlayerStore().getPlayerCount() : 0;
   }

   public static PlayerInfo getPlayerInfo(String nickname) {
      ServerApi api = Main.getInstance().getServerApi();
      return api != null && api.getPlayerStore() != null ? api.getPlayerStore().getPlayerByNickname(nickname) : null;
   }

   public static String getPlayerRole(String nickname) {
      PlayerInfo info = getPlayerInfo(nickname);
      return info != null ? info.role() : "user";
   }
}
