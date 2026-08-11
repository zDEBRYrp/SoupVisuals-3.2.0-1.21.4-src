package padej.soup.implement.events.player;

import net.minecraft.entity.player.PlayerEntity;

@Deprecated
public class JumpEvent extends padej.soup.api.event.events.player.JumpEvent {
   public JumpEvent(PlayerEntity player) {
      super(player);
   }
}
