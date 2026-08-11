package padej.soup.implement.features.draggables;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.feature.module.Category;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.LinearAnimation;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.core.Main;

public class ModulesList extends AbstractDraggable {
   private final Map<Module, Boolean> moduleEnabledStates = new HashMap<>();
   private final Map<String, Animation> categoryAnimations = new HashMap<>();
   private final Map<Module, Float> targetYPositions = new HashMap<>();
   private final Map<Module, Float> currentYPositions = new HashMap<>();
   private final Map<String, Float> categoryTargetYPositions = new HashMap<>();
   private final Map<String, Float> categoryCurrentYPositions = new HashMap<>();
   private ArrayList<ModulesList.ModuleEntry> sortedModules = new ArrayList<>();
   private boolean positionInitialized = false;
   private float cachedMaxWidth = 0.0F;
   private boolean lastLowercaseState = false;
   private final Map<String, Float> moduleNameWidthCache = new HashMap<>();
   private final Map<String, Float> moduleNameHeightCache = new HashMap<>();
   private final Map<Module, Integer> moduleSettingsCountCache = new HashMap<>();
   private String lastSortMode = "";
   private long lastSortUpdate = 0L;
   private static final long SORT_UPDATE_INTERVAL = 33L;
   private static final float TARGET_FRAME_TIME_30FPS = 33.333332F;
   private static final float BASE_POSITION_SMOOTHNESS_30FPS = 0.3F;
   private long lastPositionUpdateTime = System.currentTimeMillis();
   private boolean needsResort = true;
   private boolean cachedHudEnabled = false;
   private boolean cachedWorldEnabled = false;
   private String cachedPlaceholder = "Modules List";
   private boolean cachedShowBackground = false;
   private boolean cachedShowSideLine = false;
   private boolean cachedGradientSideLine = false;
   private boolean cachedGradientText = false;
   private boolean cachedLowercase = false;

   public ModulesList() {
      super("ModulesList", 0, 3, 100, 100, true);
   }

   @Override
   public void tick() {
      boolean stateChanged = false;
      List<Module> modules = Main.getInstance().getModuleProvider().getModules();
      this.moduleEnabledStates.keySet().removeIf(module -> !modules.contains(module));
      this.cachedHudEnabled = false;
      this.cachedWorldEnabled = false;

      for (Module module : modules) {
         boolean enabled = module.isEnabled();
         Boolean previous = this.moduleEnabledStates.put(module, enabled);
         if (previous == null || previous != enabled) {
            stateChanged = true;
         }

         if (enabled) {
            Category cat = module.getCategory();
            if (cat == ModuleCategory.HUD) {
               this.cachedHudEnabled = true;
            } else if (cat == ModuleCategory.WORLD) {
               this.cachedWorldEnabled = true;
            }
         }
      }

      boolean hudChanged = this.updateCategoryAnimation(ModuleCategory.HUD.name(), this.cachedHudEnabled);
      boolean worldChanged = this.updateCategoryAnimation(ModuleCategory.WORLD.name(), this.cachedWorldEnabled);
      if (stateChanged || hudChanged || worldChanged) {
         this.needsResort = true;
      }

      this.updateSortedModulesTick();
      padej.soup.implement.features.modules.hud.ModulesList modulesListModule = padej.soup.implement.features.modules.hud.ModulesList.getInstance();
      if (this.lastLowercaseState != modulesListModule.getLowercase().isValue()) {
         this.clearNameWidthCache();
         this.needsResort = true;
         this.lastLowercaseState = modulesListModule.getLowercase().isValue();
      }

      this.cachedShowBackground = modulesListModule.getShowBackground().isValue();
      this.cachedShowSideLine = modulesListModule.getSideLine().isValue();
      this.cachedGradientSideLine = modulesListModule.getGradientSideLine().isValue();
      this.cachedGradientText = modulesListModule.getGradientText().isValue();
      this.cachedLowercase = modulesListModule.getLowercase().isValue();
      this.updatePlaceholder(modules);
   }

   private void updatePlaceholder(List<Module> modules) {
      int totalEnabled = 0;
      int hudEnabled = 0;

      for (Module m : modules) {
         if (m.isEnabled()) {
            totalEnabled++;
            if (m.getCategory() == ModuleCategory.HUD) {
               hudEnabled++;
            }
         }
      }

      this.cachedPlaceholder = totalEnabled == 0 ? "No active modules" : (totalEnabled == hudEnabled ? "Enable non-HUD modules" : "Modules List");
   }

   private float getCachedModuleNameWidth(Module module, FontRenderer font) {
      String name = this.getModuleName(module);
      return this.moduleNameWidthCache.computeIfAbsent(name, font::getStringWidth);
   }

   private int getCachedSettingsCount(Module module) {
      return this.moduleSettingsCountCache.computeIfAbsent(module, this::countAllSettings);
   }

   private void clearNameWidthCache() {
      this.moduleNameWidthCache.clear();
      this.moduleNameHeightCache.clear();
      this.cachedMaxWidth = 0.0F;
   }

   private boolean updateCategoryAnimation(String categoryName, boolean shouldShow) {
      Direction newDirection = shouldShow ? Direction.FORWARDS : Direction.BACKWARDS;
      if (!this.categoryAnimations.containsKey(categoryName)) {
         Animation animation = new LinearAnimation().setMs(100).setValue(1.0);
         animation.setDirection(newDirection);
         this.categoryAnimations.put(categoryName, animation);
         return true;
      } else {
         Animation animation = this.categoryAnimations.get(categoryName);
         if (!animation.isDirection(newDirection)) {
            animation.setDirection(newDirection);
            return true;
         } else {
            return false;
         }
      }
   }

   private void updateSortedModulesTick() {
      FontRenderer font = Fonts.getSize(15, Fonts.Type.INTER_BOLD);
      padej.soup.implement.features.modules.hud.ModulesList modulesListModule = padej.soup.implement.features.modules.hud.ModulesList.getInstance();
      String currentSortMode = modulesListModule.getSortMode().getSelected();
      long currentTime = System.currentTimeMillis();
      boolean sortModeChanged = !currentSortMode.equals(this.lastSortMode);
      boolean enoughTimePassed = currentTime - this.lastSortUpdate >= 33L;
      if (sortModeChanged) {
         this.lastSortMode = currentSortMode;
      }

      if (!sortModeChanged && (!this.needsResort || !enoughTimePassed)) {
         for (ModulesList.ModuleEntry entry : this.sortedModules) {
            if (entry.module != null) {
               Animation anim = entry.module.getAnimation();
               if (anim != null) {
                  entry.animation = anim;
               }
            }
         }
      } else {
         this.rebuildAndSortEntries(font, modulesListModule);
         this.needsResort = false;
         this.lastSortUpdate = currentTime;
      }

      float lineHeight = 11.0F;
      float targetY = 0.0F;
      boolean anyFinished = false;

      for (ModulesList.ModuleEntry entryx : this.sortedModules) {
         float output = entryx.animation.getOutput().floatValue();
         if (output <= 0.01F) {
            if (output <= 0.0F) {
               anyFinished = true;
            }
         } else {
            if (entryx.module != null) {
               this.targetYPositions.put(entryx.module, targetY);
            } else if (entryx.categoryLabel != null) {
               this.categoryTargetYPositions.put(entryx.categoryLabel, targetY);
            }

            entryx.targetY = targetY;
            targetY += lineHeight;
         }
      }

      if (anyFinished) {
         this.sortedModules.removeIf(entryxx -> entryxx.animation.getOutput() <= 0.0);
      }
   }

   private void interpolatePositions() {
      long now = System.currentTimeMillis();
      float deltaMs = Math.max(1.0F, Math.min(100.0F, (float)(now - this.lastPositionUpdateTime)));
      this.lastPositionUpdateTime = now;
      float frameRatio = deltaMs / 33.333332F;
      float smoothness = 1.0F - (float)Math.pow(0.7F, frameRatio);

      for (ModulesList.ModuleEntry entry : this.sortedModules) {
         if (entry.module != null) {
            float current = this.currentYPositions.getOrDefault(entry.module, entry.targetY);
            float interpolated = current + (entry.targetY - current) * smoothness;
            this.currentYPositions.put(entry.module, interpolated);
            entry.currentY = interpolated;
         } else if (entry.categoryLabel != null) {
            float current = this.categoryCurrentYPositions.getOrDefault(entry.categoryLabel, entry.targetY);
            float interpolated = current + (entry.targetY - current) * smoothness;
            this.categoryCurrentYPositions.put(entry.categoryLabel, interpolated);
            entry.currentY = interpolated;
         }
      }
   }

   private void rebuildAndSortEntries(FontRenderer font, padej.soup.implement.features.modules.hud.ModulesList modulesListModule) {
      List<Module> allModules = Main.getInstance().getModuleProvider().getModules();
      ArrayList<ModulesList.ModuleEntry> moduleEntries = new ArrayList<>(allModules.size());

      for (Module module : allModules) {
         if (module.isVisible()) {
            Category cat = module.getCategory();
            if (cat != ModuleCategory.HUD && cat != ModuleCategory.WORLD && !cat.isHidden()) {
               String cn = module.getClass().getSimpleName();
               if (!"Language".equals(cn) && !"Bright".equals(cn) && !"Fog".equals(cn) && !"Time".equals(cn) && !"Light".equals(cn)) {
                  Animation animation = module.getAnimation();
                  if (animation != null && (module.isEnabled() || animation.getOutput() > 0.01)) {
                     this.currentYPositions.putIfAbsent(module, 0.0F);
                     float currentY = this.currentYPositions.getOrDefault(module, 0.0F);
                     float targetY = this.targetYPositions.getOrDefault(module, 0.0F);
                     moduleEntries.add(new ModulesList.ModuleEntry(module, animation, targetY, currentY));
                  }
               }
            }
         }
      }

      String hudKey = ModuleCategory.HUD.name();
      Animation hudAnimation = this.categoryAnimations.get(hudKey);
      if (hudAnimation != null && (this.cachedHudEnabled || hudAnimation.getOutput() > 0.01)) {
         this.categoryCurrentYPositions.putIfAbsent(hudKey, 0.0F);
         moduleEntries.add(
            new ModulesList.ModuleEntry(
               ModuleCategory.HUD.getLocalizedName(),
               hudAnimation,
               this.categoryTargetYPositions.getOrDefault(hudKey, 0.0F),
               this.categoryCurrentYPositions.getOrDefault(hudKey, 0.0F)
            )
         );
      }

      String worldKey = ModuleCategory.WORLD.name();
      Animation worldAnimation = this.categoryAnimations.get(worldKey);
      if (worldAnimation != null && (this.cachedWorldEnabled || worldAnimation.getOutput() > 0.01)) {
         this.categoryCurrentYPositions.putIfAbsent(worldKey, 0.0F);
         moduleEntries.add(
            new ModulesList.ModuleEntry(
               ModuleCategory.WORLD.getLocalizedName(),
               worldAnimation,
               this.categoryTargetYPositions.getOrDefault(worldKey, 0.0F),
               this.categoryCurrentYPositions.getOrDefault(worldKey, 0.0F)
            )
         );
      }

      boolean byLength = modulesListModule.getSortMode().isSelected("Length");
      if (byLength) {
         moduleEntries.sort(Comparator.<ModulesList.ModuleEntry>comparingDouble(entry -> {
            if (entry.module != null) {
               return this.getCachedModuleNameWidth(entry.module, font);
            } else {
               return entry.categoryLabel != null ? font.getStringWidth(entry.categoryLabel) : 0.0;
            }
         }).reversed());
      } else {
         moduleEntries.sort(
            Comparator.<ModulesList.ModuleEntry>comparingInt(entry -> entry.module != null ? this.getCachedSettingsCount(entry.module) : 0).reversed()
         );
      }

      this.sortedModules = moduleEntries;
   }

   @Override
   public void drawDraggable(DrawContext e) {
      MatrixStack matrix = e.getMatrices();
      FontRenderer font = Fonts.getSize(15, Fonts.Type.INTER_BOLD);
      if (!this.positionInitialized) {
         if (this.getX() == 0 && this.getY() == 3) {
            this.setX(window.getScaledWidth() - 105);
         }

         this.positionInitialized = true;
      }

      this.interpolatePositions();
      if (this.sortedModules.isEmpty()) {
         String placeholder = this.cachedPlaceholder;
         float padding = 5.0F;
         float lineHeight = 16.0F;
         float width = font.getStringWidth(placeholder) + padding * 2.0F;
         if (this.cachedShowBackground) {
            blur.render(
               ShapeProperties.create(matrix, this.getX(), this.getY(), width, lineHeight)
                  .round(3.0F)
                  .softness(1.0F)
                  .thickness(2.0F)
                  .outlineColor(ColorUtil.getOutline())
                  .color(ColorUtil.getBlurRect(0.7F))
                  .build()
            );
         }

         font.drawString(matrix, placeholder, this.getX() + padding, this.getY() + 5.5F, ColorUtil.getText());
         this.setWidth((int)width);
         this.setHeight((int)lineHeight);
      } else {
         float padding = 3.0F;
         float lineHeight = 11.0F;
         float lineGap = 0.0F;
         float sideLineWidth = 2.5F;
         boolean isRightSide = this.getX() > window.getScaledWidth() / 2.0F;
         if (this.cachedMaxWidth == 0.0F) {
            for (Module module : Main.getInstance().getModuleProvider().getModules()) {
               Category cat = module.getCategory();
               if (cat != ModuleCategory.HUD && cat != ModuleCategory.WORLD) {
                  float textWidth = this.getCachedModuleNameWidth(module, font);
                  if (textWidth > this.cachedMaxWidth) {
                     this.cachedMaxWidth = textWidth;
                  }
               }
            }

            this.cachedMaxWidth = Math.max(this.cachedMaxWidth, font.getStringWidth(ModuleCategory.HUD.getLocalizedName()));
            this.cachedMaxWidth = Math.max(this.cachedMaxWidth, font.getStringWidth(ModuleCategory.WORLD.getLocalizedName()));
         }

         float totalWidth = this.cachedMaxWidth + padding * 2.0F + (this.cachedShowSideLine ? sideLineWidth : 0.0F);
         int visibleCount = 0;

         for (ModulesList.ModuleEntry sortedModule : this.sortedModules) {
            if (sortedModule.animation.getOutput() > 0.01F) {
               visibleCount++;
            }
         }

         float totalHeight = visibleCount * (lineHeight + lineGap);
         boolean showBackground = this.cachedShowBackground;
         boolean showSideLine = this.cachedShowSideLine;
         boolean gradientSideLine = this.cachedGradientSideLine;
         boolean gradientText = this.cachedGradientText;
         int bgCol = ColorUtil.getBlurRect(0.7F);
         int textCol = ColorUtil.getText();
         int clientCol = ColorUtil.getClientColor();
         float[] rowBgWidths = new float[this.sortedModules.size()];
         Arrays.fill(rowBgWidths, -1.0F);
         int firstVisible = -1;
         int lastVisible = -1;

         for (int i = 0; i < this.sortedModules.size(); i++) {
            ModulesList.ModuleEntry pre = this.sortedModules.get(i);
            if (!(pre.animation.getOutput() <= 0.01F)) {
               String name = pre.module != null ? this.getModuleName(pre.module) : pre.categoryLabel;
               if (name != null) {
                  float textW = this.moduleNameWidthCache.computeIfAbsent(name, font::getStringWidth);
                  rowBgWidths[i] = textW + padding * 2.0F;
                  if (firstVisible == -1) {
                     firstVisible = i;
                  }

                  lastVisible = i;
               }
            }
         }

         float SNAP_PX = 3.0F;
         int snapClusterStart = -1;
         float snapClusterMax = 0.0F;

         for (int ix = 0; ix < rowBgWidths.length; ix++) {
            if (!(rowBgWidths[ix] < 0.0F)) {
               if (snapClusterStart == -1) {
                  snapClusterStart = ix;
                  snapClusterMax = rowBgWidths[ix];
               } else if (Math.abs(rowBgWidths[ix] - snapClusterMax) <= 3.0F) {
                  snapClusterMax = Math.max(snapClusterMax, rowBgWidths[ix]);
               } else {
                  for (int j = snapClusterStart; j < ix; j++) {
                     if (rowBgWidths[j] >= 0.0F) {
                        rowBgWidths[j] = snapClusterMax;
                     }
                  }

                  snapClusterStart = ix;
                  snapClusterMax = rowBgWidths[ix];
               }
            }
         }

         if (snapClusterStart != -1) {
            for (int jx = snapClusterStart; jx < rowBgWidths.length; jx++) {
               if (rowBgWidths[jx] >= 0.0F) {
                  rowBgWidths[jx] = snapClusterMax;
               }
            }
         }

         float cornerR = 3.0F;
         float anchorY = firstVisible >= 0 ? Math.round(this.getY() + this.sortedModules.get(firstVisible).currentY) : 0.0F;
         int lineIndex = 0;

         for (int ixx = 0; ixx < this.sortedModules.size(); ixx++) {
            ModulesList.ModuleEntry entry = this.sortedModules.get(ixx);
            Animation animation = entry.animation;
            float animationProgress = animation.getOutput().floatValue();
            if (!(animationProgress < 0.01F)) {
               String displayName;
               if (entry.module != null) {
                  displayName = this.getModuleName(entry.module);
               } else {
                  if (entry.categoryLabel == null) {
                     continue;
                  }

                  displayName = entry.categoryLabel;
               }

               float textWidth = this.moduleNameWidthCache.computeIfAbsent(displayName, font::getStringWidth);
               float backgroundWidth = rowBgWidths[ixx] >= 0.0F ? rowBgWidths[ixx] : textWidth + padding * 2.0F;
               float lineWidth = backgroundWidth + (showSideLine ? sideLineWidth : 0.0F);
               float backgroundX;
               if (isRightSide) {
                  float gap = -0.4F;
                  backgroundX = this.getX() + totalWidth - lineWidth + (showSideLine ? sideLineWidth + gap : 0.0F);
               } else {
                  float gap = -2.1F;
                  backgroundX = this.getX() + (showSideLine ? sideLineWidth + gap : 0.0F);
               }

               float slideOffset = (1.0F - animationProgress) * 20.0F * (isRightSide ? 1 : -1);
               float pxY = anchorY + lineIndex * lineHeight;
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, animationProgress);
               matrix.push();
               matrix.translate(slideOffset, 0.0F, 0.0F);
               if (showBackground) {
                  float prevW = 0.0F;
                  float nextW = 0.0F;

                  for (int jxx = ixx - 1; jxx >= 0; jxx--) {
                     if (rowBgWidths[jxx] >= 0.0F) {
                        prevW = rowBgWidths[jxx];
                        break;
                     }
                  }

                  for (int jxxx = ixx + 1; jxxx < this.sortedModules.size(); jxxx++) {
                     if (rowBgWidths[jxxx] >= 0.0F) {
                        nextW = rowBgWidths[jxxx];
                        break;
                     }
                  }

                  float thisW = rowBgWidths[ixx];
                  boolean noPrev = ixx == firstVisible;
                  boolean noNext = ixx == lastVisible;
                  boolean widerThanPrev = !noPrev && thisW > prevW + 0.5F;
                  boolean widerThanNext = !noNext && thisW > nextW + 0.5F;
                  float rTR;
                  float rBR;
                  float rTL;
                  float rBL;
                  if (isRightSide) {
                     rTR = 0.0F;
                     rBR = 0.0F;
                     rTL = !noPrev && !widerThanPrev ? 0.0F : 3.0F;
                     rBL = !noNext && !widerThanNext ? 0.0F : 3.0F;
                  } else {
                     rTL = 0.0F;
                     rBL = 0.0F;
                     rTR = !noPrev && !widerThanPrev ? 0.0F : 3.0F;
                     rBR = !noNext && !widerThanNext ? 0.0F : 3.0F;
                  }

                  blur.render(
                     ShapeProperties.create(matrix, backgroundX, pxY, backgroundWidth, lineHeight)
                        .round(rTR, rBR, rTL, rBL)
                        .softness(-0.5F)
                        .thickness(0.0F)
                        .color(bgCol)
                        .build()
                  );
               }

               int lineColor;
               if (showSideLine && gradientSideLine) {
                  lineColor = ColorUtil.fade(lineIndex, 20);
               } else {
                  lineColor = clientCol;
               }

               int textColor = gradientText && gradientSideLine ? lineColor : (gradientText ? ColorUtil.fade(lineIndex, 70) : textCol);
               float textPxY = lineIndex == 0 ? pxY + 1.0F : pxY;
               font.drawCenteredInRow(matrix, displayName, backgroundX, textPxY, backgroundWidth, lineHeight, textColor);
               matrix.pop();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               lineIndex++;
            }
         }

         if (showSideLine && firstVisible >= 0 && lastVisible >= 0) {
            float lineX = isRightSide ? this.getX() + totalWidth - sideLineWidth : this.getX();
            float lineY1 = this.getY() + this.sortedModules.get(firstVisible).currentY;
            float lineY2 = this.getY() + this.sortedModules.get(lastVisible).currentY + lineHeight;
            int colorTop;
            int colorBottom;
            if (gradientSideLine) {
               colorTop = ColorUtil.fade(0, 20);
               colorBottom = ColorUtil.fade(Math.max(0, lineIndex - 1), 20);
            } else {
               colorTop = clientCol;
               colorBottom = clientCol;
            }

            rectangle.render(
               ShapeProperties.create(matrix, lineX, lineY1, sideLineWidth, lineY2 - lineY1)
                  .color(colorTop, colorBottom, colorTop, colorBottom)
                  .softness(0.0F)
                  .thickness(0.0F)
                  .build()
            );
         }

         this.setWidth((int)totalWidth);
         this.setHeight((int)totalHeight);
      }
   }

   private String getModuleName(Module module) {
      String name = module.getLocalizedName();
      if (this.cachedLowercase) {
         name = name.toLowerCase();
      }

      return name;
   }

   private int countAllSettings(Module module) {
      int count = 0;

      for (Setting setting : module.settings()) {
         count++;
         if (setting instanceof GroupSetting groupSetting) {
            count += this.countSettingsInGroup(groupSetting);
         }
      }

      return count;
   }

   private int countSettingsInGroup(GroupSetting group) {
      int count = 0;

      for (Setting setting : group.getSubSettings()) {
         count++;
         if (setting instanceof GroupSetting nestedGroup) {
            count += this.countSettingsInGroup(nestedGroup);
         }
      }

      return count;
   }

   private static class ModuleEntry {
      Module module;
      Animation animation;
      float targetY;
      float currentY;
      String categoryLabel;

      ModuleEntry(Module module, Animation animation, float targetY, float currentY) {
         this.module = module;
         this.animation = animation;
         this.targetY = targetY;
         this.currentY = currentY;
         this.categoryLabel = null;
      }

      ModuleEntry(String categoryLabel, Animation animation, float targetY, float currentY) {
         this.module = null;
         this.animation = animation;
         this.targetY = targetY;
         this.currentY = currentY;
         this.categoryLabel = categoryLabel;
      }
   }
}
