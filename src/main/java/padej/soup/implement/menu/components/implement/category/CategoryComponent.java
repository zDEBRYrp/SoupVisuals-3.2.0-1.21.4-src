package padej.soup.implement.menu.components.implement.category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import padej.soup.api.feature.module.Category;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.msdf.MsdfFont;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.render.ScissorManager;
import padej.soup.core.Main;
import padej.soup.implement.menu.MenuScreen;
import padej.soup.implement.menu.components.AbstractComponent;
import padej.soup.implement.menu.components.implement.module.ModuleComponent;
import padej.soup.implement.menu.components.implement.other.ActivityCalendarComponent;
import padej.soup.implement.menu.components.implement.other.ConfigManagerComponent;
import padej.soup.implement.menu.components.implement.other.FriendsListComponent;
import padej.soup.implement.menu.components.implement.other.ModuleDescriptionComponent;
import padej.soup.implement.menu.components.implement.settings.AbstractSettingComponent;

public class CategoryComponent extends AbstractComponent {
   private static final List<ModuleComponent> SHARED_MODULE_COMPONENTS = new ArrayList<>();
   private static boolean sharedModulesInitialized = false;
   private final List<ModuleComponent> moduleComponents = new ArrayList<>();
   private final List<ModuleComponent> visibleModuleComponents = new ArrayList<>();
   private final Map<ModuleComponent, Integer> assignedColumns = new HashMap<>();
   private final int[] offsetsBuffer = new int[2];
   private final Category category;
   private ActivityCalendarComponent activityCalendarComponent;
   private final FriendsListComponent friendsListComponent;
   private final ConfigManagerComponent configManagerComponent;
   private final Animation hoverAnimation = new DecelerateAnimation().setMs(200).setValue(0.15F);
   private final Animation selectionAnimation = new DecelerateAnimation().setMs(300).setValue(1.0);
   private Category lastCategory = null;
   private String lastSearchText = "";
   private long lastConfigSaveTime = 0L;
   private static final long CONFIG_SAVE_INTERVAL = 4000L;
   private int mouseX = 0;
   private int mouseY = 0;
   private final int categoryIconCodepoint;
   private final String categoryIconTexture;
   private final MsdfFont categoryIconFont;
   private FontRenderer cachedAddonIconRenderer;
   private float cachedAddonIconSize = -1.0F;

   public CategoryComponent(Category category) {
      this.category = category;
      this.categoryIconCodepoint = category.getIconCodepoint();
      this.categoryIconFont = category.getIconFont();
      this.categoryIconTexture = this.categoryIconCodepoint < 0 ? "textures/modules/" + category.getIdentifier() + ".png" : null;
      this.initialize();
      if (Main.getInstance().getActivityManager() != null) {
         this.activityCalendarComponent = new ActivityCalendarComponent(Main.getInstance().getActivityManager());
      }

      this.friendsListComponent = new FriendsListComponent();
      this.configManagerComponent = new ConfigManagerComponent();
   }

   private void initialize() {
      if (!ModuleCategory.HOME.equals(this.category) && !ModuleCategory.PERSONAL_INFO.equals(this.category)) {
         ensureSharedModulesInitialized();
         this.moduleComponents.addAll(SHARED_MODULE_COMPONENTS);
      }
   }

   public static void invalidateSharedModules() {
      SHARED_MODULE_COMPONENTS.clear();
      sharedModulesInitialized = false;
   }

   private static void ensureSharedModulesInitialized() {
      if (!sharedModulesInitialized) {
         List<Module> modules = new ArrayList<>(Main.getInstance().getModuleRepository().modules());
         modules.sort((a, b) -> Integer.compare(countAllSettingsStatic(b), countAllSettingsStatic(a)));

         for (Module module : modules) {
            if (module.isVisible()) {
               SHARED_MODULE_COMPONENTS.add(new ModuleComponent(module));
            }
         }

         sharedModulesInitialized = true;
      }
   }

   public void renderIcon(DrawContext context, int mouseX, int mouseY) {
      if (!ModuleCategory.PERSONAL_INFO.equals(this.category) && !ModuleCategory.SEARCH.equals(this.category) && !ModuleCategory.HOME.equals(this.category)) {
         this.drawCategoryTab(context, context.getMatrices(), mouseX, mouseY);
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      ScissorManager scissorManager = Main.getInstance().getScissorManager();
      Category currentCategory = menuScreen.getCategory();
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      if (ModuleCategory.HOME.equals(currentCategory) && ModuleCategory.HOME.equals(this.category) && mc.currentScreen == menuScreen) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastConfigSaveTime >= 4000L) {
            this.lastConfigSaveTime = currentTime;
            if (this.configManagerComponent != null) {
               this.configManagerComponent.saveActiveConfigIfExists();
            }
         }
      } else {
         this.lastConfigSaveTime = 0L;
      }

      if (ModuleCategory.PERSONAL_INFO.equals(currentCategory)) {
         if (ModuleCategory.PERSONAL_INFO.equals(this.category)) {
            float offsetX = 33.0F;
            float offsetY = 28.0F;
            scissorManager.push(renderMatrix, menuScreen.x + offsetX, menuScreen.y + offsetY, menuScreen.width - offsetX, menuScreen.height - offsetY - 1.0F);
            this.renderStatsContent(context, menuScreen, mouseX, mouseY, delta);
            scissorManager.pop();
            this.renderStatsTooltips(context, mouseX, mouseY);
         }
      } else if (ModuleCategory.HOME.equals(currentCategory)) {
         if (ModuleCategory.HOME.equals(this.category)) {
            float offsetX = 33.0F;
            float offsetY = 28.0F;
            scissorManager.push(renderMatrix, menuScreen.x + offsetX, menuScreen.y + offsetY, menuScreen.width - offsetX, menuScreen.height - offsetY - 1.0F);
            this.renderHomeContent(context, menuScreen, mouseX, mouseY, delta);
            scissorManager.pop();
         }
      } else if (this.isContentOwner(currentCategory)) {
         String currentSearchText = this.getSearchTextLower(menuScreen, currentCategory);
         this.updateVisibleModules(currentCategory, currentSearchText);
         int[] totalOffsets = this.calculateOffsets();
         int leftOffset = totalOffsets[0];
         int rightOffset = totalOffsets[1];
         int columnWidth = 130;
         int columnGapX = 10;
         int columnGapY = 10;
         float offsetX = 33.0F;
         float offsetY = 28.0F;
         scissorManager.push(renderMatrix, menuScreen.x + offsetX, menuScreen.y + offsetY, menuScreen.width - offsetX, menuScreen.height - offsetY - 1.0F);

         for (int i = this.visibleModuleComponents.size() - 1; i >= 0; i--) {
            ModuleComponent component = this.visibleModuleComponents.get(i);
            int componentHeight = component.getComponentHeight() + columnGapY;
            int column = this.getModuleColumn(component);
            int columnOffset = column == 0 ? leftOffset : rightOffset;
            component.x = menuScreen.x + 42 + column * (columnWidth + columnGapX);
            component.y = (float)(menuScreen.y + 32 + columnGapY + columnOffset - componentHeight + this.smoothedScroll);
            component.width = columnWidth;
            component.render(context, mouseX, mouseY, delta);
            if (column == 0) {
               leftOffset -= componentHeight;
            } else {
               rightOffset -= componentHeight;
            }
         }

         scissorManager.pop();
         this.renderTooltips(context);
         int maxScrollHeight = this.calculateMaxScrollHeight(totalOffsets[0], totalOffsets[1]);
         this.scroll = MathHelper.clamp(this.scroll, -maxScrollHeight, 0.0);
         this.smoothedScroll = MathUtil.interpolateSmooth(2.0, this.smoothedScroll, this.scroll);
      }
   }

   @Override
   public void tick() {
      Category currentCategory = MenuScreen.INSTANCE.getCategory();
      if (this.isContentOwner(currentCategory) && !ModuleCategory.HOME.equals(currentCategory) && !ModuleCategory.PERSONAL_INFO.equals(currentCategory)) {
         for (ModuleComponent moduleComponent : this.moduleComponents) {
            moduleComponent.tick();
         }
      }

      super.tick();
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      Category currentCategory = menuScreen.getCategory();
      if (!ModuleCategory.PERSONAL_INFO.equals(this.category) && !ModuleCategory.SEARCH.equals(this.category) && !ModuleCategory.HOME.equals(this.category)) {
         float iconSize = 11.0F;
         float padding = 6.0F;
         float outlineSize = iconSize + padding * 2.0F;
         float iconX = this.x + (this.width - iconSize) / 2.0F;
         float iconY = this.y + (this.height - iconSize) / 2.0F;
         float outlineX = iconX - padding;
         float outlineY = iconY - padding;
         if (MathUtil.isHovered(mouseX, mouseY, outlineX, outlineY, outlineSize, outlineSize) && button == 0) {
            if (!MenuScreen.INSTANCE.getCategory().equals(this.category)) {
               SoundManager.playSound(SoundManager.CHANGE_CATEGORY, 2.0F, 1.3F);
            }

            MenuScreen.INSTANCE.setCategory(this.category);
         }
      }

      if (!this.isContentOwner(currentCategory)) {
         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         float offsetX = 33.0F;
         float offsetY = 28.0F;
         if (MathUtil.isHovered(mouseX, mouseY, menuScreen.x + offsetX, menuScreen.y + offsetY, menuScreen.width - offsetX, menuScreen.height - offsetY)) {
            if (ModuleCategory.PERSONAL_INFO.equals(currentCategory)
               && ModuleCategory.PERSONAL_INFO.equals(this.category)
               && this.activityCalendarComponent != null
               && this.activityCalendarComponent.mouseClicked(mouseX, mouseY, button)) {
               return true;
            }

            if (ModuleCategory.HOME.equals(currentCategory) && ModuleCategory.HOME.equals(this.category)) {
               if (this.friendsListComponent != null && this.friendsListComponent.mouseClicked(mouseX, mouseY, button)) {
                  return true;
               }

               if (this.configManagerComponent != null && this.configManagerComponent.mouseClicked(mouseX, mouseY, button)) {
                  return true;
               }
            }

            this.updateVisibleModules(currentCategory, this.getSearchTextLower(menuScreen, currentCategory));
            boolean isAnyComponentHovered = false;

            for (ModuleComponent moduleComponent : this.visibleModuleComponents) {
               if (moduleComponent.isHover(mouseX, mouseY)) {
                  isAnyComponentHovered = true;
                  break;
               }
            }

            if (isAnyComponentHovered) {
               for (ModuleComponent moduleComponentx : this.visibleModuleComponents) {
                  if (moduleComponentx.isHover(mouseX, mouseY) && moduleComponentx.mouseClicked(mouseX, mouseY, button)) {
                     return true;
                  }
               }

               return super.mouseClicked(mouseX, mouseY, button);
            }
         }

         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      Category currentCategory = menuScreen.getCategory();
      if (!this.isContentOwner(currentCategory)) {
         return super.isHover(mouseX, mouseY);
      } else {
         this.updateVisibleModules(currentCategory, this.getSearchTextLower(menuScreen, currentCategory));

         for (ModuleComponent moduleComponent : this.visibleModuleComponents) {
            if (moduleComponent.isHover(mouseX, mouseY)) {
               return true;
            }
         }

         return super.isHover(mouseX, mouseY);
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      Category currentCategory = menuScreen.getCategory();
      if (this.isContentOwner(currentCategory) && !ModuleCategory.HOME.equals(currentCategory) && !ModuleCategory.PERSONAL_INFO.equals(currentCategory)) {
         this.updateVisibleModules(currentCategory, this.getSearchTextLower(menuScreen, currentCategory));

         for (ModuleComponent moduleComponent : this.visibleModuleComponents) {
            moduleComponent.mouseReleased(mouseX, mouseY, button);
         }
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      Category currentCategory = menuScreen.getCategory();
      float offsetX = 33.0F;
      float offsetY = 28.0F;
      if (MathUtil.isHovered(mouseX, mouseY, menuScreen.x + offsetX, menuScreen.y + offsetY, menuScreen.width - offsetX, menuScreen.height - offsetY)) {
         if (ModuleCategory.HOME.equals(currentCategory) && ModuleCategory.HOME.equals(this.category)) {
            if (this.friendsListComponent != null && this.friendsListComponent.mouseScrolled(mouseX, mouseY, amount)) {
               return true;
            }

            if (this.configManagerComponent != null && this.configManagerComponent.mouseScrolled(mouseX, mouseY, amount)) {
               return true;
            }
         } else if (this.isContentOwner(currentCategory)) {
            this.scroll += amount * 20.0;
         }
      }

      if (this.isContentOwner(currentCategory) && !ModuleCategory.HOME.equals(currentCategory) && !ModuleCategory.PERSONAL_INFO.equals(currentCategory)) {
         this.updateVisibleModules(currentCategory, this.getSearchTextLower(menuScreen, currentCategory));

         for (ModuleComponent moduleComponent : this.visibleModuleComponents) {
            moduleComponent.mouseScrolled(mouseX, mouseY, amount);
         }
      }

      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      Category currentCategory = menuScreen.getCategory();
      if (ModuleCategory.HOME.equals(currentCategory) && ModuleCategory.HOME.equals(this.category)) {
         if (this.friendsListComponent != null && this.friendsListComponent.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
         }

         if (this.configManagerComponent != null && this.configManagerComponent.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
         }
      }

      if (this.isContentOwner(currentCategory) && !ModuleCategory.HOME.equals(currentCategory) && !ModuleCategory.PERSONAL_INFO.equals(currentCategory)) {
         this.updateVisibleModules(currentCategory, this.getSearchTextLower(menuScreen, currentCategory));

         for (ModuleComponent moduleComponent : this.visibleModuleComponents) {
            moduleComponent.keyPressed(keyCode, scanCode, modifiers);
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      Category currentCategory = menuScreen.getCategory();
      if (ModuleCategory.HOME.equals(currentCategory) && ModuleCategory.HOME.equals(this.category)) {
         if (this.friendsListComponent != null && this.friendsListComponent.charTyped(chr, modifiers)) {
            return true;
         }

         if (this.configManagerComponent != null && this.configManagerComponent.charTyped(chr, modifiers)) {
            return true;
         }
      }

      if (this.isContentOwner(currentCategory) && !ModuleCategory.HOME.equals(currentCategory) && !ModuleCategory.PERSONAL_INFO.equals(currentCategory)) {
         this.updateVisibleModules(currentCategory, this.getSearchTextLower(menuScreen, currentCategory));

         for (ModuleComponent moduleComponent : this.visibleModuleComponents) {
            moduleComponent.charTyped(chr, modifiers);
         }
      }

      return super.charTyped(chr, modifiers);
   }

   private void drawCategoryTab(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
      float iconSize = 12.0F;
      float padding = 6.0F;
      float outlineSize = iconSize + padding * 2.0F;
      float iconX = this.x + (this.width - iconSize) / 2.0F;
      float iconY = this.y + (this.height - iconSize) / 2.0F;
      float outlineX = iconX - padding;
      float outlineY = iconY - padding;
      boolean isSelected = this.category.equals(MenuScreen.INSTANCE.getCategory());
      this.selectionAnimation.setDirection(isSelected ? Direction.FORWARDS : Direction.BACKWARDS);
      boolean isHovered = MathUtil.isHovered(mouseX, mouseY, outlineX, outlineY, outlineSize, outlineSize);
      this.hoverAnimation.setDirection(isHovered ? Direction.FORWARDS : Direction.BACKWARDS);
      float hoverScale = 1.0F + this.hoverAnimation.getOutput().floatValue();
      float selectionProgress = this.selectionAnimation.getOutput().floatValue();
      MathUtil.scale(matrix, this.x + this.width / 2.0F, this.y + this.height / 2.0F, hoverScale, () -> {
         int descriptionColor = ColorUtil.getDescription();
         int clientColor = ColorUtil.getClientColor();
         int iconColor = ColorUtil.overCol(descriptionColor, clientColor, selectionProgress);
         if (this.categoryIconCodepoint >= 0) {
            if (this.categoryIconFont != null) {
               if (this.cachedAddonIconRenderer == null || this.cachedAddonIconSize != iconSize) {
                  this.cachedAddonIconRenderer = new FontRenderer(this.categoryIconFont, iconSize);
                  this.cachedAddonIconSize = iconSize;
               }

               this.cachedAddonIconRenderer.drawIcon(matrix, this.categoryIconCodepoint, iconX, iconY, iconSize, iconColor);
            } else {
               image.setIcon(this.categoryIconCodepoint).render(ShapeProperties.create(matrix, iconX, iconY, iconSize, iconSize).color(iconColor).build());
            }
         } else {
            image.setTexture(this.categoryIconTexture).render(ShapeProperties.create(matrix, iconX, iconY, iconSize, iconSize).color(iconColor).build());
         }
      });
   }

   private void updateVisibleModules(Category currentCategory, String searchText) {
      if (!currentCategory.equals(this.lastCategory) || !searchText.equals(this.lastSearchText)) {
         this.lastCategory = currentCategory;
         this.lastSearchText = searchText;
         this.visibleModuleComponents.clear();
         if (ModuleCategory.SEARCH.equals(currentCategory)) {
            if (searchText.isEmpty()) {
               this.assignedColumns.clear();
               return;
            }

            for (ModuleComponent component : this.moduleComponents) {
               if (this.isSearchMatch(component, searchText)) {
                  this.visibleModuleComponents.add(component);
               }
            }
         } else {
            for (ModuleComponent componentx : this.moduleComponents) {
               if (componentx.getModule().getCategory().equals(currentCategory)) {
                  this.visibleModuleComponents.add(componentx);
               }
            }
         }

         this.assignColumns(currentCategory);
      }
   }

   private void assignColumns(Category currentCategory) {
      this.assignedColumns.clear();
      if (!this.visibleModuleComponents.isEmpty()) {
         if (ModuleCategory.CLIENT.equals(currentCategory)) {
            int columnIndex = 0;

            for (int i = this.visibleModuleComponents.size() - 1; i >= 0; i--) {
               ModuleComponent component = this.visibleModuleComponents.get(i);
               String moduleName = component.getModule().getVisibleName();

               int targetColumn = switch (moduleName) {
                  case "Theme" -> 0;
                  case "Fake Player", "Key Bind" -> 1;
                  default -> columnIndex++ % 2;
               };
               this.assignedColumns.put(component, targetColumn);
            }
         } else {
            int columnIndex = 0;

            for (int i = this.visibleModuleComponents.size() - 1; i >= 0; i--) {
               ModuleComponent component = this.visibleModuleComponents.get(i);
               this.assignedColumns.put(component, columnIndex % 2);
               columnIndex++;
            }
         }
      }
   }

   private int getModuleColumn(ModuleComponent component) {
      return this.assignedColumns.getOrDefault(component, 0);
   }

   private int[] calculateOffsets() {
      this.offsetsBuffer[0] = 0;
      this.offsetsBuffer[1] = 0;
      int columnGapY = 10;

      for (int i = this.visibleModuleComponents.size() - 1; i >= 0; i--) {
         ModuleComponent component = this.visibleModuleComponents.get(i);
         int componentHeight = component.getComponentHeight() + columnGapY;
         int column = this.getModuleColumn(component);
         this.offsetsBuffer[column] = this.offsetsBuffer[column] + componentHeight;
      }

      return this.offsetsBuffer;
   }

   private int calculateMaxScrollHeight(int leftHeight, int rightHeight) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      int maxColumnHeight = Math.max(leftHeight, rightHeight);
      int visibleHeight = menuScreen.height - 38;
      int maxScroll = maxColumnHeight - visibleHeight;
      return Math.max(0, maxScroll);
   }

   private boolean isSearchMatch(ModuleComponent component, String searchText) {
      String moduleName = component.getModule().getLocalizedName().toLowerCase(Locale.ROOT);
      if (moduleName.contains(searchText)) {
         return true;
      } else {
         for (Setting setting : component.getModule().settings()) {
            if (this.searchInSetting(setting, searchText)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean searchInSetting(Setting setting, String searchText) {
      if (setting.getLocalizedName().toLowerCase(Locale.ROOT).contains(searchText)) {
         return true;
      } else if (setting.getLocalizedDescription() != null && setting.getLocalizedDescription().toLowerCase(Locale.ROOT).contains(searchText)) {
         return true;
      } else {
         if (setting instanceof SelectSetting selectSetting && selectSetting.getList() != null) {
            for (String option : selectSetting.getList()) {
               if (option.toLowerCase(Locale.ROOT).contains(searchText)) {
                  return true;
               }
            }
         }

         if (setting instanceof MultiSelectSetting multiSelectSetting && multiSelectSetting.getList() != null) {
            for (String optionx : multiSelectSetting.getList()) {
               if (optionx.toLowerCase(Locale.ROOT).contains(searchText)) {
                  return true;
               }
            }
         }

         if (setting instanceof GroupSetting groupSetting) {
            for (Setting subSetting : groupSetting.getSubSettings()) {
               if (this.searchInSetting(subSetting, searchText)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean isContentOwner(Category currentCategory) {
      if (ModuleCategory.PERSONAL_INFO.equals(currentCategory)) {
         return ModuleCategory.PERSONAL_INFO.equals(this.category);
      } else if (ModuleCategory.HOME.equals(currentCategory)) {
         return ModuleCategory.HOME.equals(this.category);
      } else {
         return ModuleCategory.SEARCH.equals(currentCategory) ? ModuleCategory.SEARCH.equals(this.category) : this.category.equals(currentCategory);
      }
   }

   private String getSearchTextLower(MenuScreen menuScreen, Category currentCategory) {
      return !ModuleCategory.SEARCH.equals(currentCategory) ? "" : menuScreen.getSearchComponent().getText().toLowerCase(Locale.ROOT);
   }

   private void renderStatsContent(DrawContext context, MenuScreen menuScreen, int mouseX, int mouseY, float delta) {
      float contentX = menuScreen.x + 36;
      float contentY = menuScreen.y - 40;
      if (this.activityCalendarComponent != null) {
         this.activityCalendarComponent.x = contentX - 3.0F;
         this.activityCalendarComponent.y = contentY + 75.0F;
         this.activityCalendarComponent.render(context, mouseX, mouseY, delta);
      }
   }

   private void renderHomeContent(DrawContext context, MenuScreen menuScreen, int mouseX, int mouseY, float delta) {
      float contentX = menuScreen.x + 41;
      float contentY = menuScreen.y + 32;
      float availableWidth = menuScreen.width - 41 - 10;
      float gap = 10.0F;
      float halfWidth = (availableWidth - gap) / 2.0F;
      if (this.friendsListComponent != null) {
         this.friendsListComponent.x = contentX;
         this.friendsListComponent.y = contentY;
         this.friendsListComponent.width = halfWidth;
         this.friendsListComponent.render(context, mouseX, mouseY, delta);
      }

      if (this.configManagerComponent != null) {
         this.configManagerComponent.x = contentX + halfWidth + gap;
         this.configManagerComponent.y = contentY;
         this.configManagerComponent.width = halfWidth;
         this.configManagerComponent.render(context, mouseX, mouseY, delta);
      }
   }

   private void renderStatsTooltips(DrawContext context, int mouseX, int mouseY) {
      if (this.activityCalendarComponent != null
         && this.activityCalendarComponent.hoveredDate != null
         && this.activityCalendarComponent.hoveredActivity != null) {
         this.activityCalendarComponent.renderTooltip(context, mouseX, mouseY);
      }
   }

   private void renderTooltips(DrawContext context) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      ModuleDescriptionComponent descriptionComponent = menuScreen.getModuleDescriptionComponent();
      if (descriptionComponent != null) {
         if (windowManager.isMouseOverAnyWindow(this.mouseX, this.mouseY)) {
            return;
         }

         ModuleComponent hoveredModule = null;
         AbstractSettingComponent hoveredSetting = null;

         for (ModuleComponent moduleComponent : this.visibleModuleComponents) {
            if (moduleComponent.isHover(this.mouseX, this.mouseY)) {
               float headerHeight = moduleComponent.getModule().isShowEnable() ? 40.0F : 28.0F;
               if (this.mouseY < moduleComponent.y + headerHeight) {
                  descriptionComponent.setHoveredModule(moduleComponent.getModule());
                  descriptionComponent.render(context, this.mouseX, this.mouseY, 0.0F);
                  return;
               }

               for (AbstractSettingComponent settingComponent : moduleComponent.getComponents()) {
                  if (settingComponent.isHover(this.mouseX, this.mouseY)) {
                     hoveredSetting = settingComponent;
                     break;
                  }
               }

               if (hoveredSetting != null) {
                  descriptionComponent.setHoveredSetting(hoveredSetting.getSetting());
                  descriptionComponent.render(context, this.mouseX, this.mouseY, 0.0F);
                  return;
               }
               break;
            }
         }

         descriptionComponent.hide();
      }
   }

   private static int countAllSettingsStatic(Module module) {
      int count = 0;

      for (Setting setting : module.settings()) {
         count++;
         if (setting instanceof GroupSetting groupSetting) {
            count += countSettingsInGroupStatic(groupSetting);
         }
      }

      return count;
   }

   private static int countSettingsInGroupStatic(GroupSetting group) {
      int count = 0;

      for (Setting setting : group.getSubSettings()) {
         count++;
         if (setting instanceof GroupSetting nestedGroup) {
            count += countSettingsInGroupStatic(nestedGroup);
         }
      }

      return count;
   }

   public Category getCategory() {
      return this.category;
   }

   public ConfigManagerComponent getConfigManagerComponent() {
      return this.configManagerComponent;
   }
}
