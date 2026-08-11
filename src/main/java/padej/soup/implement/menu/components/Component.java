package padej.soup.implement.menu.components;

import net.minecraft.client.gui.DrawContext;

public interface Component {
   void render(DrawContext var1, int var2, int var3, float var4);

   void tick();

   boolean mouseClicked(double var1, double var3, int var5);

   boolean mouseReleased(double var1, double var3, int var5);

   boolean mouseDragged(double var1, double var3, int var5, double var6, double var8);

   boolean mouseScrolled(double var1, double var3, double var5);

   boolean keyPressed(int var1, int var2, int var3);

   boolean keyReleased(int var1, int var2, int var3);

   boolean charTyped(char var1, int var2);

   boolean isHover(double var1, double var3);
}
