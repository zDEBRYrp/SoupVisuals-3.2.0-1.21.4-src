package padej.soup.implement.features.draggables.playerinfo;

import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.features.modules.hud.Icons;

public interface PlayerInfoComponent {
   default boolean shouldShowIcons() {
      return Icons.getInstance().getShowIcons().isValue();
   }

   default int getIconColor() {
      Icons icons = Icons.getInstance();
      if (!icons.getColoredIcons().isValue()) {
         return ColorUtil.getText();
      } else {
         return icons.getIconGradient().isValue() ? ColorUtil.fade(8) : ColorUtil.getClientColor();
      }
   }

   float render(MatrixStack var1, float var2, float var3, float var4, FontRenderer var5);

   float getWidth(FontRenderer var1, float var2);

   default float getSmoothedWidth(FontRenderer font, float height) {
      return this.getWidth(font, height);
   }

   default void tick() {
   }

   String getName();

   default boolean hasSeparator() {
      return true;
   }
}
