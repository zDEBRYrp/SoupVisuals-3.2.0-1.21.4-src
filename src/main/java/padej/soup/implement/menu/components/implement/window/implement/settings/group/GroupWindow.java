package padej.soup.implement.menu.components.implement.window.implement.settings.group;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.SettingComponentAdder;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.render.ScissorManager;
import padej.soup.core.Main;
import padej.soup.implement.menu.components.AbstractComponent;
import padej.soup.implement.menu.components.implement.settings.AbstractSettingComponent;
import padej.soup.implement.menu.components.implement.window.AbstractWindow;

public class GroupWindow extends AbstractWindow {
   private final List<AbstractSettingComponent> components = new ArrayList<>();
   private final GroupSetting setting;
   private int cachedComponentHeight = 0;

   public GroupWindow(GroupSetting setting) {
      this.setting = setting;
      new SettingComponentAdder().addSettingComponent(setting.getSubSettings(), this.components);
   }

   @Override
   public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      ScissorManager scissorManager = Main.getInstance().getScissorManager();
      this.cachedComponentHeight = this.calculateComponentHeight();
      this.height = MathHelper.clamp(this.cachedComponentHeight, 0, 200);
      rectangle.render(
         ShapeProperties.create(matrix, this.x, this.y, this.width, this.height)
            .round(4.0F)
            .thickness(2.0F)
            .softness(1.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getGuiRectColor(1.0F))
            .build()
      );
      Fonts.getSize(15, Fonts.Type.INTER_BOLD)
         .drawString(context.getMatrices(), this.setting.getLocalizedName(), this.x + 9.0F, this.y + 10.0F, ColorUtil.getText());
      boolean isLimitedHeight = MathHelper.clamp(this.height, 0.0F, 200.0F) == 200.0F;
      if (isLimitedHeight) {
         scissorManager.push(renderMatrix, this.x, this.y + 23.0F, this.width, this.height - 28.0F);
      }

      float offset = 0.0F;
      int totalHeight = 0;

      for (int i = this.components.size() - 1; i >= 0; i--) {
         AbstractSettingComponent component = this.components.get(i);
         Supplier<Boolean> visible = component.getSetting().getVisible();
         if (visible == null || visible.get()) {
            component.x = this.x;
            component.y = (float)(this.y + 19.0F + offset + (this.height - 25.0F - component.height) + this.smoothedScroll);
            component.width = this.width;
            component.render(context, mouseX, mouseY, delta);
            offset -= component.height;
            totalHeight += (int)component.height;
         }
      }

      if (isLimitedHeight) {
         scissorManager.pop();
      }

      int maxScroll = (int)Math.max(0.0F, totalHeight - (this.height - 23.0F));
      this.scroll = MathHelper.clamp(this.scroll, -maxScroll, 0.0);
      this.smoothedScroll = MathHelper.lerp(0.1F, this.smoothedScroll, this.scroll);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, 19.0) && button == 1) {
         this.closeChildGroupWindows(this.setting);
         windowManager.delete(this);
         return true;
      } else {
         this.draggable(MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, 19.0) && button == 0);
         boolean isAnyComponentHovered = this.components.stream().anyMatch(abstractComponent -> abstractComponent.isHover(mouseX, mouseY));
         if (isAnyComponentHovered) {
            this.closeChildGroupWindows(this.setting);

            for (AbstractSettingComponent component : this.components) {
               if (component.isHover(mouseX, mouseY) && component.mouseClicked(mouseX, mouseY, button)) {
                  return true;
               }
            }

            return super.mouseClicked(mouseX, mouseY, button);
         } else {
            this.components.forEach(abstractComponent -> abstractComponent.mouseClicked(mouseX, mouseY, button));
            return super.mouseClicked(mouseX, mouseY, button);
         }
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

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      this.components.forEach(abstractComponentx -> abstractComponentx.isHover(mouseX, mouseY));

      for (AbstractComponent abstractComponent : this.components) {
         if (abstractComponent.isHover(mouseX, mouseY)) {
            return true;
         }
      }

      return super.isHover(mouseX, mouseY);
   }

   @Override
   public boolean isHovered(double mouseX, double mouseY) {
      for (AbstractComponent abstractComponent : this.components) {
         if (abstractComponent.isHover(mouseX, mouseY)) {
            return true;
         }
      }

      return super.isHovered(mouseX, mouseY);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.components.forEach(abstractComponent -> abstractComponent.mouseReleased(mouseX, mouseY, button));
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      boolean scrolled = MathHelper.clamp(this.height, 0.0F, 200.0F) == 200.0F && MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
      if (scrolled) {
         this.scroll += amount * 20.0;
      }

      this.components.forEach(abstractComponent -> abstractComponent.mouseScrolled(mouseX, mouseY, amount));
      return scrolled;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.components.forEach(abstractComponent -> abstractComponent.keyPressed(keyCode, scanCode, modifiers));
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      this.components.forEach(abstractComponent -> abstractComponent.charTyped(chr, modifiers));
      return super.charTyped(chr, modifiers);
   }

   private int calculateComponentHeight() {
      float offsetY = 0.0F;

      for (AbstractSettingComponent component : this.components) {
         Supplier<Boolean> visible = component.getSetting().getVisible();
         if (visible == null || visible.get()) {
            offsetY += component.height;
         }
      }

      return (int)(offsetY + 25.0F);
   }

   public int getComponentHeight() {
      return this.cachedComponentHeight;
   }

   public List<AbstractSettingComponent> getComponents() {
      return this.components;
   }

   public GroupSetting getSetting() {
      return this.setting;
   }

   public int getCachedComponentHeight() {
      return this.cachedComponentHeight;
   }
}
