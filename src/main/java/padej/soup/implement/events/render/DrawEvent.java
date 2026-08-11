package padej.soup.implement.events.render;

import net.minecraft.client.gui.DrawContext;
import padej.soup.api.system.draw.DrawEngine;

@Deprecated
public class DrawEvent extends padej.soup.api.event.events.render.DrawEvent {
   public DrawEvent(DrawContext drawContext, DrawEngine drawEngine, float partialTicks) {
      this.set(drawContext, drawEngine, partialTicks);
   }
}
