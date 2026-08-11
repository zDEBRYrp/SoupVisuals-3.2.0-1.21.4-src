package padej.soup.implement.menu.components.implement.other;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import padej.soup.api.file.impl.ConfigFile;
import padej.soup.api.repository.config.Config;
import padej.soup.api.repository.config.ConfigUtils;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.render.ScissorManager;
import padej.soup.core.Main;
import padej.soup.implement.menu.components.AbstractComponent;

public class ConfigManagerComponent extends AbstractComponent {
   public static boolean typing = false;
   private String inputText = "";
   private int cursorPosition = 0;
   private long lastClickTime = 0L;
   private float xOffset = 0.0F;
   private String renamingConfig = null;
   private String editingConfigName = null;
   private String editingText = "";
   private int editingCursorPosition = 0;
   private float editingXOffset = 0.0F;
   private final Animation hoverAnimation = new DecelerateAnimation().setMs(200).setValue(1.0);
   private final Animation typingAnimation = new DecelerateAnimation().setMs(200).setValue(0.3F);
   private final Map<String, Animation> cardAppearAnimations = new HashMap<>();
   private final Map<String, Animation> editIconHoverAnimations = new HashMap<>();
   private final Map<String, Animation> deleteIconHoverAnimations = new HashMap<>();
   private final Map<String, Boolean> cardDisappearing = new HashMap<>();
   private double listScrollOffset = 0.0;
   private double smoothListScrollOffset = 0.0;
   private long lastConfigRefresh = 0L;
   private static final long CONFIG_REFRESH_INTERVAL = 3000L;
   private String[] lastScannedFiles = new String[0];
   private static final int HEADER_HEIGHT = 18;
   private static final int INPUT_HEIGHT = 15;
   private static final int CONFIG_ENTRY_HEIGHT = 14;
   private static final int CONFIG_ENTRY_SPACING = 2;
   private static final int PADDING = 9;
   private static final int MIN_CARD_HEIGHT = 130;
   private final ConfigFile configFile = new ConfigFile();

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      long currentTime = System.currentTimeMillis();
      if (currentTime - this.lastConfigRefresh > 3000L) {
         this.refreshConfigsOptimized();
         this.lastConfigRefresh = currentTime;
      }

      this.height = this.getComponentHeight();
      boolean isHovered = this.isHover(mouseX, mouseY);
      this.hoverAnimation.setDirection(isHovered ? Direction.FORWARDS : Direction.BACKWARDS);
      float scale = 1.0F + (this.hoverAnimation.getOutput().floatValue() - 1.0F) * 0.05F;
      matrix.push();
      matrix.translate(this.x + this.width / 2.0F, this.y + this.height / 2.0F, 0.0F);
      matrix.scale(scale, scale, 1.0F);
      matrix.translate(-(this.x + this.width / 2.0F), -(this.y + this.height / 2.0F), 0.0F);
      rectangle.render(
         ShapeProperties.create(matrix, this.x, this.y, this.width, 18.0).round(5.0F, 0.0F, 5.0F, 0.0F).color(ColorUtil.getGuiRectColor2(1.0F)).build()
      );
      rectangle.render(
         ShapeProperties.create(matrix, this.x, this.y, this.width, this.height)
            .round(5.0F)
            .softness(1.0F)
            .thickness(2.2F)
            .outlineColor(ColorUtil.getOutline(0.56F))
            .color(ColorUtil.getRectDarker(0.55F))
            .build()
      );
      Fonts.getSize(14, Fonts.Type.INTER_BOLD)
         .drawCenteredString(matrix, LocalizationManager.getInstance().get("ui.config_manager"), this.x + this.width / 2.0F, this.y + 7.0F, ColorUtil.getText());
      this.renderInputField(context, mouseX, mouseY);
      this.renderConfigsList(context, mouseX, mouseY);
      matrix.pop();
   }

   private void renderInputField(DrawContext context, int mouseX, int mouseY) {
      MatrixStack matrix = context.getMatrices();
      float inputY = this.y + 18.0F + 9.0F;
      boolean inputHovered = MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, inputY, this.width - 18.0F, 15.0);
      this.typingAnimation.setDirection(!typing && !inputHovered ? Direction.BACKWARDS : Direction.FORWARDS);
      float typingAnim = this.typingAnimation.getOutput().floatValue();
      rectangle.render(
         ShapeProperties.create(matrix, this.x + 9.0F, inputY, this.width - 18.0F, 15.0)
            .round(2.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getGuiRectColor(0.5F + typingAnim * 0.3F))
            .build()
      );
      FontRenderer font = Fonts.getSize(13);
      this.updateXOffset(font, this.cursorPosition);
      String displayText = this.inputText.equalsIgnoreCase("") && !typing ? LocalizationManager.getInstance().get("ui.config.placeholder") : this.inputText;
      int textColor = this.inputText.isEmpty() && !typing ? ColorUtil.getDescription() : ColorUtil.getText();
      ScissorManager scissorManager = Main.getInstance().getScissorManager();
      scissorManager.push(renderMatrix, this.x + 9.0F + 3.0F, inputY, this.width - 18.0F - 6.0F, 15.0F);
      font.drawString(matrix, displayText, this.x + 9.0F + 3.0F - this.xOffset, inputY + 6.0F, textColor);
      long currentTime = System.currentTimeMillis();
      boolean focused = typing && currentTime % 1000L < 500L;
      if (focused) {
         float cursorX = font.getStringWidth(this.inputText.substring(0, this.cursorPosition));
         rectangle.render(ShapeProperties.create(matrix, this.x + 9.0F + 3.0F - this.xOffset + cursorX, inputY + 2.0F, 0.5, 9.0).color(-1).build());
      }

      scissorManager.pop();
      String instructionText = this.inputText.isEmpty()
         ? LocalizationManager.getInstance().get("ui.config.instruction_empty")
         : (
            ConfigUtils.isConfig(this.inputText.trim())
               ? LocalizationManager.getInstance().get("ui.config.instruction_exists")
               : LocalizationManager.getInstance().get("ui.config.instruction_new")
         );
      Fonts.getSize(10).drawCenteredString(matrix, instructionText, this.x + this.width / 2.0F, inputY + 15.0F + 4.0F, ColorUtil.getDescription());
   }

   private void renderConfigsList(DrawContext context, int mouseX, int mouseY) {
      List<Config> configs = ConfigUtils.searchConfigs(this.inputText);
      MatrixStack matrix = context.getMatrices();
      ScissorManager scissorManager = Main.getInstance().getScissorManager();
      if (configs.isEmpty()) {
         float emptyY = this.y + 18.0F + 15.0F + 18.0F + 14.0F + 15.0F;
         if (this.inputText.trim().isEmpty()) {
            Fonts.getSize(12)
               .drawCenteredString(
                  matrix, LocalizationManager.getInstance().get("ui.config.empty_title"), this.x + this.width / 2.0F, emptyY, ColorUtil.getDescription()
               );
            Fonts.getSize(10)
               .drawCenteredString(
                  matrix,
                  LocalizationManager.getInstance().get("ui.config.empty_subtitle"),
                  this.x + this.width / 2.0F,
                  emptyY + 12.0F,
                  ColorUtil.getDescription(0.4F)
               );
         } else {
            Fonts.getSize(12)
               .drawCenteredString(
                  matrix, LocalizationManager.getInstance().get("ui.config.no_match_title"), this.x + this.width / 2.0F, emptyY, ColorUtil.getDescription()
               );
            Fonts.getSize(10)
               .drawCenteredString(
                  matrix,
                  LocalizationManager.getInstance().get("ui.config.no_match_subtitle"),
                  this.x + this.width / 2.0F,
                  emptyY + 12.0F,
                  ColorUtil.getDescription(0.4F)
               );
         }
      } else {
         float listStartY = this.y + 18.0F + 15.0F + 18.0F + 14.0F;
         float listHeight = this.height - 74.0F - 5.0F;
         int maxScroll = this.getScroll(configs, (int)listHeight);
         this.listScrollOffset = MathHelper.clamp(this.listScrollOffset, 0.0, maxScroll);
         this.smoothListScrollOffset = MathUtil.interpolateSmooth(4.0, this.smoothListScrollOffset, this.listScrollOffset);
         scissorManager.push(renderMatrix, this.x + 9.0F, listStartY, this.width - 18.0F, listHeight);
         float currentY = listStartY - (float)this.smoothListScrollOffset;

         for (int i = 0; i < configs.size(); i++) {
            Config config = configs.get(i);
            float entryY = currentY + i * 16;
            if (!(entryY + 14.0F < listStartY) && !(entryY > listStartY + listHeight)) {
               boolean withinScrollableArea = mouseX >= this.x + 9.0F
                  && mouseX <= this.x + this.width - 9.0F
                  && mouseY >= listStartY
                  && mouseY <= listStartY + listHeight;
               boolean entryHovered = withinScrollableArea && mouseY >= entryY && mouseY <= entryY + 14.0F;
               String configKey = config.getName() + "_" + i;
               Animation appearAnimation = this.cardAppearAnimations.computeIfAbsent(configKey, k -> new DecelerateAnimation().setMs(300).setValue(1.0));
               float animValue = appearAnimation.getOutput().floatValue();
               boolean isDisappearing = this.cardDisappearing.getOrDefault(configKey, false);
               float effectiveValue = isDisappearing ? 1.0F - animValue : animValue;
               float scale = 0.8F + effectiveValue * 0.2F;
               int alpha = (int)(255.0F * effectiveValue);
               float cardCenterX = this.x + 9.0F + (this.width - 18.0F) / 2.0F;
               float cardCenterY = entryY + 7.0F;
               int finalI = i;
               MathUtil.scale(
                  matrix,
                  cardCenterX,
                  cardCenterY,
                  scale,
                  () -> {
                     int bgColor = alpha << 24 | ColorUtil.getGuiRectColor(0.4F) & 16777215;
                     int outlineColor = alpha << 24 | (entryHovered ? ColorUtil.getClientColor(0.8F) : ColorUtil.getOutline()) & 16777215;
                     rectangle.render(
                        ShapeProperties.create(matrix, this.x + 9.0F, entryY, this.width - 18.0F, 14.0)
                           .round(2.0F)
                           .thickness(2.0F)
                           .outlineColor(outlineColor)
                           .color(bgColor)
                           .build()
                     );
                     boolean isActiveConfig = ConfigUtils.isActiveConfig(config.getName());
                     int baseTextColor = isActiveConfig ? ColorUtil.getClientColor() : ColorUtil.getText();
                     int textColor = alpha << 24 | baseTextColor & 16777215;
                     String configName = config.getName();
                     String activeIndicator = isActiveConfig ? LocalizationManager.getInstance().get("ui.config.active_indicator") : "";
                     if (this.editingConfigName != null && this.editingConfigName.equals(configName)) {
                        this.renderInlineEditField(context, matrix, this.x + 9.0F + 5.0F, entryY + 2.0F, this.width - 18.0F - 50.0F);
                     } else {
                        String displayName;
                        if (configName.length() > 13) {
                           displayName = configName.substring(0, 13) + "..." + activeIndicator;
                        } else {
                           displayName = configName + activeIndicator;
                        }

                        Fonts.getSize(12, Fonts.Type.INTER_BOLD).drawString(matrix, displayName, this.x + 9.0F + 5.0F, entryY + 6.0F, textColor);
                     }

                     if (isActiveConfig) {
                        float dotSize = 4.0F;
                        float dotX = this.x + this.width - 9.0F - 44.0F;
                        float dotY = entryY + 7.0F - dotSize / 2.0F;
                        int dotColor = alpha << 24 | ColorUtil.getClientColor() & 16777215;
                        rectangle.render(ShapeProperties.create(matrix, dotX, dotY, dotSize, dotSize).round(dotSize / 2.0F).color(dotColor).build());
                     }

                     float iconSize = 8.4F;
                     float editX = this.x + this.width - 9.0F - iconSize * 2.0F - 8.0F;
                     float deleteX = this.x + this.width - 9.0F - iconSize - 4.0F;
                     float iconY = entryY + (14.0F - iconSize) / 2.0F;
                     boolean editIconHovered = MathUtil.isHovered(mouseX, mouseY, editX, iconY, iconSize, iconSize);
                     boolean deleteIconHovered = MathUtil.isHovered(mouseX, mouseY, deleteX, iconY, iconSize, iconSize);
                     String editAnimKey = config.getName() + "_" + finalI + "_edit";
                     Animation editHoverAnim = this.editIconHoverAnimations
                        .computeIfAbsent(editAnimKey, k -> new DecelerateAnimation().setMs(150).setValue(1.0));
                     editHoverAnim.setDirection(editIconHovered ? Direction.BACKWARDS : Direction.FORWARDS);
                     float editAnimValue = editHoverAnim.getOutput().floatValue();
                     int greyColor = ColorUtil.getDescription();
                     int clientColor = ColorUtil.getClientColor();
                     int editColor = this.interpolateColor(clientColor, greyColor, editAnimValue);
                     int editIconColor = alpha << 24 | editColor & 16777215;
                     image.setIcon(61445).render(ShapeProperties.create(matrix, editX, iconY, iconSize, iconSize).color(editIconColor).build());
                     boolean canDelete = ConfigUtils.canDeleteConfig();
                     String deleteAnimKey = config.getName() + "_" + finalI + "_delete";
                     Animation deleteHoverAnim = this.deleteIconHoverAnimations
                        .computeIfAbsent(deleteAnimKey, k -> new DecelerateAnimation().setMs(150).setValue(1.0));
                     deleteHoverAnim.setDirection(deleteIconHovered && canDelete ? Direction.BACKWARDS : Direction.FORWARDS);
                     float deleteAnimValue = deleteHoverAnim.getOutput().floatValue();
                     int disabledColor = ColorUtil.getDescription(0.27F);
                     int deleteBaseColor = canDelete ? greyColor : disabledColor;
                     int deleteTargetColor = canDelete ? clientColor : disabledColor;
                     int deleteColor = this.interpolateColor(deleteTargetColor, deleteBaseColor, deleteAnimValue);
                     int deleteIconColor = alpha << 24 | deleteColor & 16777215;
                     image.setIcon(61459).render(ShapeProperties.create(matrix, deleteX, iconY, iconSize, iconSize).color(deleteIconColor).build());
                  }
               );
            }
         }

         this.cardAppearAnimations.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            Animation animation = entry.getValue();
            boolean isDisappearingx = this.cardDisappearing.getOrDefault(key, false);
            if (isDisappearingx && animation.getOutput().floatValue() <= 0.01F) {
               this.cardDisappearing.remove(key);
               this.editIconHoverAnimations.remove(key + "_edit");
               this.deleteIconHoverAnimations.remove(key + "_delete");
               return true;
            } else {
               return false;
            }
         });
         scissorManager.pop();
      }
   }

   private int getScroll(List<Config> configs, int listHeight) {
      int totalContentHeight = configs.size() * 16;
      return Math.max(0, totalContentHeight - listHeight);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         float inputY = this.y + 18.0F + 9.0F;
         if (MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, inputY, this.width - 18.0F, 15.0)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastClickTime < 250L) {
               this.cursorPosition = this.inputText.length();
            } else {
               SearchComponent.typing = false;
               FriendsListComponent.typing = false;
               typing = true;
               this.lastClickTime = currentTime;
               this.cursorPosition = this.getCursorIndexAt(mouseX);
            }

            return true;
         } else {
            List<Config> configs = ConfigUtils.searchConfigs(this.inputText);
            if (!configs.isEmpty()) {
               float listStartY = this.y + 18.0F + 15.0F + 18.0F + 14.0F;
               float listHeight = this.height - 74.0F;
               if (MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, listStartY, this.width - 18.0F, listHeight)) {
                  float currentY = listStartY - (float)this.smoothListScrollOffset;

                  for (int i = 0; i < configs.size(); i++) {
                     Config config = configs.get(i);
                     float entryY = currentY + i * 16;
                     if (!(entryY + 14.0F < listStartY) && !(entryY > listStartY + listHeight)) {
                        float iconSize = 8.4F;
                        float editX = this.x + this.width - 9.0F - iconSize * 2.0F - 8.0F;
                        float deleteX = this.x + this.width - 9.0F - iconSize - 4.0F;
                        float iconY = entryY + (14.0F - iconSize) / 2.0F;
                        if (mouseX >= editX - 2.0F && mouseX <= editX + iconSize + 2.0F && mouseY >= iconY - 2.0F && mouseY <= iconY + iconSize + 2.0F) {
                           this.startRenaming(config.getName());
                           return true;
                        }

                        if (ConfigUtils.canDeleteConfig()
                           && mouseX >= deleteX - 2.0F
                           && mouseX <= deleteX + iconSize + 2.0F
                           && mouseY >= iconY - 2.0F
                           && mouseY <= iconY + iconSize + 2.0F) {
                           this.removeConfig(config.getName());
                           return true;
                        }

                        if (mouseY >= entryY && mouseY <= entryY + 14.0F && mouseX < editX - 5.0F) {
                           if (this.editingConfigName != null && this.editingConfigName.equals(config.getName())) {
                              float fieldX = this.x + 9.0F + 5.0F;
                              float relativeX = (float)mouseX - fieldX - 3.0F + this.editingXOffset;
                              FontRenderer font = Fonts.getSize(12, Fonts.Type.INTER_BOLD);

                              int position;
                              for (position = 0; position < this.editingText.length(); position++) {
                                 float textWidth = font.getStringWidth(this.editingText.substring(0, position + 1));
                                 if (textWidth > relativeX) {
                                    break;
                                 }
                              }

                              this.editingCursorPosition = position;
                           } else if (!ConfigUtils.isActiveConfig(config.getName())) {
                              this.loadConfig(config.getName());
                           }

                           return true;
                        }
                     }
                  }

                  return true;
               }
            }

            typing = false;
            if (this.editingConfigName != null) {
               this.editingConfigName = null;
               this.editingText = "";
            }

            return super.mouseClicked(mouseX, mouseY, button);
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      List<Config> configs = ConfigUtils.searchConfigs(this.inputText);
      if (configs.isEmpty()) {
         return super.mouseScrolled(mouseX, mouseY, amount);
      } else {
         float listStartY = this.y + 18.0F + 15.0F + 18.0F + 14.0F;
         float listHeight = this.height - 74.0F;
         if (MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, listStartY, this.width - 18.0F, listHeight)) {
            int totalContentHeight = configs.size() * 16;
            int maxScroll = Math.max(0, totalContentHeight - (int)listHeight);
            if (maxScroll > 0) {
               this.listScrollOffset = MathHelper.clamp(this.listScrollOffset - amount * 15.0, 0.0, maxScroll);
               return true;
            }
         }

         return super.mouseScrolled(mouseX, mouseY, amount);
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.editingConfigName != null) {
         switch (keyCode) {
            case 256:
               this.editingConfigName = null;
               this.editingText = "";
               return true;
            case 257:
               if (!this.editingText.trim().isEmpty()) {
                  this.renameConfig(this.editingConfigName, this.editingText.trim());
               }

               this.editingConfigName = null;
               this.editingText = "";
               return true;
            case 258:
            case 260:
            case 261:
            default:
               break;
            case 259:
               if (this.editingCursorPosition > 0) {
                  this.editingText = this.editingText.substring(0, this.editingCursorPosition - 1) + this.editingText.substring(this.editingCursorPosition);
                  this.editingCursorPosition--;
               }

               return true;
            case 262:
               if (this.editingCursorPosition < this.editingText.length()) {
                  this.editingCursorPosition++;
               }

               return true;
            case 263:
               if (this.editingCursorPosition > 0) {
                  this.editingCursorPosition--;
               }

               return true;
         }
      } else if (typing) {
         switch (keyCode) {
            case 257:
            case 259:
               this.handleTextModification(keyCode);
            case 258:
            case 260:
            case 261:
            default:
               break;
            case 262:
            case 263:
               this.moveCursor(keyCode);
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.editingConfigName != null && chr >= ' ') {
         this.editingText = this.editingText.substring(0, this.editingCursorPosition) + chr + this.editingText.substring(this.editingCursorPosition);
         this.editingCursorPosition++;
         return true;
      } else if (typing && Fonts.getSize(13).getStringWidth(this.inputText) < this.width - 18.0F - 7.0F && chr >= ' ') {
         this.inputText = this.inputText.substring(0, this.cursorPosition) + chr + this.inputText.substring(this.cursorPosition);
         this.cursorPosition++;
         return true;
      } else {
         return false;
      }
   }

   private void saveCurrentConfig(String configName) {
      try {
         this.configFile.saveConfig(configName);
         ConfigUtils.setActiveConfig(configName);
      } catch (Exception var3) {
         LoggerUtil.error("Failed to save current config: " + configName, var3);
      }
   }

   private void loadConfig(String configName) {
      try {
         String currentActiveConfig = ConfigUtils.getActiveConfigName();
         if (currentActiveConfig != null && ConfigUtils.isConfig(currentActiveConfig)) {
            this.configFile.saveConfig(currentActiveConfig);
         }

         this.configFile.loadConfig(configName);
         ConfigUtils.setActiveConfig(configName);
      } catch (Exception var3) {
         LoggerUtil.error("Failed to load config: " + configName, var3);
      }
   }

   private void removeConfig(String configName) {
      if (ConfigUtils.canDeleteConfig()) {
         if (ConfigUtils.isActiveConfig(configName)) {
            String nextConfig = ConfigUtils.getNextConfigForDeletion(configName);
            if (nextConfig != null) {
               this.loadConfig(nextConfig);
            }
         }

         this.cardAppearAnimations.forEach((key, animation) -> {
            if (key.startsWith(configName + "_")) {
               this.cardDisappearing.put(key, true);
               animation.setDirection(Direction.BACKWARDS);
            }
         });

         try {
            this.configFile.deleteConfig(configName);
         } catch (Exception var3) {
            LoggerUtil.error("Failed to remove config: " + configName, var3);
         }
      }
   }

   public void saveActiveConfigIfExists() {
      try {
         String activeConfigName = ConfigUtils.getActiveConfigName();
         if (activeConfigName != null && ConfigUtils.isConfig(activeConfigName)) {
            this.configFile.saveConfig(activeConfigName);
         }
      } catch (Exception var2) {
         LoggerUtil.error("Failed to save active config", var2);
      }
   }

   public int getComponentHeight() {
      List<Config> configs = ConfigUtils.searchConfigs(this.inputText);
      int baseHeight = 20;
      if (configs.isEmpty()) {
         int emptyHeight = baseHeight + 50;
         return Math.max(emptyHeight, 130);
      } else {
         int configsListHeight = Math.min(configs.size() * 16, 120);
         int totalHeight = baseHeight + configsListHeight + 10;
         return Math.max(totalHeight, 130);
      }
   }

   private void handleTextModification(int keyCode) {
      if (keyCode == 259) {
         if (this.cursorPosition > 0) {
            this.inputText = this.inputText.substring(0, this.cursorPosition - 1) + this.inputText.substring(this.cursorPosition);
            this.cursorPosition--;
         }
      } else if (keyCode == 257) {
         this.handleEnterKey();
      }
   }

   private void handleEnterKey() {
      String configName = this.inputText.trim();
      if (!configName.isEmpty()) {
         if (this.renamingConfig != null) {
            this.renameConfig(this.renamingConfig, configName);
            this.renamingConfig = null;
         } else if (ConfigUtils.isConfig(configName)) {
            this.loadConfig(configName);
         } else {
            this.saveCurrentConfig(configName);
         }

         this.inputText = "";
         this.cursorPosition = 0;
         typing = false;
      }
   }

   private void moveCursor(int keyCode) {
      if (keyCode == 263 && this.cursorPosition > 0) {
         this.cursorPosition--;
      } else if (keyCode == 262 && this.cursorPosition < this.inputText.length()) {
         this.cursorPosition++;
      }
   }

   private int getCursorIndexAt(double mouseX) {
      FontRenderer font = Fonts.getSize(13);
      float relativeX = (float)mouseX - this.x - 9.0F - 3.0F + this.xOffset;

      int position;
      for (position = 0; position < this.inputText.length(); position++) {
         float textWidth = font.getStringWidth(this.inputText.substring(0, position + 1));
         if (textWidth > relativeX) {
            break;
         }
      }

      return position;
   }

   private void updateXOffset(FontRenderer font, int cursorPosition) {
      float cursorX = font.getStringWidth(this.inputText.substring(0, cursorPosition));
      if (cursorX < this.xOffset) {
         this.xOffset = cursorX;
      } else if (cursorX - this.xOffset > this.width - 18.0F - 7.0F) {
         this.xOffset = cursorX - (this.width - 18.0F - 7.0F);
      }
   }

   private void updateEditingXOffset(FontRenderer font, int cursorPosition) {
      float cursorX = font.getStringWidth(this.editingText.substring(0, cursorPosition));
      float fieldWidth = this.width - 18.0F - 50.0F - 6.0F;
      if (cursorX < this.editingXOffset) {
         this.editingXOffset = cursorX;
      } else if (cursorX - this.editingXOffset > fieldWidth) {
         this.editingXOffset = cursorX - fieldWidth;
      }
   }

   private void renderInlineEditField(DrawContext context, MatrixStack matrix, float fieldX, float fieldY, float fieldWidth) {
      FontRenderer font = Fonts.getSize(12, Fonts.Type.INTER_BOLD);
      this.updateEditingXOffset(font, this.editingCursorPosition);
      ScissorManager scissorManager = Main.getInstance().getScissorManager();
      scissorManager.push(renderMatrix, fieldX + 2.0F, fieldY, fieldWidth - 4.0F, 10.0F);
      font.drawString(matrix, this.editingText, fieldX + 3.0F - this.editingXOffset, fieldY + 4.0F, -1);
      long currentTime = System.currentTimeMillis();
      boolean focused = currentTime % 1000L < 500L;
      if (focused) {
         float cursorX = font.getStringWidth(this.editingText.substring(0, this.editingCursorPosition));
         rectangle.render(ShapeProperties.create(matrix, fieldX + 3.0F - this.editingXOffset + cursorX, fieldY + 1.0F, 0.5, 8.0).color(-1).build());
      }

      scissorManager.pop();
   }

   private void startRenaming(String configName) {
      this.editingConfigName = configName;
      this.editingText = configName;
      this.editingCursorPosition = configName.length();
      this.editingXOffset = 0.0F;
      SearchComponent.typing = false;
      FriendsListComponent.typing = false;
      typing = false;
   }

   private void renameConfig(String oldName, String newName) {
      try {
         if (ConfigUtils.isConfig(newName)) {
            LoggerUtil.warn("Config with name " + newName + " already exists");
            return;
         }

         Config oldConfig = ConfigUtils.getConfig(oldName);
         if (oldConfig == null) {
            LoggerUtil.warn("Config not found: " + oldName);
            return;
         }

         File configDir = ConfigUtils.getConfigDirectory();
         File oldFile = new File(configDir, oldConfig.getFileName());
         String newFileName = newName.toLowerCase() + ".soup";
         File newFile = new File(configDir, newFileName);
         if (!oldFile.exists()) {
            LoggerUtil.warn("Config file not found: " + oldConfig.getFileName());
            return;
         }

         if (newFile.exists()) {
            LoggerUtil.error("Config file with name '" + newFileName + "' already exists, cannot rename");
            return;
         }

         LoggerUtil.info("Renaming: " + oldFile.getAbsolutePath() + " -> " + newFile.getAbsolutePath());
         boolean renamed = false;

         try {
            try {
               Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
               LoggerUtil.info("File renamed successfully using atomic move");
               renamed = true;
            } catch (AtomicMoveNotSupportedException var13) {
               LoggerUtil.info("Atomic move not supported, using regular move");
               Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
               LoggerUtil.info("File renamed successfully using regular move");
               renamed = true;
            }
         } catch (IOException var15) {
            LoggerUtil.warn("Failed to move file, trying copy+delete approach: " + var15.getMessage());

            try {
               Files.copy(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
               LoggerUtil.info("File copied successfully");
               if (!newFile.exists()) {
                  LoggerUtil.error("New file was not created!");
                  return;
               }

               int attempts;
               for (attempts = 0; attempts < 5 && oldFile.exists(); attempts++) {
                  if (oldFile.delete()) {
                     LoggerUtil.info("Old file deleted successfully");
                     renamed = true;
                     break;
                  }

                  try {
                     Thread.sleep(100L);
                  } catch (InterruptedException var12) {
                     Thread.currentThread().interrupt();
                  }
               }

               if (!renamed) {
                  LoggerUtil.error("Failed to delete old config file after " + attempts + " attempts: " + oldFile.getName());
                  newFile.delete();
               }
            } catch (IOException var14) {
               LoggerUtil.error("Failed to copy config file during rename", var14);
               if (newFile.exists()) {
                  newFile.delete();
               }
            }
         }

         if (renamed) {
            boolean wasActive = ConfigUtils.isActiveConfig(oldName);
            ConfigUtils.removeConfig(oldConfig);
            String capitalizedNewName = newName;
            if (!newName.isEmpty()) {
               capitalizedNewName = newName.substring(0, 1).toUpperCase() + newName.substring(1);
            }

            Config newConfig = new Config(capitalizedNewName, oldConfig.getDescription(), newFileName, newFile.lastModified());
            ConfigUtils.addConfig(newConfig);
            if (wasActive) {
               if (ConfigUtils.getConfigManager() != null) {
                  ConfigUtils.getConfigManager().setCurrentConfig(newFile);
                  ConfigUtils.getConfigManager().saveCurrentConfig();
               }

               ConfigUtils.setActiveConfigName(capitalizedNewName);
            }

            LoggerUtil.info("Successfully renamed config from '" + oldName + "' to '" + capitalizedNewName + "'");
         } else {
            LoggerUtil.error("Failed to rename config file from " + oldConfig.getFileName() + " to " + newFileName);
         }
      } catch (Exception var16) {
         LoggerUtil.error("Failed to rename config from " + oldName + " to " + newName, var16);
      }
   }

   private int interpolateColor(int startColor, int endColor, float progress) {
      int startA = startColor >> 24 & 0xFF;
      int startR = startColor >> 16 & 0xFF;
      int startG = startColor >> 8 & 0xFF;
      int startB = startColor & 0xFF;
      int endA = endColor >> 24 & 0xFF;
      int endR = endColor >> 16 & 0xFF;
      int endG = endColor >> 8 & 0xFF;
      int endB = endColor & 0xFF;
      int a = (int)(startA + (endA - startA) * progress);
      int r = (int)(startR + (endR - startR) * progress);
      int g = (int)(startG + (endG - startG) * progress);
      int b = (int)(startB + (endB - startB) * progress);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private void refreshConfigsOptimized() {
      try {
         File configDir = ConfigUtils.getConfigDirectory();
         if (!configDir.exists()) {
            return;
         }

         File[] configFiles = configDir.listFiles((dir, name) -> name.endsWith(".soup"));
         String[] currentFiles = new String[0];
         if (configFiles != null) {
            currentFiles = new String[configFiles.length];

            for (int i = 0; i < configFiles.length; i++) {
               currentFiles[i] = configFiles[i].getName();
            }

            Arrays.sort((Object[])currentFiles);
         }

         if (!Arrays.equals((Object[])this.lastScannedFiles, (Object[])currentFiles)) {
            ConfigUtils.loadConfigsFromDirectory();
            this.lastScannedFiles = currentFiles;
         }
      } catch (Exception var5) {
         LoggerUtil.warn("Failed to refresh configs, will retry later", var5);
      }
   }
}
