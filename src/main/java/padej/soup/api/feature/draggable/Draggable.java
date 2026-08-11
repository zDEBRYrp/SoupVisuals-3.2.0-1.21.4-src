package padej.soup.api.feature.draggable;

import net.minecraft.client.gui.DrawContext;
import padej.soup.api.event.events.container.SetScreenEvent;
import padej.soup.api.event.events.packet.PacketEvent;

public interface Draggable {
   boolean visible();

   void tick();

   void render(DrawContext var1, int var2, int var3, float var4);

   void packet(PacketEvent var1);

   void setScreen(SetScreenEvent var1);

   boolean mouseClicked(double var1, double var3, int var5);

   boolean mouseReleased(double var1, double var3, int var5);
}
