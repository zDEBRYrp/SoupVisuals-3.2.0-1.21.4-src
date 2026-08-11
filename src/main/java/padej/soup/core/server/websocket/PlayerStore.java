package padej.soup.core.server.websocket;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import padej.soup.core.server.ServerApi;

public class PlayerStore {
   private final Map<Integer, PlayerInfo> players = new ConcurrentHashMap<>();

   public void addPlayer(PlayerInfo player) {
      this.players.put(player.getPlayerId(), player);
      this.notifyCountChanged();
   }

   public void removePlayer(int playerId) {
      PlayerInfo removed = this.players.remove(playerId);
      if (removed != null) {
         this.notifyCountChanged();
      }
   }

   public PlayerInfo getPlayerByNickname(String nickname) {
      return this.players.values().stream().filter(p -> p.getNickname().equalsIgnoreCase(nickname)).findFirst().orElse(null);
   }

   public Collection<PlayerInfo> getAllPlayers() {
      return this.players.values();
   }

   public boolean hasPlayerByNickname(String nickname) {
      return this.players.values().stream().anyMatch(p -> p.getNickname().equalsIgnoreCase(nickname));
   }

   public int getPlayerCount() {
      return this.players.size();
   }

   public void clear() {
      this.players.clear();
      this.notifyCountChanged();
   }

   private void notifyCountChanged() {
      ServerApi.updateOnlineCount(this.players.size());
   }
}
