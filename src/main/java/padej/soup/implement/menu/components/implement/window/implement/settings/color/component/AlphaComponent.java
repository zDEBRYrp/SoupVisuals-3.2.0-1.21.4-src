package padej.soup.implement.menu.components.implement.window.implement.settings.color.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.menu.components.AbstractComponent;

public class AlphaComponent extends AbstractComponent {
   private final ColorSetting setting;
   private boolean alphaDragging;
   private float X;
   private float Y;
   private float W;
   private float H;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      this.X = this.x + 6.0F;
      this.Y = this.y + 83.5F;
      this.W = 138.0F;
      this.H = 4.0F;
      float clampedX = MathHelper.clamp(this.X + this.W * this.setting.getAlpha(), this.X, this.X + this.W - 4.0F);
      float min = MathHelper.clamp((mouseX - this.X) / this.W, 0.0F, 1.0F);
      image.setTexture("textures/color_picker/alpha.png").render(ShapeProperties.create(matrix, this.X, this.Y, this.W, this.H).build());
      rectangle.render(
         ShapeProperties.create(matrix, this.X, this.Y - 0.2, this.W + 0.5, this.H + 1.0F)
            .round(1.5F)
            .color(Integer.MIN_VALUE, 134217728, this.setting.getColorWithAlpha(), this.setting.getColorWithAlpha())
            .build()
      );
      rectangle.render(
         ShapeProperties.create(matrix, clampedX, this.Y, this.H, this.H).round(this.H / 2.0F).thickness(3.0F).color(16777215).outlineColor(-1).build()
      );
      if (this.alphaDragging) {
         this.setting.setAlpha(min);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.alphaDragging = button == 0 && MathUtil.isHovered(mouseX, mouseY, this.X, this.Y, this.W, this.H);
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.alphaDragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public AlphaComponent(ColorSetting setting) {
      this.setting = setting;
   }
}
