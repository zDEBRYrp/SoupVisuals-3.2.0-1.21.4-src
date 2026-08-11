package padej.soup.core.server.websocket;

public record PlayerInfo(int id, String name, String role, boolean official) {
   public PlayerInfo(int id, String name, String role) {
      this(id, name, role, false);
   }

   public int getPlayerId() {
      return this.id;
   }

   public String getNickname() {
      return this.name;
   }
}
