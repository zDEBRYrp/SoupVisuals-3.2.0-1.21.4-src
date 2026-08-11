package padej.soup.implement.menu.components.implement.settings;

import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.menu.components.AbstractComponent;

public abstract class AbstractSettingComponent extends AbstractComponent {
   private final Setting setting;
   private final Animation visibilityAnimation = new DecelerateAnimation().setMs(350).setValue(1.0);
   protected final ResetIconComponent resetIcon = new ResetIconComponent();
   public float currentAlpha = 1.0F;

   public void updateVisibilityAnimation() {
      this.visibilityAnimation.setDirection(this.setting.isVisible() ? Direction.FORWARDS : Direction.BACKWARDS);
      this.currentAlpha = this.visibilityAnimation.getOutput().floatValue();
   }

   public double getVisibilityProgress() {
      return this.visibilityAnimation.getOutput();
   }

   public boolean shouldSkipRender() {
      return this.getVisibilityProgress() <= 0.01;
   }

   public float getExpandedHeight() {
      return this.height;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 1 && this.setting.isModified() && MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
         this.setting.reset();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public Setting getSetting() {
      return this.setting;
   }

   public Animation getVisibilityAnimation() {
      return this.visibilityAnimation;
   }

   public ResetIconComponent getResetIcon() {
      return this.resetIcon;
   }

   public float getCurrentAlpha() {
      return this.currentAlpha;
   }

   public AbstractSettingComponent(Setting setting) {
      this.setting = setting;
   }
}
