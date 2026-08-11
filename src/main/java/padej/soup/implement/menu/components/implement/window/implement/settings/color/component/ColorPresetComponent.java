package padej.soup.implement.menu.components.implement.window.implement.settings.color.component;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.implement.menu.components.AbstractComponent;
import padej.soup.implement.menu.components.implement.window.implement.settings.color.ColorPresetButton;

public class ColorPresetComponent extends AbstractComponent {
   private final List<ColorPresetButton> colorPresetButtonList = new ArrayList<>();
   private final ColorSetting setting;
   private float windowHeight;

   public ColorPresetComponent(ColorSetting setting) {
      this.setting = setting;
      if (setting.getPresets() != null) {
         for (int preset : setting.getPresets()) {
            this.colorPresetButtonList.add(new ColorPresetButton(setting, preset));
         }
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (!this.colorPresetButtonList.isEmpty()) {
         Fonts.getSize(11).drawString(context.getMatrices(), LocalizationManager.getInstance().get("ui.prepared_swatches"), this.x + 6.0F, this.y + 112.0F, -1);
      }

      int xOffset = 0;
      int yOffset = 0;
      int colorIndex = 0;
      int size = 13;

      for (ColorPresetButton button : this.colorPresetButtonList) {
         button.x = this.x + 6.0F + xOffset;
         button.y = this.y + 117.0F + yOffset;
         button.render(context, mouseX, mouseY, delta);
         xOffset += size;
         if (++colorIndex >= 11) {
            colorIndex = 0;
            xOffset = 0;
            yOffset += size - 1;
         }
      }

      this.windowHeight = this.colorPresetButtonList.isEmpty() ? 132.0F : 156 + yOffset - yOffset / 2.0F;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.colorPresetButtonList.forEach(colorPresetButton -> colorPresetButton.mouseClicked(mouseX, mouseY, button));
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public List<ColorPresetButton> getColorPresetButtonList() {
      return this.colorPresetButtonList;
   }

   public ColorSetting getSetting() {
      return this.setting;
   }

   public float getWindowHeight() {
      return this.windowHeight;
   }
}
