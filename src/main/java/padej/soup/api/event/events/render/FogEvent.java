package padej.soup.api.event.events.render;

import padej.soup.api.event.events.callables.EventCancellable;

public class FogEvent extends EventCancellable {
   public static final FogEvent INSTANCE = new FogEvent();
   private float distance;
   private int color;

   public FogEvent reset() {
      this.distance = 0.0F;
      this.color = 0;
      this.setCancelled(false);
      return this;
   }

   public float getDistance() {
      return this.distance;
   }

   public int getColor() {
      return this.color;
   }

   public void setDistance(float distance) {
      this.distance = distance;
   }

   public void setColor(int color) {
      this.color = color;
   }
}
