package padej.soup.implement.menu.components.implement.window;

import net.minecraft.client.gui.DrawContext;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.menu.components.AbstractComponent;

public abstract class AbstractWindow extends AbstractComponent {
   private boolean dragging;
   private boolean draggable;
   private int dragX;
   private int dragY;
   private final Animation scaleAnimation = new DecelerateAnimation().setValue(1.0).setMs(200);

   public AbstractWindow() {
      this.scaleAnimation.setDirection(Direction.FORWARDS);
   }

   public AbstractWindow draggable(boolean draggable) {
      this.draggable = draggable;
      return this;
   }

   public AbstractWindow size(float width, float height) {
      this.width = width;
      this.height = height;
      return this;
   }

   public AbstractWindow position(float x, float y) {
      this.x = x;
      this.y = y;
      return this;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isHovered(mouseX, mouseY) && button == 0 && this.draggable) {
         this.dragging = true;
         this.dragX = (int)(this.x - mouseX);
         this.dragY = (int)(this.y - mouseY);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (this.dragging && this.draggable) {
         this.x = mouseX + this.dragX;
         this.y = mouseY + this.dragY;
         int screenWidth = mc.getWindow().getScaledWidth();
         int screenHeight = mc.getWindow().getScaledHeight();
         this.x = Math.max(0.0F, Math.min(this.x, screenWidth - this.width));
         this.y = Math.max(0.0F, Math.min(this.y, screenHeight - this.height));
      }

      float scale = this.scaleAnimation.getOutput().floatValue();
      MathUtil.scale(
         context.getMatrices(), this.x + this.width / 2.0F, this.y + this.height / 2.0F, scale, () -> this.drawWindow(context, mouseX, mouseY, delta)
      );
   }

   protected abstract void drawWindow(DrawContext var1, int var2, int var3, float var4);

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
      return true;
   }

   public boolean isHovered(double mouseX, double mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
   }

   public void startCloseAnimation() {
      this.scaleAnimation.setDirection(Direction.BACKWARDS);
   }

   public boolean isCloseAnimationFinished() {
      return this.scaleAnimation.isFinished(Direction.BACKWARDS);
   }

   public Animation getScaleAnimation() {
      return this.scaleAnimation;
   }
}
