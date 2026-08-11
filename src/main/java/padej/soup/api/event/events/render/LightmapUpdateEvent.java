package padej.soup.api.event.events.render;

import net.minecraft.client.render.LightmapTextureManager;
import padej.soup.api.event.events.Event;

public class LightmapUpdateEvent implements Event {
   public static final LightmapUpdateEvent INSTANCE = new LightmapUpdateEvent();
   private LightmapTextureManager lightmapTextureManager;

   public LightmapUpdateEvent set(LightmapTextureManager lightmapTextureManager) {
      this.lightmapTextureManager = lightmapTextureManager;
      return this;
   }

   public LightmapTextureManager getLightmapTextureManager() {
      return this.lightmapTextureManager;
   }
}
