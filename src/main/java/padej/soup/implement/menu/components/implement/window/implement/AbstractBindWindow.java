package padej.soup.implement.menu.components.implement.window.implement;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.StringUtil;
import padej.soup.implement.menu.components.implement.window.AbstractWindow;
import padej.soup.implement.menu.components.implement.window.implement.module.ModuleBindWindow;

public abstract class AbstractBindWindow extends AbstractWindow {
   private boolean binding;

   protected abstract int getKey();

   protected abstract void setKey(int var1);

   protected abstract int getType();

   protected abstract void setType(int var1);

   @Override
   public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      rectangle.render(ShapeProperties.create(matrix, this.x, this.y, this.width, this.height).round(4.0F).softness(25.0F).color(838860800).build());
      rectangle.render(
         ShapeProperties.create(matrix, this.x, this.y, this.width, this.height)
            .round(4.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline(0.8F, 1.0F))
            .color(ColorUtil.getRect(1.0F))
            .build()
      );
      Fonts.getSize(14).drawString(matrix, LocalizationManager.getInstance().get("ui.binding_module"), this.x + 5.0F, this.y + 8.0F, -1);
      image.setIcon(61459).render(ShapeProperties.create(matrix, this.x + this.width - 13.0F, this.y + 5.3F, 8.0, 8.0).color(-1).build());
      this.drawKeyButton(matrix);
      this.drawTypeButton(matrix);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 57.0F, this.y + 37.0F, 52.0, 13.0)) {
            this.setType(this.getType() != 1 ? 1 : 0);
         }

         float stringWidth = Fonts.getSize(14).getStringWidth(StringUtil.getBindName(this.getKey()));
         if (MathUtil.isHovered(mouseX, mouseY, this.x + this.width - stringWidth - 15.0F, this.y + 18.8F, stringWidth + 10.0F, 13.0)) {
            this.binding = !this.binding;
         }

         if (MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 13.0F, this.y + 5.3F, 8.0, 8.0)) {
            this.setKey(-1);
            if (this instanceof ModuleBindWindow) {
               ((ModuleBindWindow)this).getModule().setState(false);
            }
         }
      }

      if (this.binding && button > 1) {
         this.setKey(button);
         this.binding = false;
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      int key = keyCode == 261 ? -1 : keyCode;
      if (this.binding) {
         this.setKey(key);
         this.binding = false;
         if (key == -1 && this instanceof ModuleBindWindow) {
            ((ModuleBindWindow)this).getModule().setState(false);
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private void drawKeyButton(MatrixStack matrix) {
      float stringWidth = Fonts.getSize(14).getStringWidth(StringUtil.getBindName(this.getKey()));
      rectangle.render(
         ShapeProperties.create(matrix, this.x + this.width - stringWidth - 15.0F, this.y + 18.8F, stringWidth + 10.0F, 13.0)
            .round(2.0F)
            .thickness(2.0F)
            .softness(1.0F)
            .outlineColor(ColorUtil.getOutline(0.8F, 1.0F))
            .color(ColorUtil.getOutline(0.1F, 1.0F))
            .build()
      );
      int bindingColor = this.binding ? -8288257 : ColorUtil.getText();
      Fonts.getSize(14).drawString(matrix, StringUtil.getBindName(this.getKey()), this.x + this.width - 10.0F - stringWidth, this.y + 23.6F, bindingColor);
      Fonts.getSize(14).drawString(matrix, LocalizationManager.getInstance().get("ui.key"), (int)(this.x + 5.0F), (int)(this.y + 24.3), ColorUtil.getText());
   }

   private void drawTypeButton(MatrixStack matrix) {
      rectangle.render(
         ShapeProperties.create(matrix, this.x + this.width - 57.0F, this.y + 37.0F, 52.0, 13.0)
            .round(2.0F)
            .thickness(2.0F)
            .softness(1.0F)
            .outlineColor(ColorUtil.getOutline(0.8F, 1.0F))
            .color(ColorUtil.getOutline(0.1F, 1.0F))
            .build()
      );
      if (this.getType() == 1) {
         rectangle.render(
            ShapeProperties.create(matrix, this.x + this.width - 34.0F, this.y + 37.0F, 29.0, 13.0).round(2.0F, 2.0F, 0.0F, 0.0F).color(-8288257).build()
         );
      } else {
         rectangle.render(
            ShapeProperties.create(matrix, this.x + this.width - 57.0F, this.y + 37.0F, 23.0, 13.0).round(0.0F, 0.0F, 2.0F, 2.0F).color(-8288257).build()
         );
      }

      Fonts.getSize(12).drawString(matrix, LocalizationManager.getInstance().get("ui.hold"), this.x + 52.0F, this.y + 42.3, ColorUtil.getText());
      Fonts.getSize(12).drawString(matrix, LocalizationManager.getInstance().get("ui.toggle"), this.x + 73.0F, this.y + 42.3, ColorUtil.getText());
      Fonts.getSize(14)
         .drawString(matrix, LocalizationManager.getInstance().get("ui.bind_mode"), (int)(this.x + 5.0F), (int)(this.y + 42.3F), ColorUtil.getText());
   }
}
