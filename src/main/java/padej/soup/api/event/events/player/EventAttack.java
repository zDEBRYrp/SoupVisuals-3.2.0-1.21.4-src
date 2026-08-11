package padej.soup.api.event.events.player;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import padej.soup.api.event.events.callables.EventCancellable;

public class EventAttack extends EventCancellable {
   private final Entity target;
   private final boolean pre;
   private final Vec3d hitPos;

   public EventAttack(Entity target, boolean pre, Vec3d hitPos) {
      this.target = target;
      this.pre = pre;
      this.hitPos = hitPos;
   }

   public Entity getTarget() {
      return this.target;
   }

   public boolean isPre() {
      return this.pre;
   }

   public Vec3d getHitPos() {
      return this.hitPos;
   }
}
