package padej.soup.implement.menu.components.implement.settings;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.StringUtil;

public class ValueComponent extends AbstractSettingComponent {
   public static final int SLIDER_WIDTH = 45;
   private final ValueSetting setting;
   private boolean dragging;
   private double animation;
   private float previousValue;
   private float sliderY;
   private boolean isSliderHovered = false;

   public ValueComponent(ValueSetting setting) {
      super(setting);
      this.setting = setting;
      this.previousValue = setting.getValue();
   }

   @Override
   public void tick() {
      super.tick();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.updateVisibilityAnimation();
      boolean isModified = this.setting.isModified();
      float textOffset = isModified ? ResetIconComponent.getTextOffset() : 0.0F;
      MatrixStack matrix = context.getMatrices();
      String wrapped = StringUtil.wrap(this.setting.getLocalizedName(), (int)(this.width - 68.0F - textOffset), 14);
      float wrappedHeight = Fonts.getSize(14).getStringHeight(wrapped);
      float oneLineHeight = Fonts.getSize(14).getStringHeight("");
      this.height = (int)(24.0F + Math.max(0.0F, wrappedHeight - oneLineHeight));
      this.sliderY = this.y + this.height / 2.0F;
      this.isSliderHovered = MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 45.0F - 9.0F, this.sliderY - 2.0F, 45.0, 6.0);
      String value = String.valueOf(this.setting.getValue());
      int valueColor = ColorUtil.multAlpha(ColorUtil.getClientColor(), this.currentAlpha);
      int textColor = ColorUtil.multAlpha(ColorUtil.getText(), this.currentAlpha);
      Fonts.getSize(12, Fonts.Type.INTER_BOLD)
         .drawString(matrix, value, this.x + this.width - 9.0F - Fonts.getSize(12).getStringWidth(value), this.y + 3.0F, valueColor);
      this.changeValue(this.getDifference(mouseX, matrix));
      this.resetIcon.position(this.x, this.y).alpha(this.currentAlpha).modified(isModified).render(matrix);
      float textX = this.x + 9.0F + textOffset;
      if (isModified) {
         Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawItalicString(matrix, wrapped, textX, this.y + 9.0F, textColor);
      } else {
         Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawString(matrix, wrapped, textX, this.y + 9.0F, textColor);
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      if (!this.setting.isVisible()) {
         return false;
      } else {
         return MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 45.0F - 9.0F, this.sliderY - 2.0F, 45.0, 6.0)
            ? true
            : MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, this.y + 6.0F, this.width - 45.0F - 18.0F, this.height - 12.0F);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.resetIcon.isHovered(mouseX, mouseY)) {
         this.setting.reset();
         return true;
      } else {
         boolean wasClicked = MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 45.0F - 9.0F, this.sliderY - 2.0F, 45.0, 6.0) && button == 0;
         if (wasClicked) {
            SoundManager.playSound(SoundManager.CLICK);
            this.dragging = true;
            return true;
         } else {
            this.dragging = false;
            return super.mouseClicked(mouseX, mouseY, button);
         }
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   private float getDifference(int mouseX, MatrixStack matrix) {
      float percentValue = 45.0F * (this.setting.getValue() - this.setting.getMin()) / (this.setting.getMax() - this.setting.getMin());
      float difference = MathHelper.clamp(mouseX - (this.x + this.width - 45.0F - 9.0F), 0.0F, 45.0F);
      this.animation = MathUtil.interpolate(this.animation, (double)percentValue);
      int bgColor = ColorUtil.multAlpha(758006093, this.currentAlpha);
      int sliderColor = ColorUtil.multAlpha(ColorUtil.getClientColor(), this.currentAlpha);
      int sliderDarkColor = ColorUtil.multAlpha(new Color(ColorUtil.getClientColor()).darker().getRGB(), this.currentAlpha);
      int thumbBg = ColorUtil.multAlpha(ColorUtil.getMainGuiColor(), this.currentAlpha);
      rectangle.render(ShapeProperties.create(matrix, this.x + this.width - 45.0F - 9.0F, this.sliderY, 45.0, 1.0).color(bgColor).build());
      rectangle.render(
         ShapeProperties.create(matrix, this.x + this.width - 45.0F - 9.0F, this.sliderY, (float)this.animation, 1.0)
            .color(sliderColor, sliderColor, sliderDarkColor, sliderDarkColor)
            .build()
      );
      float v = MathHelper.clamp((float)(this.x + this.width - 45.0F + this.animation), 0.0F, this.x + this.width - 4.0F);
      rectangle.render(ShapeProperties.create(matrix, v - 10.0F, this.sliderY - 2.5F, 6.0, 6.0).round(3.0F).color(thumbBg).build());
      rectangle.render(ShapeProperties.create(matrix, v - 8.8F, this.sliderY - 1.5F, 4.0, 4.0).round(2.0F).color(sliderColor).build());
      return difference;
   }

   private void changeValue(float difference) {
      BigDecimal bd = BigDecimal.valueOf((double)(difference / 45.0F * (this.setting.getMax() - this.setting.getMin()) + this.setting.getMin()))
         .setScale(2, RoundingMode.HALF_UP);
      if (this.dragging) {
         float value = difference == 0.0F ? this.setting.getMin() : bd.floatValue();
         if (this.setting.isInteger()) {
            value = (int)value;
         }

         if (value != this.previousValue) {
            SoundManager.playSound(SoundManager.SLIDER_STEP, 1.0F, 1.0F);
            this.previousValue = value;
         }

         this.setting.setValue(value);
      }
   }

   public boolean isSliderHovered() {
      return this.isSliderHovered;
   }
}
