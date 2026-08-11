package padej.soup.implement.menu.components.implement.module;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.SettingComponentAdder;
import padej.soup.api.feature.module.setting.implement.BindSetting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.TextSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.api.system.shape.implement.Image;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.StringUtil;
import padej.soup.base.util.render.BindBadgeRenderer;
import padej.soup.base.util.render.ScissorManager;
import padej.soup.core.Main;
import padej.soup.implement.menu.MenuScreen;
import padej.soup.implement.menu.components.AbstractComponent;
import padej.soup.implement.menu.components.implement.other.CheckComponent;
import padej.soup.implement.menu.components.implement.settings.AbstractSettingComponent;

public class ModuleComponent extends AbstractComponent {
   private static final Gson GSON_COMPACT = new Gson();
   private final List<AbstractSettingComponent> components = new ArrayList<>();
   private final CheckComponent checkComponent = new CheckComponent();
   private final Module module;
   private boolean binding;
   private boolean collapsed = false;
   private boolean isHovered = false;
   private long lastCopyTime = 0L;
   private long lastPasteTime = 0L;
   private static final long COPY_PASTE_COOLDOWN_MS = 150L;
   private final Animation hoverAnimation = new DecelerateAnimation().setMs(200).setValue(1.0);
   private final Animation arrowRotationAnimation = new DecelerateAnimation().setMs(300).setValue(1.0);
   private final Animation collapseAnimation = new DecelerateAnimation().setMs(250).setValue(1.0);
   private final Animation outlineColorAnimation = new DecelerateAnimation().setMs(300).setValue(1.0);
   private float visualX;
   private float visualY;
   private float prevX;
   private float prevY;
   private float cachedExpandedHeight = 0.0F;
   private final Image arrowImage = new Image().setTexture("textures/modules/arrow.png");

   private void initialize() {
      new SettingComponentAdder().addSettingComponent(this.module.settings(), this.components);
   }

   public ModuleComponent(Module module) {
      this.module = module;
      this.initialize();
      this.collapsed = this.canCollapse() && !module.isExpanded();
      this.collapseAnimation.setDirection(this.collapsed ? Direction.BACKWARDS : Direction.FORWARDS);
      if (this.collapsed) {
         this.collapseAnimation.reset();
      }

      this.visualX = this.prevX = 0.0F;
      this.visualY = this.prevY = 0.0F;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (this.prevX == 0.0F && this.prevY == 0.0F) {
         this.visualX = this.prevX = this.x;
         this.visualY = this.prevY = this.y;
      } else if (this.prevX != this.x || this.prevY != this.y) {
         this.prevX = this.x;
         this.prevY = this.y;
      }

      if (MenuScreen.INSTANCE.isMenuDragging()) {
         this.visualX = this.x;
         this.visualY = this.y;
      } else {
         this.visualX = MathUtil.interpolateSmooth(2.0, this.visualX, this.x);
         this.visualY = MathUtil.interpolateSmooth(2.0, this.visualY, this.y);
      }

      float offsetX = this.visualX - this.x;
      float offsetY = this.visualY - this.y;
      this.isHovered = this.isHover(mouseX, mouseY);
      this.hoverAnimation.setDirection(this.isHovered ? Direction.FORWARDS : Direction.BACKWARDS);
      this.outlineColorAnimation.setDirection(this.module.isState() ? Direction.FORWARDS : Direction.BACKWARDS);
      float scale = 1.0F + (this.hoverAnimation.getOutput().floatValue() - 1.0F) * 0.05F;
      context.getMatrices().push();
      context.getMatrices().translate(offsetX, offsetY, 0.0F);
      context.getMatrices().translate(this.x + this.width / 2.0F, this.y + this.height / 2.0F, 0.0F);
      context.getMatrices().scale(scale, scale, 1.0F);
      context.getMatrices().translate(-(this.x + this.width / 2.0F), -(this.y + this.height / 2.0F), 0.0F);
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x, this.y, this.width, 18.0)
            .round(5.0F, 0.0F, 5.0F, 0.0F)
            .color(ColorUtil.getGuiRectColor2(1.0F))
            .build()
      );
      int defaultOutlineColor = ColorUtil.getOutline(0.56F);
      int activeOutlineColor = ColorUtil.getClientColor();
      int outlineColor = ColorUtil.overCol(defaultOutlineColor, activeOutlineColor, this.outlineColorAnimation.getOutput().floatValue());
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x, this.y, this.width, this.height = this.getComponentHeight())
            .round(5.0F)
            .softness(1.0F)
            .thickness(2.2F)
            .outlineColor(outlineColor)
            .color(ColorUtil.getRectDarker(0.55F))
            .build()
      );
      if (this.canCollapse()) {
         this.arrowRotationAnimation.setDirection(this.collapsed ? Direction.FORWARDS : Direction.BACKWARDS);
         this.renderArrow(context, this.x + 4.0F, this.y + 4.0F);
      }

      float nameOffset = this.canCollapse() ? 18.0F : 9.0F;
      float availableNameWidth;
      if (this.module.isShowEnable() && this.module.isCanBind()) {
         String bindDisplayName = this.binding ? "(" + StringUtil.getBindName(this.module.getKey()) + ") ..." : StringUtil.getBindName(this.module.getKey());
         float bindBadgeLeft = BindBadgeRenderer.getBadgeX(bindDisplayName, this.x + this.width);
         availableNameWidth = bindBadgeLeft - (this.x + nameOffset) - 4.0F;
      } else {
         availableNameWidth = this.width - nameOffset - 9.0F;
      }

      ScissorManager scissor = Main.getInstance().getScissorManager();
      scissor.push(context.getMatrices().peek().getPositionMatrix(), this.x + nameOffset, this.y, availableNameWidth, 18.0F);
      Fonts.getSize(14, Fonts.Type.INTER_BOLD)
         .drawStringWithScroll(
            context.getMatrices(), this.module.getLocalizedName(), this.x + nameOffset, this.y + 8.0F, availableNameWidth, ColorUtil.getText()
         );
      scissor.pop();
      if (this.module.isShowEnable()) {
         String enableText = LocalizationManager.getInstance().get("module.setting.enable");
         Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawString(context.getMatrices(), enableText, this.x + 9.0F, this.y + 29.0F, ColorUtil.getText());
         ((CheckComponent)this.checkComponent.position(this.x + this.width - 16.0F, this.y + 27.0F))
            .setRunnable(this.module::switchState)
            .setState(this.module.isState())
            .render(context, mouseX, mouseY, delta);
      }

      if (this.module.isShowEnable() && this.module.isCanBind()) {
         this.drawBind(context, this.x, this.y);
      }

      if (!this.components.isEmpty()) {
         float animationProgress = this.collapseAnimation.getOutput().floatValue();
         if (animationProgress > 0.01F) {
            for (AbstractSettingComponent component : this.components) {
               component.updateVisibilityAnimation();
            }

            this.cachedExpandedHeight = this.calculateExpandedSettingsHeight();
            float hiddenHeight = this.cachedExpandedHeight * (1.0F - animationProgress);
            ScissorManager scissorManager = Main.getInstance().getScissorManager();
            MenuScreen menuScreen = MenuScreen.INSTANCE;
            float desiredScissorY = this.module.isShowEnable() ? this.y + 32.0F : this.y + 14.0F;
            float visualDesiredScissorY = desiredScissorY + offsetY;
            float menuTopBoundary = menuScreen.y + 37;
            float visualScissorY = Math.max(visualDesiredScissorY, menuTopBoundary);
            float scissorHeight = menuScreen.y + menuScreen.height - visualScissorY;
            scissorManager.push(renderMatrix, this.x + offsetX, visualScissorY, this.width, scissorHeight);
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, -hiddenHeight, 0.0F);
            float offset = this.y + (this.module.isShowEnable() ? 42 : 24);

            for (int i = this.components.size() - 1; i >= 0; i--) {
               AbstractSettingComponent component = this.components.get(i);
               if (!component.shouldSkipRender()) {
                  double visibilityProgress = component.getVisibilityProgress();
                  float animatedHeight = (float)(component.height * visibilityProgress);
                  component.x = this.x;
                  component.y = offset + (this.getExpandedHeightFloat() - (this.module.isShowEnable() ? 46 : 28) - animatedHeight);
                  component.width = this.width;
                  component.render(context, mouseX, mouseY, delta);
                  offset -= animatedHeight;
               }
            }

            context.getMatrices().pop();
            scissorManager.pop();
         }
      }

      context.getMatrices().pop();
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.canCollapse() && button == 0 && MathUtil.isHovered(mouseX, mouseY, this.x + 4.0F, this.y + 4.0F, 10.0, 10.0)) {
         this.collapsed = !this.collapsed;
         this.module.setExpanded(!this.collapsed);
         this.collapseAnimation.setDirection(this.collapsed ? Direction.BACKWARDS : Direction.FORWARDS);
         SoundManager.playSound(this.collapsed ? SoundManager.TURN_OFF : SoundManager.TURN_ON, 1.0F, 1.3F);
         return true;
      } else {
         if (this.collapseAnimation.getOutput().floatValue() > 0.5F) {
            boolean isAnyComponentHovered = this.components.stream().anyMatch(abstractComponent -> abstractComponent.isHover(mouseX, mouseY));
            if (isAnyComponentHovered) {
               for (AbstractSettingComponent abstractComponent : this.components) {
                  if (abstractComponent.isHover(mouseX, mouseY) && abstractComponent.mouseClicked(mouseX, mouseY, button)) {
                     return true;
                  }
               }

               return super.mouseClicked(mouseX, mouseY, button);
            }
         }

         if (this.module.isShowEnable() && this.module.isCanBind()) {
            String bindName = StringUtil.getBindName(this.module.getKey());
            float badgeX = BindBadgeRenderer.getBadgeX(bindName, this.x + this.width);
            float badgeW = BindBadgeRenderer.getBadgeWidth(bindName);
            if (MathUtil.isHovered(mouseX, mouseY, badgeX, this.y + 4.5F, badgeW, 9.0) && button == 0) {
               if (!this.binding) {
                  SoundManager.playSound(SoundManager.CLICK, 1.0F, 1.8F);
               } else {
                  SoundManager.playSound(SoundManager.CLICK, 1.0F, 0.8F);
               }

               this.binding = !this.binding;
               return true;
            }

            if (this.binding) {
               if (button == -1) {
                  SoundManager.playSound(SoundManager.TURN_OFF, 1.0F, 1.3F);
               } else {
                  SoundManager.playSound(SoundManager.TURN_ON, 1.0F, 1.3F);
               }

               this.module.setKey(button);
               this.binding = false;
               return true;
            }
         }

         return this.checkComponent.mouseClicked(mouseX, mouseY, button) ? true : super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      float menuTop = menuScreen.y;
      float menuBottom = menuScreen.y + menuScreen.height;
      float menuLeft = menuScreen.x;
      float menuRight = menuScreen.x + menuScreen.width;
      if (!(mouseX < menuLeft) && !(mouseX > menuRight) && !(mouseY < menuTop) && !(mouseY > menuBottom)) {
         if (this.collapseAnimation.getOutput().floatValue() > 0.01F) {
            for (AbstractComponent abstractComponent : this.components) {
               if (abstractComponent.isHover(mouseX, mouseY) && abstractComponent.y >= menuTop && abstractComponent.y <= menuBottom) {
                  return true;
               }
            }
         }

         boolean isModuleHovered = MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
         if (!isModuleHovered) {
            return false;
         } else {
            float visibleTop = Math.max(this.y, menuTop);
            float visibleBottom = Math.min(this.y + this.height, menuBottom);
            return mouseY >= visibleTop && mouseY <= visibleBottom;
         }
      } else {
         return false;
      }
   }

   @Override
   public void tick() {
      for (AbstractComponent component : this.components) {
         component.tick();
      }

      super.tick();
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.collapseAnimation.getOutput().floatValue() > 0.5F) {
         for (AbstractComponent component : this.components) {
            component.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
         }
      }

      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.collapseAnimation.getOutput().floatValue() > 0.5F) {
         for (AbstractComponent component : this.components) {
            component.mouseReleased(mouseX, mouseY, button);
         }
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      if (this.collapseAnimation.getOutput().floatValue() > 0.5F) {
         for (AbstractComponent component : this.components) {
            component.mouseScrolled(mouseX, mouseY, amount);
         }
      }

      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      int key = keyCode == 261 ? -1 : keyCode;
      if (this.binding && this.module.isCanBind()) {
         if (key == -1) {
            SoundManager.playSound(SoundManager.TURN_OFF, 1.0F, 1.3F);
         } else {
            SoundManager.playSound(SoundManager.TURN_ON, 1.0F, 1.3F);
         }

         this.module.setKey(key);
         this.binding = false;
         return true;
      } else if (keyCode == 67 && (modifiers & 2) != 0 && this.isHovered) {
         this.copyModuleConfig();
         return true;
      } else {
         if (keyCode == 86 && (modifiers & 2) != 0) {
            this.pasteModuleConfig();
         }

         if (this.collapseAnimation.getOutput().floatValue() > 0.5F) {
            for (AbstractComponent component : this.components) {
               component.keyPressed(keyCode, scanCode, modifiers);
            }
         }

         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   @Override
   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      if (this.collapseAnimation.getOutput().floatValue() > 0.5F) {
         for (AbstractComponent component : this.components) {
            component.keyReleased(keyCode, scanCode, modifiers);
         }
      }

      return super.keyReleased(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.collapseAnimation.getOutput().floatValue() > 0.5F) {
         for (AbstractComponent component : this.components) {
            component.charTyped(chr, modifiers);
         }
      }

      return super.charTyped(chr, modifiers);
   }

   private boolean canCollapse() {
      return this.module.isShowEnable() && !this.components.isEmpty();
   }

   private float calculateExpandedSettingsHeight() {
      float offsetY = 0.0F;

      for (AbstractSettingComponent component : this.components) {
         if (!component.shouldSkipRender()) {
            double visibilityProgress = component.getVisibilityProgress();
            offsetY += (float)(component.height * visibilityProgress);
         }
      }

      return offsetY;
   }

   private float getExpandedSettingsHeight() {
      return this.cachedExpandedHeight;
   }

   private float getExpandedHeightFloat() {
      return this.getExpandedSettingsHeight() + (this.module.isShowEnable() ? 46 : 28);
   }

   public int getComponentHeight() {
      int baseHeight = this.module.isShowEnable() ? 46 : 28;
      if (this.components.isEmpty()) {
         return baseHeight;
      } else {
         float animationProgress = this.collapseAnimation.getOutput().floatValue();
         float expandedSettingsHeight = this.getExpandedSettingsHeight();
         float currentSettingsHeight = expandedSettingsHeight * animationProgress;
         return Math.round(baseHeight + currentSettingsHeight);
      }
   }

   private void drawBind(DrawContext context, float renderX, float renderY) {
      String bindName = StringUtil.getBindName(this.module.getKey());
      String name = this.binding ? "(" + bindName + ") ..." : bindName;
      BindBadgeRenderer.draw(context.getMatrices(), name, renderX + this.width, renderY + 9.0F);
   }

   private void renderArrow(DrawContext context, float x, float y) {
      float rotationAngle = -180.0F + this.arrowRotationAnimation.getOutput().floatValue() * 180.0F;
      float centerX = x + 5.0F;
      float centerY = y + 5.0F;
      context.getMatrices().push();
      context.getMatrices().translate(centerX, centerY, 0.0F);
      context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationAngle));
      context.getMatrices().translate(-centerX, -centerY, 0.0F);
      this.arrowImage.render(ShapeProperties.create(context.getMatrices(), x, y, 10.0, 10.0).color(ColorUtil.getDescription()).build());
      context.getMatrices().pop();
   }

   private void copyModuleConfig() {
      long currentTime = System.currentTimeMillis();
      if (currentTime - this.lastCopyTime >= 150L) {
         this.lastCopyTime = currentTime;

         try {
            JsonObject rootObject = new JsonObject();
            JsonObject moduleData = new JsonObject();
            rootObject.addProperty("moduleId", this.module.getIdentifier());
            rootObject.addProperty("moduleName", this.module.getName());
            moduleData.addProperty("enabled", this.module.isState());
            moduleData.addProperty("bind", this.module.getKey());

            for (Setting setting : this.module.settings()) {
               this.saveSetting(setting, moduleData);
            }

            rootObject.add("config", moduleData);
            String jsonString = GSON_COMPACT.toJson(rootObject);
            long windowHandle = mc.getWindow().getHandle();
            GLFW.glfwSetClipboardString(windowHandle, jsonString);
            LoggerUtil.info("Copied config for module: " + this.module.getIdentifier());
            SoundManager.playSound(SoundManager.CLICK, 1.0F, 1.5F);
         } catch (Exception var8) {
            LoggerUtil.error("Failed to copy module config", var8);
         }
      }
   }

   private void pasteModuleConfig() {
      long currentTime = System.currentTimeMillis();
      if (currentTime - this.lastPasteTime >= 150L) {
         this.lastPasteTime = currentTime;

         try {
            long windowHandle = mc.getWindow().getHandle();
            String jsonString = GLFW.glfwGetClipboardString(windowHandle);
            if (jsonString == null || jsonString.trim().isEmpty()) {
               LoggerUtil.warn("Clipboard is empty");
               return;
            }

            jsonString = jsonString.trim();
            JsonObject rootObject = (JsonObject)GSON_COMPACT.fromJson(jsonString, JsonObject.class);
            if (!rootObject.has("moduleId") || !rootObject.has("config")) {
               LoggerUtil.warn("Clipboard does not contain valid module config");
               return;
            }

            String clipboardModuleId = rootObject.get("moduleId").getAsString();
            if (!clipboardModuleId.equals(this.module.getIdentifier())) {
               return;
            }

            JsonObject moduleData = rootObject.getAsJsonObject("config");
            if (moduleData.has("enabled")) {
               this.module.setStateSilent(moduleData.get("enabled").getAsBoolean());
            }

            if (moduleData.has("bind")) {
               this.module.setKey(moduleData.get("bind").getAsInt());
            }

            for (Setting setting : this.module.settings()) {
               try {
                  this.loadSetting(setting, moduleData);
               } catch (Exception var12) {
                  LoggerUtil.error("Failed to load setting: " + setting.getNameKey(), var12);
               }
            }

            if (this.module.isEnabled()) {
               this.module.setStateSilent(true);
            }

            LoggerUtil.info("Pasted config for module: " + this.module.getIdentifier());
            SoundManager.playSound(SoundManager.CLICK, 1.0F, 2.0F);
         } catch (Exception var13) {
            LoggerUtil.error("Failed to paste module config", var13);
         }
      }
   }

   private void saveSetting(Setting setting, JsonObject moduleData) {
      if (setting.isSaveToConfig()) {
         String key = setting.getNameKey();
         switch (setting) {
            case BooleanSetting booleanSetting:
               moduleData.addProperty(key, booleanSetting.isValue());
               break;
            case ValueSetting valueSetting:
               moduleData.addProperty(key, valueSetting.getValue());
               break;
            case TextSetting textSetting:
               String value = textSetting.getText();
               value = value.replace(" ", "%%").replace("/", "++");
               moduleData.addProperty(key, value);
               break;
            case BindSetting bindSetting:
               moduleData.addProperty(key, bindSetting.getKey());
               break;
            case ColorSetting colorSetting:
               moduleData.addProperty(key, colorSetting.getColor());
               break;
            case SelectSetting selectSetting:
               moduleData.addProperty(key, selectSetting.getSelected());
               break;
            case MultiSelectSetting multiSelectSetting:
               List<String> selected = multiSelectSetting.getSelected();
               moduleData.addProperty(key, String.join(",", selected));
               break;
            case MultiColorSetting multiColor:
               JsonObject colorObject = new JsonObject();
               colorObject.addProperty("selectedColorIndex", multiColor.getSelectedColorIndex());
               JsonArray colorsArray = new JsonArray();

               for (ColorSetting color : multiColor.getAllColors()) {
                  colorsArray.add(color.getColor());
               }

               colorObject.add("colors", colorsArray);
               moduleData.add(key, colorObject);
               break;
            case GroupSetting group:
               JsonObject groupObject = new JsonObject();
               groupObject.addProperty("state", group.isValue());

               for (Setting subSetting : group.getSubSettings()) {
                  this.saveSetting(subSetting, groupObject);
               }

               moduleData.add(key, groupObject);
               return;
            default:
         }
      }
   }

   private void loadSetting(Setting setting, JsonObject moduleData) {
      if (setting.isSaveToConfig()) {
         String key = setting.getNameKey();
         if (moduleData.has(key)) {
            JsonElement element = moduleData.get(key);
            switch (setting) {
               case BooleanSetting booleanSetting:
                  booleanSetting.setValue(element.getAsBoolean());
                  break;
               case ValueSetting valueSetting:
                  valueSetting.setValue(element.getAsFloat());
                  break;
               case TextSetting textSetting: {
                  String value = element.getAsString();
                  value = value.replace("%%", " ").replace("++", "/");
                  textSetting.setText(value);
                  break;
               }
               case BindSetting bindSetting:
                  bindSetting.setKey(element.getAsInt());
                  break;
               case ColorSetting colorSetting:
                  colorSetting.setColor(element.getAsInt());
                  break;
               case SelectSetting selectSetting:
                  selectSetting.setSelected(element.getAsString());
                  break;
               case MultiSelectSetting multiSelectSetting: {
                  String value = element.getAsString();
                  List<String> selected = new ArrayList<>(Arrays.asList(value.split(",")));
                  selected.removeIf(s -> !((MultiSelectSetting)setting).getList().contains(s));
                  multiSelectSetting.setSelected(selected);
                  break;
               }
               case MultiColorSetting multiColor:
                  if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                     int oldColor = element.getAsInt();
                     if (multiColor.getColor1() != null) {
                        multiColor.getColor1().setColor(oldColor);
                     }

                     return;
                  }

                  JsonObject colorObject = element.getAsJsonObject();
                  if (colorObject.has("selectedColorIndex")) {
                     multiColor.setSelectedColorIndex(colorObject.get("selectedColorIndex").getAsInt());
                  }

                  if (colorObject.has("colors")) {
                     JsonArray colorsArray = colorObject.getAsJsonArray("colors");
                     List<ColorSetting> colorSettings = multiColor.getAllColors();

                     for (int i = 0; i < Math.min(colorsArray.size(), colorSettings.size()); i++) {
                        colorSettings.get(i).setColor(colorsArray.get(i).getAsInt());
                     }
                  }
                  break;
               case GroupSetting group:
                  label63: {
                     JsonObject groupObject = element.getAsJsonObject();
                     if (groupObject.has("state")) {
                        group.setValue(groupObject.get("state").getAsBoolean());
                     }

                     for (Setting subSetting : group.getSubSettings()) {
                        this.loadSetting(subSetting, groupObject);
                     }
                     break label63;
                  }
               default:
            }
         }
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ModuleComponent that = (ModuleComponent)o;
         return this.module.equals(that.module);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.module);
   }

   public List<AbstractSettingComponent> getComponents() {
      return this.components;
   }

   public CheckComponent getCheckComponent() {
      return this.checkComponent;
   }

   public Module getModule() {
      return this.module;
   }

   public boolean isBinding() {
      return this.binding;
   }

   public boolean isCollapsed() {
      return this.collapsed;
   }

   public boolean isHovered() {
      return this.isHovered;
   }

   public long getLastCopyTime() {
      return this.lastCopyTime;
   }

   public long getLastPasteTime() {
      return this.lastPasteTime;
   }

   public Animation getHoverAnimation() {
      return this.hoverAnimation;
   }

   public Animation getArrowRotationAnimation() {
      return this.arrowRotationAnimation;
   }

   public Animation getCollapseAnimation() {
      return this.collapseAnimation;
   }

   public Animation getOutlineColorAnimation() {
      return this.outlineColorAnimation;
   }

   public float getVisualX() {
      return this.visualX;
   }

   public float getVisualY() {
      return this.visualY;
   }

   public float getPrevX() {
      return this.prevX;
   }

   public float getPrevY() {
      return this.prevY;
   }

   public float getCachedExpandedHeight() {
      return this.cachedExpandedHeight;
   }

   public Image getArrowImage() {
      return this.arrowImage;
   }
}
