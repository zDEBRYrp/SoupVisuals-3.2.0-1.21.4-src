package padej.soup.implement.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.feature.module.setting.implement.BindSetting;
import padej.soup.api.system.font.Fonts;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.StringUtil;
import padej.soup.base.util.render.BindBadgeRenderer;

public class BindComponent extends AbstractSettingComponent {
   private final BindSetting setting;
   private boolean binding;

   public BindComponent(BindSetting setting) {
      super(setting);
      this.setting = setting;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.updateVisibilityAnimation();
      boolean isModified = this.setting.isModified();
      float textOffset = isModified ? ResetIconComponent.getTextOffset() : 0.0F;
      MatrixStack matrix = context.getMatrices();
      String bindName = StringUtil.getBindName(this.setting.getKey());
      String name = this.binding ? "(" + bindName + ") ..." : bindName;
      String wrapped = StringUtil.wrap(this.setting.getLocalizedName(), (int)(this.width - 74.0F - textOffset), 14);
      float wrappedHeight = Fonts.getSize(14).getStringHeight(wrapped);
      float oneLineHeight = Fonts.getSize(14).getStringHeight("");
      this.height = (int)(18.0F + Math.max(0.0F, wrappedHeight - oneLineHeight));
      BindBadgeRenderer.draw(matrix, name, this.x + this.width, this.y + this.height / 2.0F);
      int textColor = ColorUtil.multAlpha(ColorUtil.getText(), this.currentAlpha);
      this.resetIcon.position(this.x, this.y).alpha(this.currentAlpha).modified(isModified).render(matrix);
      float textX = this.x + 9.0F + textOffset;
      if (isModified) {
         Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawItalicString(matrix, wrapped, textX, this.y + 9.0F, textColor);
      } else {
         Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawString(matrix, wrapped, textX, this.y + 9.0F, textColor);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.resetIcon.isHovered(mouseX, mouseY)) {
         this.setting.reset();
         return true;
      } else if (button == 0 && MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
         this.binding = !this.binding;
         return true;
      } else if (this.binding && button > 1) {
         this.setting.setKey(button);
         this.binding = false;
         return true;
      } else {
         if (this.binding && button == 0) {
            this.binding = false;
         }

         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return !this.setting.isVisible() ? false : MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.binding) {
         int key = keyCode == 261 ? -1 : keyCode;
         this.setting.setKey(key);
         this.binding = false;
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }
}
