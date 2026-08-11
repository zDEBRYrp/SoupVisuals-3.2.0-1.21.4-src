package padej.soup.implement.menu.components.implement.settings.multiselect;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.StringUtil;
import padej.soup.base.util.render.ScissorManager;
import padej.soup.core.Main;
import padej.soup.implement.menu.components.implement.settings.AbstractSettingComponent;
import padej.soup.implement.menu.components.implement.settings.ResetIconComponent;

public class MultiSelectComponent extends AbstractSettingComponent {
   private final List<MultiSelectedButton> multiSelectedButtons = new ArrayList<>();
   private static final List<MultiSelectComponent> openDropdowns = new ArrayList<>();
   private static final float DROPDOWN_SCREEN_PADDING = 4.0F;
   private final MultiSelectSetting setting;
   private boolean open;
   private float dropdownListX;
   private float dropDownListY;
   private float dropDownListWidth;
   private float dropDownListHeight;
   private final Animation alphaAnimation = new DecelerateAnimation().setMs(300).setValue(1.0);
   private final Animation slideAnimation = new DecelerateAnimation().setMs(250).setValue(1.0);

   public MultiSelectComponent(MultiSelectSetting setting) {
      super(setting);
      this.setting = setting;
      this.alphaAnimation.setDirection(Direction.BACKWARDS);
      this.slideAnimation.setDirection(Direction.BACKWARDS);

      for (String s : setting.getList()) {
         this.multiSelectedButtons.add(new MultiSelectedButton(setting, s));
      }
   }

   private void setOpen(boolean open) {
      if (this.open != open) {
         this.open = open;
         if (open) {
            for (MultiSelectComponent d : new ArrayList<>(openDropdowns)) {
               if (d != this) {
                  d.open = false;
               }
            }

            if (!openDropdowns.contains(this)) {
               openDropdowns.add(this);
            }
         }
      }
   }

   public static void renderOpenDropdowns(DrawContext context, int mouseX, int mouseY, float delta) {
      openDropdowns.removeIf(d -> d.alphaAnimation.isFinished(Direction.BACKWARDS));

      for (MultiSelectComponent dropdown : new ArrayList<>(openDropdowns)) {
         dropdown.renderSelectList(context, mouseX, mouseY, delta);
      }
   }

   public static boolean handleMouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      } else {
         for (MultiSelectComponent dropdown : new ArrayList<>(openDropdowns)) {
            if (dropdown.open
               && MathUtil.isHovered(mouseX, mouseY, dropdown.dropdownListX, dropdown.dropDownListY, dropdown.dropDownListWidth, dropdown.dropDownListHeight)) {
               for (MultiSelectedButton btn : dropdown.multiSelectedButtons) {
                  if (btn.mouseClicked(mouseX, mouseY, button)) {
                     return true;
                  }
               }

               return true;
            }
         }

         return false;
      }
   }

   public static void closeAllDropdowns() {
      for (MultiSelectComponent dropdown : new ArrayList<>(openDropdowns)) {
         dropdown.open = false;
         dropdown.alphaAnimation.setDirection(Direction.BACKWARDS);
         dropdown.slideAnimation.setDirection(Direction.BACKWARDS);
      }
   }

   public static void handleGlobalClick(double mouseX, double mouseY) {
      for (MultiSelectComponent dropdown : new ArrayList<>(openDropdowns)) {
         if (dropdown.open && !dropdown.isHover(mouseX, mouseY)) {
            dropdown.setOpen(false);
         }
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.updateVisibilityAnimation();
      boolean isModified = this.setting.isModified();
      float textOffset = isModified ? ResetIconComponent.getTextOffset() : 0.0F;
      MatrixStack matrices = context.getMatrices();
      String wrapped = StringUtil.wrap(this.setting.getLocalizedName(), (int)(this.width - 89.0F - textOffset), 14);
      float wrappedHeight = Fonts.getSize(14).getStringHeight(wrapped);
      float oneLineHeight = Fonts.getSize(14).getStringHeight("");
      this.height = (int)(18.0F + Math.max(0.0F, wrappedHeight - oneLineHeight));
      List<String> fullSettingsList = this.setting.getList();
      this.dropdownListX = this.x + this.width - 75.0F + 2.0F;
      this.dropDownListWidth = 66.0F;
      this.dropDownListHeight = fullSettingsList.size() * 12;
      this.dropDownListY = this.resolveDropdownListY(this.y + this.height + 2.0F, this.dropDownListHeight);
      this.alphaAnimation.setDirection(this.open ? Direction.FORWARDS : Direction.BACKWARDS);
      this.slideAnimation.setDirection(this.open ? Direction.FORWARDS : Direction.BACKWARDS);
      this.renderSelected(matrices);
      int textColor = ColorUtil.multAlpha(ColorUtil.getText(), this.currentAlpha);
      this.resetIcon.position(this.x, this.y).alpha(this.currentAlpha).modified(isModified).render(matrices);
      float textX = this.x + 9.0F + textOffset;
      if (isModified) {
         Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawItalicString(matrices, wrapped, textX, this.y + 9.0F, textColor);
      } else {
         Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawString(matrices, wrapped, textX, this.y + 9.0F, textColor);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.resetIcon.isHovered(mouseX, mouseY)) {
            this.setting.reset();
            return true;
         }

         float buttonY = this.y + this.height / 2.0F - 7.0F;
         if (MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 75.0F + 2.0F, buttonY, 66.0, 14.0)) {
            SoundManager.playSound(SoundManager.CLICK);
            this.setOpen(!this.open);
            return true;
         }

         if (this.open && !this.isHoveredList(mouseX, mouseY)) {
            this.setOpen(false);
            return true;
         }

         if (this.open) {
            for (MultiSelectedButton selectedButton : this.multiSelectedButtons) {
               if (selectedButton.mouseClicked(mouseX, mouseY, button)) {
                  return true;
               }
            }

            if (this.isHoveredList(mouseX, mouseY)) {
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      float buttonY = this.y + this.height / 2.0F - 7.0F;
      if (MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 75.0F + 2.0F, buttonY, 66.0, 14.0)) {
         return true;
      } else {
         return MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, this.y + 6.0F, this.width - 75.0F - 9.0F, this.height - 12.0F)
            ? true
            : this.open && this.isHoveredList(mouseX, mouseY);
      }
   }

   private void renderSelected(MatrixStack matrix) {
      FontRenderer font = Fonts.getSize(12);
      int x1 = (int)(this.x + this.width - 72.0F + 2.0F);
      float buttonY = this.y + this.height / 2.0F - 7.0F;
      rectangle.render(
         ShapeProperties.create(matrix, x1 - 3, buttonY, 66.0, 14.0)
            .round(2.0F)
            .thickness(2.0F)
            .softness(0.5F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getGuiRectColor(0.5F))
            .build()
      );
      String selectedName = String.join(", ", this.setting.getSelected());
      float offset = 64.0F;
      ScissorManager scissor = Main.getInstance().getScissorManager();
      scissor.push(matrix.peek().getPositionMatrix(), x1 - 1, buttonY, 62.0F, 14.0F);
      font.drawStringWithScroll(matrix, selectedName, x1, buttonY + 6.0F, offset, ColorUtil.getText());
      scissor.pop();
      if (font.getStringWidth(selectedName) - offset > 0.0F) {
         rectangle.render(
            ShapeProperties.create(matrix, this.x + this.width - 13.75F + 2.0F, buttonY + 1.0F, 4.0, 12.0)
               .round(3.0F)
               .color(ColorUtil.getGuiRectColor(0.0F), ColorUtil.getGuiRectColor(0.0F), ColorUtil.getGuiRectColor(1.0F), ColorUtil.getGuiRectColor(1.0F))
               .build()
         );
         rectangle.render(
            ShapeProperties.create(matrix, x1 - 2.25F, buttonY + 1.0F, 4.0, 12.0)
               .round(3.0F)
               .color(ColorUtil.getGuiRectColor(1.0F), ColorUtil.getGuiRectColor(1.0F), ColorUtil.getGuiRectColor(0.0F), ColorUtil.getGuiRectColor(0.0F))
               .build()
         );
      }
   }

   private void renderSelectList(DrawContext context, int mouseX, int mouseY, float delta) {
      float opacity = this.alphaAnimation.getOutput().floatValue();
      float slideProgress = this.slideAnimation.getOutput().floatValue();
      float animatedHeight = this.dropDownListHeight * slideProgress;
      float animatedY = this.dropDownListY;
      context.getMatrices().push();
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.dropdownListX, animatedY, this.dropDownListWidth, animatedHeight)
            .round(4.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline(opacity, 1.0F))
            .color(ColorUtil.getGuiRectColor(opacity))
            .build()
      );
      ScissorManager scissor = Main.getInstance().getScissorManager();
      scissor.push(context.getMatrices().peek().getPositionMatrix(), this.dropdownListX, animatedY, this.dropDownListWidth, animatedHeight);
      float offset = this.dropDownListY;
      int visibleButtons = Math.max(1, (int)(this.multiSelectedButtons.size() * slideProgress));

      for (int i = 0; i < Math.min(visibleButtons, this.multiSelectedButtons.size()); i++) {
         MultiSelectedButton button = this.multiSelectedButtons.get(i);
         button.x = this.dropdownListX;
         button.y = offset;
         button.width = this.dropDownListWidth;
         button.height = 12.0F;
         button.setAlpha(opacity);
         button.render(context, mouseX, mouseY, delta);
         offset += 12.0F;
      }

      scissor.pop();
      context.getMatrices().pop();
   }

   private boolean isHoveredList(double mouseX, double mouseY) {
      return MathUtil.isHovered(mouseX, mouseY, this.dropdownListX, this.dropDownListY - 16.0F, this.dropDownListWidth, this.dropDownListHeight + 16.0F);
   }

   private float resolveDropdownListY(float preferredY, float listHeight) {
      float minY = 4.0F;
      float maxY = window.getScaledHeight() - listHeight - 4.0F;
      return maxY <= minY ? minY : Math.max(minY, Math.min(preferredY, maxY));
   }

   @Override
   public float getExpandedHeight() {
      if (this.open && !(this.slideAnimation.getOutput().floatValue() < 0.01F)) {
         float slideProgress = this.slideAnimation.getOutput().floatValue();
         float dropdownHeight = this.dropDownListHeight * slideProgress;
         return this.height + 2.0F + dropdownHeight;
      } else {
         return this.height;
      }
   }
}
