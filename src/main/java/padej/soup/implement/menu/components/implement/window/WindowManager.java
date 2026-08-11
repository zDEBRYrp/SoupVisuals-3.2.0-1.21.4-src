package padej.soup.implement.menu.components.implement.window;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import padej.soup.implement.menu.MenuScreen;
import padej.soup.implement.menu.components.AbstractComponent;
import padej.soup.implement.menu.components.implement.other.ModuleDescriptionComponent;
import padej.soup.implement.menu.components.implement.settings.AbstractSettingComponent;
import padej.soup.implement.menu.components.implement.window.implement.settings.color.ColorWindow;
import padej.soup.implement.menu.components.implement.window.implement.settings.group.GroupWindow;

public class WindowManager extends AbstractComponent {
   public static final WindowManager INSTANCE = new WindowManager();
   private final List<AbstractWindow> windows = new ArrayList<>();

   public void add(AbstractWindow window) {
      this.windows.add(window);
   }

   public void delete(AbstractWindow window) {
      window.startCloseAnimation();
   }

   public boolean isMouseOverAnyWindow(double mouseX, double mouseY) {
      for (AbstractWindow window : this.windows) {
         if (window.isHovered(mouseX, mouseY)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      int i = 0;

      while (i < this.windows.size()) {
         AbstractWindow window = this.windows.get(i);
         window.render(context, mouseX, mouseY, delta);
         if (window.isCloseAnimationFinished()) {
            this.windows.remove(i);
         } else {
            i++;
         }
      }

      this.renderWindowHoverDescriptions(context, mouseX, mouseY, delta);
   }

   private void renderWindowHoverDescriptions(DrawContext context, int mouseX, int mouseY, float delta) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      ModuleDescriptionComponent descriptionComponent = menuScreen.getModuleDescriptionComponent();
      if (descriptionComponent != null) {
         boolean mouseOverWindow = false;

         for (int i = this.windows.size() - 1; i >= 0; i--) {
            AbstractWindow window = this.windows.get(i);
            if (window.isHovered(mouseX, mouseY)) {
               mouseOverWindow = true;
               this.handleWindowHoverDescriptions(window, descriptionComponent, mouseX, mouseY, context, delta);
               break;
            }
         }

         if (!mouseOverWindow) {
            descriptionComponent.hide();
         }
      }
   }

   private void handleWindowHoverDescriptions(
      AbstractWindow window, ModuleDescriptionComponent descriptionComponent, int mouseX, int mouseY, DrawContext context, float delta
   ) {
      if (window instanceof GroupWindow groupWindow) {
         for (AbstractSettingComponent settingComponent : groupWindow.getComponents()) {
            if (settingComponent.isHover(mouseX, mouseY)) {
               descriptionComponent.setHoveredSetting(settingComponent.getSetting());
               descriptionComponent.render(context, mouseX, mouseY, delta);
               return;
            }
         }
      }

      if (window instanceof ColorWindow colorWindow) {
         for (AbstractComponent component : colorWindow.getComponents()) {
            if (component.isHover(mouseX, mouseY)) {
               String componentDescription = this.getColorComponentDescription(component);
               if (componentDescription != null && !componentDescription.isEmpty()) {
                  descriptionComponent.setHoveredSettingDescription(componentDescription);
                  descriptionComponent.render(context, mouseX, mouseY, delta);
                  return;
               }
            }
         }
      }

      descriptionComponent.hide();
   }

   private String getColorComponentDescription(AbstractComponent component) {
      String className = component.getClass().getSimpleName();
      switch (className) {
         case "HueComponent":
            return "setting.color.hue.name";
         case "SaturationComponent":
            return "setting.color.saturation.name";
         case "AlphaComponent":
            return "setting.color.alpha.name";
         case "ColorEditorComponent":
            return "setting.color.editor.name";
         case "ColorPresetComponent":
            return "setting.color.preset.name";
         default:
            return null;
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      boolean clickedInsideWindow = false;

      for (int i = this.windows.size() - 1; i >= 0; i--) {
         AbstractWindow window = this.windows.get(i);
         if (window.isHovered(mouseX, mouseY)) {
            clickedInsideWindow = true;
            if (i != this.windows.size() - 1) {
               this.windows.remove(i);
               this.windows.add(window);
            }

            window.mouseClicked(mouseX, mouseY, button);
            break;
         }
      }

      if (clickedInsideWindow) {
         return true;
      } else {
         for (AbstractWindow window : this.windows) {
            window.startCloseAnimation();
         }

         return false;
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      for (AbstractWindow window : this.windows) {
         window.isHovered(mouseX, mouseY);
         if (window.isHover(mouseX, mouseY)) {
            return true;
         }
      }

      return super.isHover(mouseX, mouseY);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      for (AbstractWindow window : this.windows) {
         window.charTyped(chr, modifiers);
      }

      return super.charTyped(chr, modifiers);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      for (AbstractWindow window : this.windows) {
         if (window.mouseScrolled(mouseX, mouseY, amount)) {
            return true;
         }
      }

      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      for (AbstractWindow window : this.windows) {
         window.keyPressed(keyCode, scanCode, modifiers);
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      for (AbstractWindow window : this.windows) {
         window.keyReleased(keyCode, scanCode, modifiers);
      }

      return super.keyReleased(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      for (AbstractWindow window : this.windows) {
         window.mouseReleased(mouseX, mouseY, button);
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   public List<AbstractWindow> getWindows() {
      return this.windows;
   }
}
