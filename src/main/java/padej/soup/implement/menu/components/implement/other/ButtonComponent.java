package padej.soup.implement.menu.components.implement.other;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.menu.components.AbstractComponent;

public class ButtonComponent extends AbstractComponent {
   private String text;
   private Runnable runnable;
   private int color = -8288257;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      this.width = Fonts.getSize(12).getStringWidth(this.text) + 13.0F;
      this.height = 12.0F;
      rectangle.render(ShapeProperties.create(matrix, this.x, this.y, this.width, this.height).round(2.0F).color(this.color).build());
      Fonts.getSize(12, Fonts.Type.INTER_BOLD).drawCenteredString(matrix, this.text, this.x + this.width / 2.0, this.y + 5.0F, -1);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height) && button == 0) {
         SoundManager.playSound(SoundManager.CLICK);
         this.runnable.run();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public ButtonComponent setText(String text) {
      this.text = text;
      return this;
   }

   public ButtonComponent setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }

   public ButtonComponent setColor(int color) {
      this.color = color;
      return this;
   }
}
