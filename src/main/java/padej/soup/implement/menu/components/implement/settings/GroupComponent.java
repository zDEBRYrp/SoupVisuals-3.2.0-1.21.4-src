package padej.soup.implement.menu.components.implement.settings;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.system.font.Fonts;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.StringUtil;
import padej.soup.implement.menu.components.implement.other.CheckComponent;
import padej.soup.implement.menu.components.implement.other.SettingComponent;
import padej.soup.implement.menu.components.implement.window.AbstractWindow;
import padej.soup.implement.menu.components.implement.window.implement.settings.group.GroupWindow;

public class GroupComponent extends AbstractSettingComponent {
   private final CheckComponent checkComponent = new CheckComponent();
   private final SettingComponent settingComponent = new SettingComponent();
   private final GroupSetting setting;

   public GroupComponent(GroupSetting setting) {
      super(setting);
      this.setting = setting;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.updateVisibilityAnimation();
      boolean isModified = this.setting.isModified();
      float textOffset = isModified ? ResetIconComponent.getTextOffset() : 0.0F;
      String wrapped = StringUtil.wrap(this.setting.getLocalizedName(), (int)(this.width - 42.0F - textOffset), 14);
      float wrappedHeight = Fonts.getSize(14).getStringHeight(wrapped);
      float oneLineHeight = Fonts.getSize(14).getStringHeight("");
      this.height = (int)(18.0F + Math.max(0.0F, wrappedHeight - oneLineHeight));
      int textColor = ColorUtil.multAlpha(ColorUtil.getText(), this.currentAlpha);
      this.resetIcon.position(this.x, this.y).alpha(this.currentAlpha).modified(isModified).render(context.getMatrices());
      float textX = this.x + 9.0F + textOffset;
      if (isModified) {
         Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawItalicString(context.getMatrices(), wrapped, textX, this.y + 9.0F, textColor);
      } else {
         Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawString(context.getMatrices(), wrapped, textX, this.y + 9.0F, textColor);
      }

      boolean isWindowOpen = this.isGroupWindowOpen();
      if (this.setting.isCheckbox()) {
         ((CheckComponent)this.checkComponent.position(this.x + this.width - 15.0F, this.y + this.height / 2.0F - 3.5F))
            .setRunnable(() -> this.setting.setValue(!this.setting.isValue()))
            .setState(this.setting.isValue())
            .render(context, mouseX, mouseY, delta);
         ((SettingComponent)this.settingComponent.position(this.x + this.width - 27.0F, this.y + this.height / 2.0F - 2.0F))
            .setRunnable(() -> this.spawnWindow(mouseX, mouseY))
            .setWindowOpen(isWindowOpen)
            .render(context, mouseX, mouseY, delta);
      } else {
         ((SettingComponent)this.settingComponent.position(this.x + this.width - 15.0F, this.y + this.height / 2.0F - 2.0F))
            .setRunnable(() -> this.spawnWindow(mouseX, mouseY))
            .setWindowOpen(isWindowOpen)
            .render(context, mouseX, mouseY, delta);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.resetIcon.isHovered(mouseX, mouseY)) {
         this.setting.reset();
         return true;
      } else if (this.setting.isCheckbox() && this.checkComponent.mouseClicked(mouseX, mouseY, button)) {
         return true;
      } else {
         return this.settingComponent.mouseClicked(mouseX, mouseY, button) ? true : super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      if (!this.setting.isVisible()) {
         return false;
      } else if (this.setting.isCheckbox() && MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 16.0F, this.y + this.height / 2.0F - 3.5F, 7.0, 7.0)) {
         return true;
      } else {
         if (this.setting.isCheckbox()) {
            if (MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 27.0F, this.y + this.height / 2.0F - 2.0F, 12.0, 12.0)) {
               return true;
            }
         } else if (MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 15.0F, this.y + this.height / 2.0F - 2.0F, 12.0, 12.0)) {
            return true;
         }

         return MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, this.y + 6.0F, this.width - 27.0F, this.height - 12.0F);
      }
   }

   private void spawnWindow(int mouseX, int mouseY) {
      AbstractWindow existingWindow = null;

      for (AbstractWindow window : windowManager.getWindows()) {
         if (window instanceof GroupWindow && ((GroupWindow)window).getSetting() == this.setting) {
            existingWindow = window;
            break;
         }
      }

      if (existingWindow != null) {
         this.closeChildGroupWindows(this.setting);
         windowManager.delete(existingWindow);
      } else {
         int windowWidth = 137;
         int windowHeight = 200;
         int windowX = mouseX + 5;
         int windowY = mouseY + 5;
         int screenWidth = mc.getWindow().getScaledWidth();
         int screenHeight = mc.getWindow().getScaledHeight();
         windowX = Math.max(0, Math.min(windowX, screenWidth - windowWidth));
         windowY = Math.max(0, Math.min(windowY, screenHeight - windowHeight));
         AbstractWindow groupWindow = new GroupWindow(this.setting).position(windowX, windowY).size((float)windowWidth, 23.0F).draggable(false);
         windowManager.add(groupWindow);
      }
   }

   private void closeChildGroupWindows(GroupSetting parentSetting) {
      List<AbstractWindow> windowsCopy = new ArrayList<>(windowManager.getWindows());

      for (Setting childSetting : parentSetting.getSubSettings()) {
         if (childSetting instanceof GroupSetting groupSetting) {
            this.closeChildGroupWindows(groupSetting);

            for (AbstractWindow window : windowsCopy) {
               if (window instanceof GroupWindow groupWindow && groupWindow.getSetting() == groupSetting) {
                  windowManager.delete(window);
                  break;
               }
            }
         }
      }
   }

   private boolean isGroupWindowOpen() {
      for (AbstractWindow window : windowManager.getWindows()) {
         if (window instanceof GroupWindow groupWindow && groupWindow.getSetting() == this.setting) {
            return true;
         }
      }

      return false;
   }
}
