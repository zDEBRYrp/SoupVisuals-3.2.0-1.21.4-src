package padej.soup.implement.menu.components.implement.other;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleDescription;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.menu.MenuScreen;
import padej.soup.implement.menu.components.AbstractComponent;

public class ModuleDescriptionComponent extends AbstractComponent {
   private Module hoveredModule = null;
   private Setting hoveredSetting = null;
   private String description = "";
   private boolean visible = false;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (!this.description.isEmpty() && this.visible) {
         int screenWidth = context.getScaledWindowWidth();
         int screenHeight = context.getScaledWindowHeight();
         float maxWidth = 300.0F;
         float lineHeight = 4.0F;
         int fontSize = 16;
         String[] lines = this.wrapText(this.description, maxWidth);
         float textWidth = 0.0F;

         for (String line : lines) {
            float lineWidth = Fonts.getSize(fontSize).getStringWidth(line);
            textWidth = Math.max(textWidth, lineWidth);
         }

         float padding = 4.0F;
         float boxWidth = textWidth + padding * 2.0F;
         float boxHeight = lines.length * lineHeight + padding * 2.0F;
         int menuX = MenuScreen.INSTANCE.x;
         int menuY = MenuScreen.INSTANCE.y;
         int menuWidth = MenuScreen.INSTANCE.width;
         float boxX = menuX + menuWidth / 2.0F - boxWidth / 2.0F;
         float topY = menuY - boxHeight - 10.0F;
         float bottomY = menuY + 238;
         float boxY;
         if (topY >= 10.0F) {
            boxY = topY;
         } else {
            boxY = bottomY;
         }

         boxX = Math.max(10.0F, Math.min(boxX, screenWidth - boxWidth - 10.0F));
         boxY = Math.max(10.0F, Math.min(boxY, screenHeight - boxHeight - 10.0F));
         MatrixStack matrices = context.getMatrices();
         rectangle.render(
            ShapeProperties.create(matrices, boxX, boxY, boxWidth, boxHeight)
               .round(4.0F)
               .softness(1.0F)
               .thickness(2.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(ColorUtil.getGuiRectColor(0.5F))
               .build()
         );
         int textColor = ColorUtil.getText();
         float textX = boxX + padding;
         float textY = boxY + padding;

         for (String line : lines) {
            Fonts.getSize(fontSize).drawString(matrices, line, textX, textY - 0.5F, textColor);
            textY += lineHeight;
         }
      }
   }

   public void setHoveredModule(Module module) {
      if (module != null) {
         String moduleDescription = ModuleDescription.getDescription(module);
         if (!moduleDescription.isEmpty()) {
            this.hoveredModule = module;
            this.hoveredSetting = null;
            this.description = moduleDescription;
            this.visible = true;
         } else {
            this.hoveredModule = null;
            this.hoveredSetting = null;
            this.description = "";
            this.visible = false;
         }
      } else {
         this.hoveredModule = null;
         this.hoveredSetting = null;
         this.description = "";
         this.visible = false;
      }
   }

   public void setHoveredSetting(Setting setting) {
      if (setting != null) {
         String settingDescription = setting.getLocalizedDescription();
         if (settingDescription != null && !settingDescription.isEmpty()) {
            this.hoveredModule = null;
            this.hoveredSetting = setting;
            this.description = settingDescription;
            this.visible = true;
         } else {
            this.hoveredModule = null;
            this.hoveredSetting = null;
            this.description = "";
            this.visible = false;
         }
      } else {
         this.hoveredModule = null;
         this.hoveredSetting = null;
         this.description = "";
         this.visible = false;
      }
   }

   public void setHoveredSettingDescription(String descriptionKey) {
      if (descriptionKey != null && !descriptionKey.isEmpty()) {
         String settingDescription = ModuleDescription.getDescription(descriptionKey);
         if (!settingDescription.isEmpty()) {
            this.hoveredModule = null;
            this.hoveredSetting = null;
            this.description = settingDescription;
            this.visible = true;
         } else {
            this.hoveredModule = null;
            this.hoveredSetting = null;
            this.description = "";
            this.visible = false;
         }
      } else {
         this.hoveredModule = null;
         this.hoveredSetting = null;
         this.description = "";
         this.visible = false;
      }
   }

   public void hide() {
      this.visible = false;
      this.hoveredModule = null;
      this.hoveredSetting = null;
      this.description = "";
   }

   private String[] wrapText(String text, float maxWidth) {
      if (text.isEmpty()) {
         return new String[0];
      } else {
         String[] words = text.split(" ");
         StringBuilder currentLine = new StringBuilder();
         List<String> lines = new ArrayList<>();

         for (String word : words) {
            String testLine = !currentLine.isEmpty() ? currentLine + " " + word : word;
            float lineWidth = Fonts.getSize(16).getStringWidth(testLine);
            if (lineWidth <= maxWidth) {
               currentLine = new StringBuilder(testLine);
            } else if (!currentLine.isEmpty()) {
               lines.add(currentLine.toString());
               currentLine = new StringBuilder(word);
            } else {
               lines.add(word);
            }
         }

         if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
         }

         return lines.toArray(new String[0]);
      }
   }

   public Module getHoveredModule() {
      return this.hoveredModule;
   }

   public Setting getHoveredSetting() {
      return this.hoveredSetting;
   }

   public String getDescription() {
      return this.description;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }
}
