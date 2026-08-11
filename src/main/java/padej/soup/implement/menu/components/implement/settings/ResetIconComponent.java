package padej.soup.implement.menu.components.implement.settings;

import net.minecraft.client.util.math.MatrixStack;
import padej.soup.base.QuickImports;

public class ResetIconComponent implements QuickImports {
   private static final float ICON_SIZE = 8.0F;
   private static final float ICON_X_OFFSET = 8.0F;
   private static final float ICON_Y_OFFSET = 6.5F;
   private static final String ICON_TEXTURE = "textures/reset.png";
   private float x;
   private float y;
   private float alpha = 1.0F;
   private boolean isModified = false;

   public ResetIconComponent position(float parentX, float parentY) {
      this.x = parentX + 8.0F;
      this.y = parentY + 6.5F;
      return this;
   }

   public ResetIconComponent alpha(float alpha) {
      this.alpha = alpha;
      return this;
   }

   public ResetIconComponent modified(boolean isModified) {
      this.isModified = isModified;
      return this;
   }

   public void render(MatrixStack matrix) {
   }

   public boolean isHovered(double mouseX, double mouseY) {
      return false;
   }

   public static float getTextOffset() {
      return 0.0F;
   }

   public static float getXOffset() {
      return 8.0F;
   }

   public static float getYOffset() {
      return 6.5F;
   }
}
