package padej.soup.implement.features.draggables.playerinfo;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;

public class XYZComponent implements PlayerInfoComponent, QuickImports {
   private BlockPos lastPos = BlockPos.ORIGIN;
   private float smoothedWidth = 0.0F;
   private String cachedText = "0, 0, 0";

   @Override
   public void tick() {
      if (mc.player != null) {
         BlockPos pos = mc.player.getBlockPos();
         if (!pos.equals(this.lastPos)) {
            this.lastPos = pos;
            this.cachedText = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
         }
      }
   }

   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      float yIconOffset = 4.0F;
      float xIconOffset = -1.0F;
      if (this.shouldShowIcons()) {
         image.setIcon(61461).render(ShapeProperties.create(matrix, x + xIconOffset, y + yIconOffset, iconSize, iconSize).color(this.getIconColor()).build());
      }

      font.drawString(matrix, this.cachedText, x + iconOffset, y + 6.5F, ColorUtil.getText());
      return iconOffset + font.getStringWidth(this.cachedText);
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      return iconOffset + font.getStringWidth(this.cachedText);
   }

   @Override
   public float getSmoothedWidth(FontRenderer font, float height) {
      float targetWidth = this.getWidth(font, height);
      this.smoothedWidth = MathUtil.interpolate(this.smoothedWidth, targetWidth);
      return this.smoothedWidth;
   }

   @Override
   public String getName() {
      return "XYZ";
   }
}
