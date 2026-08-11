package padej.soup.implement.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import padej.protect.KeepName;
import padej.soup.api.feature.module.Category;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.LinearAnimation;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.render.Render2DUtil;
import padej.soup.implement.menu.components.AbstractComponent;
import padej.soup.implement.menu.components.implement.other.BackgroundComponent;
import padej.soup.implement.menu.components.implement.other.CategoryContainerComponent;
import padej.soup.implement.menu.components.implement.other.HomeComponent;
import padej.soup.implement.menu.components.implement.other.ModuleDescriptionComponent;
import padej.soup.implement.menu.components.implement.other.SearchComponent;
import padej.soup.implement.menu.components.implement.other.UserComponent;
import padej.soup.implement.menu.components.implement.settings.TextComponent;
import padej.soup.implement.menu.components.implement.settings.multiselect.MultiSelectComponent;
import padej.soup.implement.menu.components.implement.settings.select.SelectComponent;

public class MenuScreen extends Screen implements QuickImports {
   public static MenuScreen INSTANCE = new MenuScreen();
   private final List<AbstractComponent> components = new ArrayList<>();
   private final BackgroundComponent backgroundComponent = new BackgroundComponent();
   private final UserComponent userComponent = new UserComponent();
   private final SearchComponent searchComponent = new SearchComponent();
   private final CategoryContainerComponent categoryContainerComponent = new CategoryContainerComponent();
   private final HomeComponent homeComponent = new HomeComponent();
   private final ModuleDescriptionComponent moduleDescriptionComponent = new ModuleDescriptionComponent();
   public final Animation animation = new LinearAnimation().setMs(200).setValue(1.0);
   public Category category = ModuleCategory.VISUALS;
   public int x;
   public int y;
   public int width;
   public int height;
   private boolean menuDragging = false;
   private int dragOffsetX = 0;
   private int dragOffsetY = 0;
   private int customX = -1;
   private int customY = -1;

   public MenuScreen() {
      super(Text.of("MenuScreen"));
      this.initialize();
   }

   public void setCategory(Category category) {
      SelectComponent.closeAllDropdowns();
      MultiSelectComponent.closeAllDropdowns();
      this.category = category;
   }

   public void refreshModules() {
      this.categoryContainerComponent.initializeCategoryComponents();
   }

   public void initialize() {
      this.animation.setDirection(Direction.FORWARDS);
      this.categoryContainerComponent.initializeCategoryComponents();
      this.backgroundComponent.setSearchComponent(this.searchComponent);
      this.components
         .addAll(
            Arrays.asList(
               this.backgroundComponent,
               this.userComponent,
               this.searchComponent,
               this.categoryContainerComponent,
               this.homeComponent,
               this.moduleDescriptionComponent
            )
         );
   }

   @KeepName
   public void tick() {
      this.close();

      for (AbstractComponent component : this.components) {
         component.tick();
      }

      super.tick();
   }

   @KeepName
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.width = 322;
      this.height = 233;
      int scaledWidth = window.getScaledWidth();
      int scaledHeight = window.getScaledHeight();
      if (this.customX != -1 && this.customY != -1) {
         this.x = this.customX;
         this.y = this.customY;
      } else {
         this.x = (scaledWidth - this.width) / 2;
         this.y = (scaledHeight - this.height) / 2;
      }

      blur.resetRegion();
      blur.expandRegion(this.x, this.y, this.width, this.height);
      blur.setup();
      float centerX = this.x + this.width / 2.0F;
      float centerY = this.y + this.height / 2.0F;
      this.backgroundComponent.position(this.x, this.y).size(this.width, this.height);
      this.userComponent.position(this.x + 257, this.y + 36);
      this.searchComponent.position(this.x + 35, this.y + 5);
      this.categoryContainerComponent.position(this.x, this.y);
      this.homeComponent.position(this.x + 8, this.y + 5);
      float scale = this.getScaleAnimation();
      int transformedMouseX = mouseX;
      int transformedMouseY = mouseY;
      if (scale != 1.0F) {
         float scaleFactor = 0.5F + scale / 2.0F;
         transformedMouseX = (int)(centerX + (mouseX - centerX) / scaleFactor);
         transformedMouseY = (int)(centerY + (mouseY - centerY) / scaleFactor);
      }

      int finalMouseX = transformedMouseX;
      int finalMouseY = transformedMouseY;
      MathUtil.scale(context.getMatrices(), centerX, centerY, scale, () -> {
         AbstractComponent.setRenderMatrix(context.getMatrices().peek().getPositionMatrix());

         for (AbstractComponent component : this.components) {
            component.render(context, finalMouseX, finalMouseY, delta);
         }

         windowManager.render(context, finalMouseX, finalMouseY, delta);
         SelectComponent.renderOpenDropdowns(context, finalMouseX, finalMouseY, delta);
         MultiSelectComponent.renderOpenDropdowns(context, finalMouseX, finalMouseY, delta);
      });
      if (this.menuDragging) {
         int windowWidth = window.getScaledWidth();
         int windowHeight = window.getScaledHeight();
         int radius = 3;
         float mouseDragX = mouseX + this.dragOffsetX;
         float mouseDragY = mouseY + this.dragOffsetY;
         if (Math.abs(mouseDragX - (windowWidth - this.width) / 2.0F) <= radius) {
            this.drawRect(windowWidth / 2.0F - 0.5F, 0.0F, 1.0F, windowHeight);
         }

         if (Math.abs(mouseDragY - (windowHeight - this.height) / 2.0F) <= radius) {
            this.drawRect(0.0F, windowHeight / 2.0F - 0.5F, windowWidth, 1.0F);
         }
      }

      super.render(context, mouseX, mouseY, delta);
   }

   private void drawRect(float x, float y, float width, float height) {
      Render2DUtil.drawQuad(x, y, width, height, ColorUtil.getText(0.5F));
   }

   public void openGui() {
      this.animation.setDirection(Direction.FORWARDS);
      mc.setScreen(this);
      SoundManager.playSound(SoundManager.OPEN_GUI);
   }

   public float getScaleAnimation() {
      return this.animation.getOutput().floatValue();
   }

   private double transformMouseX(double mouseX) {
      float scale = this.getScaleAnimation();
      if (scale == 1.0F) {
         return mouseX;
      } else {
         float scaleFactor = 0.5F + scale / 2.0F;
         float centerX = this.x + this.width / 2.0F;
         return centerX + (mouseX - centerX) / scaleFactor;
      }
   }

   private double transformMouseY(double mouseY) {
      float scale = this.getScaleAnimation();
      if (scale == 1.0F) {
         return mouseY;
      } else {
         float scaleFactor = 0.5F + scale / 2.0F;
         float centerY = this.y + this.height / 2.0F;
         return centerY + (mouseY - centerY) / scaleFactor;
      }
   }

   @KeepName
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      double transformedMouseX = this.transformMouseX(mouseX);
      double transformedMouseY = this.transformMouseY(mouseY);
      int headerHeight = 25;
      if (button == 0 && MathUtil.isHovered(transformedMouseX, transformedMouseY, this.x, this.y, this.width, headerHeight)) {
         boolean clickedOnComponent = false;

         for (AbstractComponent component : this.components) {
            if ((component == this.searchComponent || component == this.homeComponent || component == this.userComponent)
               && component.mouseClicked(transformedMouseX, transformedMouseY, button)) {
               clickedOnComponent = true;
               break;
            }
         }

         if (!clickedOnComponent) {
            this.menuDragging = true;
            this.dragOffsetX = (int)(this.x - transformedMouseX);
            this.dragOffsetY = (int)(this.y - transformedMouseY);
            return true;
         }
      }

      if (!windowManager.mouseClicked(transformedMouseX, transformedMouseY, button)) {
         for (AbstractComponent componentx : this.components) {
            if (componentx.mouseClicked(transformedMouseX, transformedMouseY, button)) {
               return true;
            }
         }

         SelectComponent.handleGlobalClick(transformedMouseX, transformedMouseY);
         MultiSelectComponent.handleGlobalClick(transformedMouseX, transformedMouseY);
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @KeepName
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      double transformedMouseX = this.transformMouseX(mouseX);
      double transformedMouseY = this.transformMouseY(mouseY);
      if (this.menuDragging && button == 0) {
         this.menuDragging = false;
         return true;
      } else {
         for (AbstractComponent component : this.components) {
            component.mouseReleased(transformedMouseX, transformedMouseY, button);
         }

         windowManager.mouseReleased(transformedMouseX, transformedMouseY, button);
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   @KeepName
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      double transformedMouseX = this.transformMouseX(mouseX);
      double transformedMouseY = this.transformMouseY(mouseY);
      if (this.menuDragging) {
         float mouseDragX = (float)(transformedMouseX + this.dragOffsetX);
         float mouseDragY = (float)(transformedMouseY + this.dragOffsetY);
         int windowWidth = window.getScaledWidth();
         int windowHeight = window.getScaledHeight();
         this.customX = (int)Math.max(0.0F, Math.min(mouseDragX, (float)(windowWidth - this.width)));
         this.customY = (int)Math.max(0.0F, Math.min(mouseDragY, (float)(windowHeight - this.height)));
         int radius = 3;
         if (Math.abs(mouseDragX - (windowWidth - this.width) / 2.0F) <= radius) {
            this.customX = (windowWidth - this.width) / 2;
         }

         if (Math.abs(mouseDragY - (windowHeight - this.height) / 2.0F) <= radius) {
            this.customY = (windowHeight - this.height) / 2;
         }

         return true;
      } else {
         if (!windowManager.mouseDragged(transformedMouseX, transformedMouseY, button, deltaX, deltaY)) {
            for (AbstractComponent component : this.components) {
               component.mouseDragged(transformedMouseX, transformedMouseY, button, deltaX, deltaY);
            }
         }

         return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
      }
   }

   @KeepName
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      double transformedMouseX = this.transformMouseX(mouseX);
      double transformedMouseY = this.transformMouseY(mouseY);
      if (!windowManager.mouseScrolled(transformedMouseX, transformedMouseY, verticalAmount)) {
         for (AbstractComponent component : this.components) {
            component.mouseScrolled(transformedMouseX, transformedMouseY, verticalAmount);
         }
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @KeepName
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256 && this.shouldCloseOnEsc()) {
         if (this.animation.isDirection(Direction.FORWARDS)) {
            SoundManager.playSound(SoundManager.CLOSE_GUI);
            this.animation.setDirection(Direction.BACKWARDS);
         }

         return true;
      } else if (keyCode == 70 && (modifiers & 2) != 0 && !SearchComponent.typing) {
         SearchComponent.typing = true;
         this.searchComponent.setText("");
         this.searchComponent.setCursorPosition(0);
         this.searchComponent.setPreviousCategory((Category)(!this.category.equals(ModuleCategory.SEARCH) ? this.category : ModuleCategory.VISUALS));
         return true;
      } else if (keyCode == 86 && (modifiers & 2) != 0) {
         if (!windowManager.keyPressed(keyCode, scanCode, modifiers)) {
            for (AbstractComponent component : this.components) {
               component.keyPressed(keyCode, scanCode, modifiers);
            }
         }

         return true;
      } else {
         if (!windowManager.keyPressed(keyCode, scanCode, modifiers)) {
            for (AbstractComponent component : this.components) {
               component.keyPressed(keyCode, scanCode, modifiers);
            }
         }

         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   @KeepName
   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      if (!windowManager.keyReleased(keyCode, scanCode, modifiers)) {
         for (AbstractComponent component : this.components) {
            component.keyReleased(keyCode, scanCode, modifiers);
         }
      }

      return super.keyReleased(keyCode, scanCode, modifiers);
   }

   @KeepName
   public boolean charTyped(char chr, int modifiers) {
      if (!windowManager.charTyped(chr, modifiers)) {
         for (AbstractComponent component : this.components) {
            component.charTyped(chr, modifiers);
         }
      }

      return super.charTyped(chr, modifiers);
   }

   @KeepName
   public boolean shouldPause() {
      return false;
   }

   @KeepName
   public void close() {
      if (this.animation.isFinished(Direction.BACKWARDS)) {
         TextComponent.typing = false;
         SelectComponent.closeAllDropdowns();
         MultiSelectComponent.closeAllDropdowns();
         super.close();
      }
   }

   public void setX(int x) {
      this.x = x;
   }

   public void setY(int y) {
      this.y = y;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public void setMenuDragging(boolean menuDragging) {
      this.menuDragging = menuDragging;
   }

   public void setDragOffsetX(int dragOffsetX) {
      this.dragOffsetX = dragOffsetX;
   }

   public void setDragOffsetY(int dragOffsetY) {
      this.dragOffsetY = dragOffsetY;
   }

   public void setCustomX(int customX) {
      this.customX = customX;
   }

   public void setCustomY(int customY) {
      this.customY = customY;
   }

   public List<AbstractComponent> getComponents() {
      return this.components;
   }

   public BackgroundComponent getBackgroundComponent() {
      return this.backgroundComponent;
   }

   public UserComponent getUserComponent() {
      return this.userComponent;
   }

   public SearchComponent getSearchComponent() {
      return this.searchComponent;
   }

   public CategoryContainerComponent getCategoryContainerComponent() {
      return this.categoryContainerComponent;
   }

   public HomeComponent getHomeComponent() {
      return this.homeComponent;
   }

   public ModuleDescriptionComponent getModuleDescriptionComponent() {
      return this.moduleDescriptionComponent;
   }

   public Animation getAnimation() {
      return this.animation;
   }

   public Category getCategory() {
      return this.category;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public boolean isMenuDragging() {
      return this.menuDragging;
   }

   public int getDragOffsetX() {
      return this.dragOffsetX;
   }

   public int getDragOffsetY() {
      return this.dragOffsetY;
   }

   public int getCustomX() {
      return this.customX;
   }

   public int getCustomY() {
      return this.customY;
   }
}
