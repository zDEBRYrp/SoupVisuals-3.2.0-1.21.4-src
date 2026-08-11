package padej.soup.implement.events.player;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

@Deprecated
public class EventAttack extends padej.soup.api.event.events.player.EventAttack {
   public EventAttack(Entity target, boolean pre, Vec3d hitPos) {
      super(target, pre, hitPos);
   }
}
