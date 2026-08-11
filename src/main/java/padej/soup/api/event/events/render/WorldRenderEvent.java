package padej.soup.api.event.events.render;

import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.event.events.Event;

public class WorldRenderEvent implements Event {
   public static final WorldRenderEvent INSTANCE = new WorldRenderEvent();
   private MatrixStack stack;
   private float partialTicks;

   public WorldRenderEvent set(MatrixStack stack, float partialTicks) {
      this.stack = stack;
      this.partialTicks = partialTicks;
      return this;
   }

   public MatrixStack getStack() {
      return this.stack;
   }

   public float getPartialTicks() {
      return this.partialTicks;
   }
}
