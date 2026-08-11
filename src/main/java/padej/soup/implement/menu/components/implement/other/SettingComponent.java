package padej.soup.implement.menu.components.implement.other;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.RotationAxis;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.EaseInOutAnimation;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.menu.components.AbstractComponent;

public class SettingComponent extends AbstractComponent {
   private Runnable runnable;
   private boolean windowOpen = false;
   private final Animation rotationAnimation = new EaseInOutAnimation().setMs(600).setValue(1.0);

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (this.windowOpen) {
         this.rotationAnimation.setDirection(Direction.FORWARDS);
      } else {
         this.rotationAnimation.setDirection(Direction.BACKWARDS);
      }

      float rotationProgress = this.rotationAnimation.getOutput().floatValue();
      float rotationAngle = 90.0F + rotationProgress * 360.0F;
      float centerX = this.x + 3.5F;
      float centerY = this.y + 3.5F;
      context.getMatrices().push();
      context.getMatrices().translate(centerX, centerY, 0.0F);
      context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationAngle));
      context.getMatrices().translate(-centerX, -centerY, 0.0F);
      image.setIcon(61457).render(ShapeProperties.create(context.getMatrices(), this.x, this.y, 7.0, 7.0).color(-5263172).build());
      context.getMatrices().pop();
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, this.x, this.y, 7.0, 7.0) && button == 0) {
         this.runnable.run();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public SettingComponent setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }

   public SettingComponent setWindowOpen(boolean windowOpen) {
      this.windowOpen = windowOpen;
      return this;
   }
}
