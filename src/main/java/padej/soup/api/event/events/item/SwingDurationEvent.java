package padej.soup.api.event.events.item;

import padej.soup.api.event.events.callables.EventCancellable;

public class SwingDurationEvent extends EventCancellable {
   private float animation;

   public float getAnimation() {
      return this.animation;
   }

   public void setAnimation(float animation) {
      this.animation = animation;
   }
}
