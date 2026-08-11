package padej.soup.api.event.events.render;

import net.minecraft.client.gui.DrawContext;
import padej.soup.api.event.events.Event;
import padej.soup.api.system.draw.DrawEngine;

public class DrawEvent implements Event {
   public static final DrawEvent INSTANCE = new DrawEvent();
   private DrawContext drawContext;
   private DrawEngine drawEngine;
   private float partialTicks;

   public DrawEvent set(DrawContext drawContext, DrawEngine drawEngine, float partialTicks) {
      this.drawContext = drawContext;
      this.drawEngine = drawEngine;
      this.partialTicks = partialTicks;
      return this;
   }

   public DrawContext getDrawContext() {
      return this.drawContext;
   }

   public DrawEngine getDrawEngine() {
      return this.drawEngine;
   }

   public float getPartialTicks() {
      return this.partialTicks;
   }
}
