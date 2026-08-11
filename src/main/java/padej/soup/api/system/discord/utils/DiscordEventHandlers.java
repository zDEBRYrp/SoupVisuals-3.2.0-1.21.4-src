package padej.soup.api.system.discord.utils;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
import padej.protect.ProtIgnore;
import padej.soup.api.system.discord.callbacks.DisconnectedCallback;
import padej.soup.api.system.discord.callbacks.ErroredCallback;
import padej.soup.api.system.discord.callbacks.JoinGameCallback;
import padej.soup.api.system.discord.callbacks.JoinRequestCallback;
import padej.soup.api.system.discord.callbacks.ReadyCallback;
import padej.soup.api.system.discord.callbacks.SpectateGameCallback;

@ProtIgnore
public class DiscordEventHandlers extends Structure {
   public DisconnectedCallback disconnected;
   public JoinRequestCallback joinRequest;
   public SpectateGameCallback spectateGame;
   public ReadyCallback ready;
   public ErroredCallback errored;
   public JoinGameCallback joinGame;

   protected List<String> getFieldOrder() {
      return Arrays.asList("ready", "disconnected", "errored", "joinGame", "spectateGame", "joinRequest");
   }

   public static class Builder {
      private final DiscordEventHandlers handlers = new DiscordEventHandlers();

      public DiscordEventHandlers build() {
         return this.handlers;
      }

      public DiscordEventHandlers.Builder disconnected(DisconnectedCallback var1) {
         this.handlers.disconnected = var1;
         return this;
      }

      public DiscordEventHandlers.Builder errored(ErroredCallback var1) {
         this.handlers.errored = var1;
         return this;
      }

      public DiscordEventHandlers.Builder ready(ReadyCallback var1) {
         this.handlers.ready = var1;
         return this;
      }

      public DiscordEventHandlers.Builder joinRequest(JoinRequestCallback var1) {
         this.handlers.joinRequest = var1;
         return this;
      }

      public DiscordEventHandlers.Builder joinGame(JoinGameCallback var1) {
         this.handlers.joinGame = var1;
         return this;
      }

      public DiscordEventHandlers.Builder spectateGame(SpectateGameCallback var1) {
         this.handlers.spectateGame = var1;
         return this;
      }
   }
}
