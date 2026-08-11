package padej.soup.implement.events.render;

import net.minecraft.client.util.math.MatrixStack;

@Deprecated
public class WorldRenderEvent extends padej.soup.api.event.events.render.WorldRenderEvent {
   public WorldRenderEvent(MatrixStack stack, float partialTicks) {
      this.set(stack, partialTicks);
   }
}
