package padej.soup.implement.menu.components.implement.other;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.menu.MenuScreen;
import padej.soup.implement.menu.components.AbstractComponent;

public class HomeComponent extends AbstractComponent {
   private final Animation hoverAnimation = new DecelerateAnimation().setMs(200).setValue(0.15F);
   private final Animation selectAnimation = new DecelerateAnimation().setMs(200).setValue(1.0);

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      float componentWidth = 17.0F;
      float componentHeight = 17.0F;
      boolean isHomeSelected = ModuleCategory.HOME.equals(MenuScreen.INSTANCE.getCategory());
      boolean isHovered = MathUtil.isHovered(mouseX, mouseY, this.x, this.y, componentWidth, componentHeight);
      this.hoverAnimation.setDirection(isHovered ? Direction.FORWARDS : Direction.BACKWARDS);
      this.selectAnimation.setDirection(isHomeSelected ? Direction.FORWARDS : Direction.BACKWARDS);
      float hoverScale = 1.0F + this.hoverAnimation.getOutput().floatValue();
      float selectProgress = this.selectAnimation.getOutput().floatValue();
      float centerX = this.x + componentWidth / 2.0F;
      float centerY = this.y + componentHeight / 2.0F;
      MathUtil.scale(
         matrix,
         centerX,
         centerY,
         hoverScale,
         () -> {
            int outlineColor = ColorUtil.overCol(ColorUtil.getOutline(), ColorUtil.getClientColor(), selectProgress);
            int iconColor = ColorUtil.overCol(ColorUtil.getDescription(), ColorUtil.getClientColor(), selectProgress);
            rectangle.render(
               ShapeProperties.create(matrix, this.x, this.y, componentWidth, componentHeight)
                  .round(3.0F)
                  .thickness(2.0F)
                  .softness(1.0F)
                  .outlineColor(outlineColor)
                  .color(ColorUtil.getGuiRectColor(0.5F))
                  .build()
            );
            float iconSize = 7.0F;
            float iconX = this.x + (componentWidth - iconSize) / 2.0F;
            float iconY = this.y + (componentHeight - iconSize) / 2.0F;
            image.setIcon(61447).render(ShapeProperties.create(matrix, iconX, iconY, iconSize, iconSize).color(iconColor).build());
         }
      );
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && MathUtil.isHovered(mouseX, mouseY, this.x, this.y, 17.0, 17.0)) {
         MenuScreen.INSTANCE.setCategory(ModuleCategory.HOME);
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }
}
