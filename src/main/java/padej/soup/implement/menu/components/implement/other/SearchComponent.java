package padej.soup.implement.menu.components.implement.other;

import java.util.Optional;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import padej.soup.api.feature.module.Category;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.render.ScissorManager;
import padej.soup.core.Main;
import padej.soup.implement.menu.MenuScreen;
import padej.soup.implement.menu.components.AbstractComponent;

public class SearchComponent extends AbstractComponent {
   public static boolean typing = false;
   private int cursorPosition = 0;
   private long lastClickTime = 0L;
   private float xOffset = 0.0F;
   private String text = "";
   private Category previousCategory = ModuleCategory.VISUALS;
   private float animatedWidth = 17.0F;
   private static final float COLLAPSED_WIDTH = 17.0F;
   private static final float EXPANDED_WIDTH = 78.0F;
   private static final float ICON_SIZE = 7.0F;
   private final Animation hoverAnimation = new DecelerateAnimation().setMs(200).setValue(0.15F);
   private final Animation selectAnimation = new DecelerateAnimation().setMs(200).setValue(1.0);

   public float getAnimationProgress() {
      return (this.animatedWidth - 17.0F) / 61.0F;
   }

   public void setText(String text) {
      this.text = text;
      this.cursorPosition = text.length();
   }

   public void setCursorPosition(int position) {
      this.cursorPosition = Math.max(0, Math.min(position, this.text.length()));
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(12);
      this.updateXOffset(font, this.cursorPosition);
      float targetWidth = typing ? 78.0F : 17.0F;
      this.animatedWidth = MathHelper.lerp(delta * 0.5F, this.animatedWidth, targetWidth);
      this.width = this.animatedWidth;
      this.height = 17.0F;
      boolean isSearchSelected = ModuleCategory.SEARCH.equals(MenuScreen.INSTANCE.getCategory());
      boolean isHovered = MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
      this.hoverAnimation.setDirection(isHovered ? Direction.FORWARDS : Direction.BACKWARDS);
      this.selectAnimation.setDirection(isSearchSelected ? Direction.FORWARDS : Direction.BACKWARDS);
      float hoverScale = 1.0F + this.hoverAnimation.getOutput().floatValue();
      float selectProgress = this.selectAnimation.getOutput().floatValue();
      float centerX = this.x + this.width / 2.0F;
      float centerY = this.y + this.height / 2.0F;
      MathUtil.scale(
         matrix,
         centerX,
         centerY,
         hoverScale,
         () -> {
            int outlineColor = ColorUtil.overCol(ColorUtil.getOutline(), ColorUtil.getClientColor(), selectProgress);
            int iconColor = ColorUtil.overCol(ColorUtil.getText(), ColorUtil.getClientColor(), selectProgress);
            rectangle.render(
               ShapeProperties.create(matrix, this.x, this.y, this.width, this.height)
                  .round(3.0F)
                  .thickness(2.0F)
                  .softness(1.0F)
                  .outlineColor(outlineColor)
                  .color(ColorUtil.getGuiRectColor(0.5F))
                  .build()
            );
            float iconY = this.y + (this.height - 7.0F) / 2.0F;
            float iconX;
            if (this.animatedWidth < 77.0F) {
               float progress = (this.animatedWidth - 17.0F) / 61.0F;
               float collapsedIconX = this.x + 5.0F;
               float expandedIconX = this.x + this.width - 7.0F - 5.0F;
               iconX = MathHelper.lerp(progress, collapsedIconX, expandedIconX);
            } else {
               iconX = this.x + this.width - 7.0F - 5.0F;
            }

            image.setIcon(61455).render(ShapeProperties.create(matrix, iconX, iconY, 7.0, 7.0).color(iconColor).build());
            float animationProgress = (this.animatedWidth - 17.0F) / 61.0F;
            float textAlpha = Math.max(0.0F, Math.min(1.0F, (animationProgress - 0.2F) / 0.6F));
            if (textAlpha > 0.01F) {
               String displayText = this.text;
               ScissorManager scissor = Main.getInstance().getScissorManager();
               scissor.push(renderMatrix, this.x + 1.0F, this.y, this.width - 7.0F - 8.0F, this.height);
               if (!this.text.isEmpty() && typing) {
                  String searchText = this.text.toLowerCase();
                  int autocompleteAlpha = (int)(textAlpha * 119.0F) << 24;
                  int autocompleteColor = autocompleteAlpha | 7829367;
                  Main.getInstance()
                     .getModuleProvider()
                     .getModules()
                     .stream()
                     .filter(mod -> mod.getLocalizedName().toLowerCase().startsWith(searchText))
                     .findFirst()
                     .ifPresentOrElse(
                        module -> {
                           String completion = module.getLocalizedName();
                           String remainingText = completion.substring(this.text.length());
                           float textWidth = font.getStringWidth(this.text);
                           FontRenderer italicFont = Fonts.getSize(12, Fonts.Type.INTER_DEFAULT);
                           italicFont.drawString(
                              context.getMatrices(), remainingText, this.x + 4.0F + textWidth, this.y + this.height / 2.0F - 1.0F, autocompleteColor
                           );
                        },
                        () -> this.findFirstMatchingSetting(searchText)
                           .ifPresent(
                              setting -> {
                                 String completion = setting.getLocalizedName();
                                 String remainingText = completion.substring(this.text.length());
                                 float textWidth = font.getStringWidth(this.text);
                                 FontRenderer italicFont = Fonts.getSize(12, Fonts.Type.INTER_DEFAULT);
                                 italicFont.drawString(
                                    context.getMatrices(), remainingText, this.x + 4.0F + textWidth, this.y + this.height / 2.0F - 1.0F, autocompleteColor
                                 );
                              }
                           )
                     );
               }

               int alpha = (int)(textAlpha * 255.0F) << 24;
               font.drawString(context.getMatrices(), displayText, this.x + 4.0F, this.y + this.height / 2.0F - 1.0F, ColorUtil.getDescription());
               scissor.pop();
               long currentTime = System.currentTimeMillis();
               boolean focused = typing && currentTime % 1000L < 500L;
               if (focused && textAlpha > 0.5F) {
                  float cursorX = font.getStringWidth(this.text.substring(0, this.cursorPosition));
                  int cursorColor = (int)(textAlpha * 255.0F) << 24 | 16777215;
                  rectangle.render(
                     ShapeProperties.create(matrix, this.x + 4.0F - this.xOffset + cursorX, this.y + this.height / 2.0F - 3.5F, 0.5, 7.0)
                        .color(cursorColor)
                        .build()
                  );
               }
            }
         }
      );
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height) && button == 0) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastClickTime < 250L && typing) {
            this.cursorPosition = this.text.length();
         } else {
            FriendsListComponent.typing = false;
            ConfigManagerComponent.typing = false;
            boolean wasTyping = typing;
            typing = true;
            this.lastClickTime = currentTime;
            if (wasTyping && this.animatedWidth > 73.0F) {
               this.cursorPosition = this.getCursorIndexAt(mouseX);
            } else {
               this.text = "";
               this.cursorPosition = 0;
               this.xOffset = 0.0F;
            }

            MenuScreen menuScreen = MenuScreen.INSTANCE;
            if (!ModuleCategory.SEARCH.equals(menuScreen.getCategory())) {
               this.previousCategory = menuScreen.getCategory();
            }

            if (!this.text.isEmpty()) {
               menuScreen.setCategory(ModuleCategory.SEARCH);
            }
         }
      } else if (typing) {
         typing = false;
         if (this.text.isEmpty()) {
            MenuScreen menuScreenx = MenuScreen.INSTANCE;
            if (ModuleCategory.SEARCH.equals(menuScreenx.getCategory())) {
               menuScreenx.setCategory(this.previousCategory);
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      float maxTextWidth = 59.0F;
      if (!typing || !(Fonts.getSize(12).getStringWidth(this.text) < maxTextWidth)) {
         return false;
      } else if ((chr == '/' || chr == '.') && this.text.isEmpty()) {
         return true;
      } else {
         if (this.text.isEmpty()) {
            MenuScreen menuScreen = MenuScreen.INSTANCE;
            if (!ModuleCategory.SEARCH.equals(menuScreen.getCategory())) {
               this.previousCategory = menuScreen.getCategory();
               menuScreen.setCategory(ModuleCategory.SEARCH);
            }
         }

         this.text = this.text.substring(0, this.cursorPosition) + chr + this.text.substring(this.cursorPosition);
         this.cursorPosition++;
         return true;
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (typing) {
         switch (keyCode) {
            case 257:
            case 259:
               this.handleTextModification(keyCode);
               break;
            case 258:
               this.handleTabCompletion();
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

   private void handleTextModification(int keyCode) {
      if (keyCode == 259) {
         if (this.cursorPosition > 0) {
            this.text = this.text.substring(0, this.cursorPosition - 1) + this.text.substring(this.cursorPosition);
            this.cursorPosition--;
            if (this.text.isEmpty()) {
               MenuScreen menuScreen = MenuScreen.INSTANCE;
               if (ModuleCategory.SEARCH.equals(menuScreen.getCategory())) {
                  menuScreen.setCategory(this.previousCategory);
               }
            }
         }
      } else if (keyCode == 257) {
         typing = false;
         if (this.text.isEmpty()) {
            MenuScreen menuScreen = MenuScreen.INSTANCE;
            if (ModuleCategory.SEARCH.equals(menuScreen.getCategory())) {
               menuScreen.setCategory(this.previousCategory);
            }
         }
      }
   }

   private void moveCursor(int keyCode) {
      if (keyCode == 263 && this.cursorPosition > 0) {
         this.cursorPosition--;
      } else if (keyCode == 262 && this.cursorPosition < this.text.length()) {
         this.cursorPosition++;
      }
   }

   private int getCursorIndexAt(double mouseX) {
      FontRenderer font = Fonts.getSize(12);
      float relativeX = (float)mouseX - this.x - 4.0F + this.xOffset;

      int position;
      for (position = 0; position < this.text.length(); position++) {
         float textWidth = font.getStringWidth(this.text.substring(0, position + 1));
         if (textWidth > relativeX) {
            break;
         }
      }

      return position;
   }

   private void updateXOffset(FontRenderer font, int cursorPosition) {
      float cursorX = font.getStringWidth(this.text.substring(0, cursorPosition));
      float availableWidth = this.width - 7.0F - 12.0F;
      if (cursorX < this.xOffset) {
         this.xOffset = cursorX;
      } else if (cursorX - this.xOffset > availableWidth) {
         this.xOffset = cursorX - availableWidth;
      }
   }

   private void handleTabCompletion() {
      if (!this.text.isEmpty()) {
         String searchText = this.text.toLowerCase();
         Main.getInstance()
            .getModuleProvider()
            .getModules()
            .stream()
            .filter(mod -> mod.getLocalizedName().toLowerCase().startsWith(searchText))
            .findFirst()
            .ifPresentOrElse(module -> {
               this.text = module.getLocalizedName();
               this.cursorPosition = this.text.length();
            }, () -> this.findFirstMatchingSetting(searchText).ifPresent(setting -> {
               this.text = setting.getLocalizedName();
               this.cursorPosition = this.text.length();
            }));
      }
   }

   private Optional<Setting> findFirstMatchingSetting(String searchText) {
      return Main.getInstance()
         .getModuleProvider()
         .getModules()
         .stream()
         .flatMap(module -> this.findMatchingSettingInModule(module, searchText).stream())
         .findFirst();
   }

   private Optional<Setting> findMatchingSettingInModule(Module module, String searchText) {
      return module.settings().stream().flatMap(setting -> this.findMatchingSettingRecursive(setting, searchText).stream()).findFirst();
   }

   private Optional<Setting> findMatchingSettingRecursive(Setting setting, String searchText) {
      if (setting.getLocalizedName().toLowerCase().startsWith(searchText)) {
         return Optional.of(setting);
      } else {
         return setting instanceof GroupSetting groupSetting
            ? groupSetting.getSubSettings().stream().flatMap(subSetting -> this.findMatchingSettingRecursive(subSetting, searchText).stream()).findFirst()
            : Optional.empty();
      }
   }

   public String getText() {
      return this.text;
   }

   public void setPreviousCategory(Category previousCategory) {
      this.previousCategory = previousCategory;
   }
}
