package padej.soup.implement.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import padej.soup.api.feature.module.setting.implement.ButtonSetting;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.StringUtil;
import padej.soup.implement.menu.components.implement.other.ButtonComponent;

public class SButtonComponent extends AbstractSettingComponent {
   private final ButtonComponent buttonComponent = new ButtonComponent();
   private final ButtonSetting setting;

   public SButtonComponent(ButtonSetting setting) {
      super(setting);
      this.setting = setting;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.updateVisibilityAnimation();
      boolean isModified = this.setting.isModified();
      float textOffset = isModified ? ResetIconComponent.getTextOffset() : 0.0F;
      String wrapped = StringUtil.wrap(this.setting.getLocalizedName(), (int)(this.width - 89.0F - textOffset), 14);
      float wrappedHeight = Fonts.getSize(14).getStringHeight(wrapped);
      float oneLineHeight = Fonts.getSize(14).getStringHeight("");
      this.height = (int)(18.0F + Math.max(0.0F, wrappedHeight - oneLineHeight));
      int textColor = ColorUtil.multAlpha(ColorUtil.getText(), this.currentAlpha);
      this.resetIcon.position(this.x, this.y).alpha(this.currentAlpha).modified(isModified).render(context.getMatrices());
      float textX = this.x + 9.0F + textOffset;
      Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawString(context.getMatrices(), wrapped, textX, this.y + 9.0F, textColor);
      ((ButtonComponent)this.buttonComponent
            .setText(LocalizationManager.getInstance().get("module.setting.button_click"))
            .setRunnable(this.setting.getRunnable())
            .position(this.x + this.width - 9.0F - this.buttonComponent.width, this.y + this.height / 2.0F - 6.0F))
         .render(context, mouseX, mouseY, delta);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.resetIcon.isHovered(mouseX, mouseY)) {
         this.setting.reset();
         return true;
      } else {
         return this.buttonComponent.mouseClicked(mouseX, mouseY, button) ? true : super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      if (!this.setting.isVisible()) {
         return false;
      } else {
         return MathUtil.isHovered(
               mouseX,
               mouseY,
               this.x + this.width - 9.0F - this.buttonComponent.width,
               this.y + this.height / 2.0F - 6.0F,
               this.buttonComponent.width,
               this.buttonComponent.height
            )
            ? true
            : MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, this.y + 6.0F, this.width - this.buttonComponent.width - 18.0F, this.height - 12.0F);
      }
   }
}
