package padej.soup.implement.menu.components.implement.other;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.menu.components.AbstractComponent;

public class CheckComponent extends AbstractComponent {
   private boolean state;
   private Runnable runnable;
   private final Animation alphaAnimation = new DecelerateAnimation().setMs(300).setValue(255.0);
   private final Animation stencilAnimation = new DecelerateAnimation().setMs(200).setValue(8.0);

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      this.alphaAnimation.setDirection(this.state ? Direction.FORWARDS : Direction.BACKWARDS);
      this.stencilAnimation.setDirection(this.state ? Direction.FORWARDS : Direction.BACKWARDS);
      int stateColor = this.state ? ColorUtil.getClientColor() : ColorUtil.getGuiRectColor(1.0F);
      int outlineStateColor = this.state ? ColorUtil.getClientColor() : ColorUtil.getOutline();
      int opacity = this.alphaAnimation.getOutput().intValue();
      rectangle.render(
         ShapeProperties.create(matrix, this.x, this.y, 8.0, 8.0)
            .round(1.5F)
            .thickness(2.0F)
            .softness(0.5F)
            .outlineColor(outlineStateColor)
            .color(MathUtil.applyOpacity(stateColor, opacity))
            .build()
      );
      float scale = this.stencilAnimation.getOutput().floatValue() / 8.0F;
      matrix.push();
      matrix.translate(this.x + 4.0F, this.y + 4.0F, 0.0F);
      matrix.scale(scale, scale, 1.0F);
      matrix.translate(-(this.x + 4.0F), -(this.y + 4.0F), 0.0F);
      image.setIcon(61442).render(ShapeProperties.create(matrix, this.x + 2.0F, this.y + 2.0F, 4.0, 4.0).color(MathUtil.applyOpacity(-1, opacity)).build());
      matrix.pop();
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, this.x, this.y, 8.0, 8.0) && button == 0) {
         SoundManager.playSound(this.state ? SoundManager.TURN_OFF : SoundManager.TURN_ON);
         this.runnable.run();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public CheckComponent setState(boolean state) {
      this.state = state;
      return this;
   }

   public CheckComponent setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }
}
