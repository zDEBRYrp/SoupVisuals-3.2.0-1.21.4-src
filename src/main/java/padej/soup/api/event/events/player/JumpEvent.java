package padej.soup.api.event.events.player;

import net.minecraft.entity.player.PlayerEntity;
import padej.soup.api.event.events.callables.EventCancellable;

public class JumpEvent extends EventCancellable {
   private PlayerEntity player;

   public PlayerEntity getPlayer() {
      return this.player;
   }

   public JumpEvent(PlayerEntity player) {
      this.player = player;
   }
}
