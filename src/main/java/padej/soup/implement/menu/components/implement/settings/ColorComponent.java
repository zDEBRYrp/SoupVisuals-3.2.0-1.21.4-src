package padej.soup.implement.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.StringUtil;
import padej.soup.implement.menu.components.implement.window.AbstractWindow;
import padej.soup.implement.menu.components.implement.window.implement.settings.color.ColorWindow;

public class ColorComponent extends AbstractSettingComponent {
   private final ColorSetting setting;

   public ColorComponent(ColorSetting setting) {
      super(setting);
      this.setting = setting;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.updateVisibilityAnimation();
      boolean isModified = this.setting.isModified();
      float textOffset = isModified ? ResetIconComponent.getTextOffset() : 0.0F;
      if (this.setting.isVisible()) {
         MatrixStack matrix = context.getMatrices();
         String wrapped = StringUtil.wrap(this.setting.getLocalizedName(), (int)(this.width - 29.0F - textOffset), 14);
         float wrappedHeight = Fonts.getSize(14).getStringHeight(wrapped);
         float oneLineHeight = Fonts.getSize(14).getStringHeight("");
         this.height = (int)(18.0F + Math.max(0.0F, wrappedHeight - oneLineHeight));
         int textColor = ColorUtil.multAlpha(ColorUtil.getText(), this.currentAlpha);
         this.resetIcon.position(this.x, this.y).alpha(this.currentAlpha).modified(isModified).render(matrix);
         float textX = this.x + 9.0F + textOffset;
         if (isModified) {
            Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawItalicString(matrix, wrapped, textX, this.y + 9.0F, textColor);
         } else {
            Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawString(matrix, wrapped, textX, this.y + 9.0F, textColor);
         }

         rectangle.render(
            ShapeProperties.create(matrix, this.x + this.width - 15.5F, this.y + this.height / 2.0F - 3.5F, 7.0, 7.0)
               .round(3.5F)
               .color(this.setting.getColor())
               .build()
         );
         rectangle.render(
            ShapeProperties.create(matrix, this.x + this.width - 15.5F, this.y + this.height / 2.0F - 3.5F, 7.0, 7.0)
               .round(3.5F)
               .thickness(2.0F)
               .softness(1.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(0)
               .build()
         );
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (!this.setting.isVisible()) {
         return false;
      } else if (button == 0 && this.resetIcon.isHovered(mouseX, mouseY)) {
         this.setting.reset();
         return true;
      } else if (MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 15.5F, this.y + this.height / 2.0F - 3.5F, 7.0, 7.0) && button == 0) {
         AbstractWindow existingWindow = null;

         for (AbstractWindow window : windowManager.getWindows()) {
            if (window instanceof ColorWindow) {
               existingWindow = window;
               break;
            }
         }

         if (existingWindow != null) {
            SoundManager.playSound(SoundManager.CLOSE_GUI, 1.0F, 1.5F);
            windowManager.delete(existingWindow);
         } else {
            SoundManager.playSound(SoundManager.OPEN_GUI, 1.0F, 1.5F);
            int windowWidth = 150;
            int windowHeight = 165;
            int windowX = (int)(mouseX + 10.0);
            int windowY = (int)(mouseY + 10.0);
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            windowX = Math.max(0, Math.min(windowX, screenWidth - windowWidth));
            windowY = Math.max(0, Math.min(windowY, screenHeight - windowHeight));
            AbstractWindow colorWindow = new ColorWindow(this.setting).position(windowX, windowY).size((float)windowWidth, (float)windowHeight).draggable(true);
            windowManager.add(colorWindow);
         }

         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      if (!this.setting.isVisible()) {
         return false;
      } else {
         return MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 15.5F, this.y + this.height / 2.0F - 3.5F, 7.0, 7.0)
            ? true
            : MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, this.y + 6.0F, this.width - 24.5F, this.height - 12.0F);
      }
   }
}
