package padej.soup.implement.menu.components.implement.other;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import padej.soup.api.feature.module.Category;
import padej.soup.api.feature.module.CustomCategory;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.render.ScissorManager;
import padej.soup.core.Main;
import padej.soup.implement.menu.MenuScreen;
import padej.soup.implement.menu.components.AbstractComponent;
import padej.soup.implement.menu.components.implement.category.CategoryComponent;
import padej.soup.implement.menu.components.implement.settings.multiselect.MultiSelectComponent;
import padej.soup.implement.menu.components.implement.settings.select.SelectComponent;

public class CategoryContainerComponent extends AbstractComponent {
   private static final List<Category> EXCLUDED_CATEGORIES = List.of(ModuleCategory.PERSONAL_INFO, ModuleCategory.SEARCH, ModuleCategory.HOME);
   private static final float ICONS_START_Y = 49.0F;
   private static final float ICON_STEP = 25.0F;
   private static final float SCROLL_BOTTOM_MARGIN = 8.0F;
   private static final float SCROLL_SPEED = 20.0F;
   private final List<CategoryComponent> categoryComponents = new ArrayList<>();
   private float selectionX = 0.0F;
   private float selectionContentY = 0.0F;
   private Category previousCategory = null;
   private final Animation selectionColorAnimation = new DecelerateAnimation().setMs(300).setValue(1.0);
   private float categoryScrollTarget = 0.0F;
   private float categoryScrollSmoothed = 0.0F;

   public void initializeCategoryComponents() {
      this.categoryComponents.clear();
      Set<Category> addedCategories = new LinkedHashSet<>();

      for (ModuleCategory category : ModuleCategory.values()) {
         if (!EXCLUDED_CATEGORIES.contains(category) && !category.isHidden()) {
            addedCategories.add(category);
            this.categoryComponents.add(new CategoryComponent(category));
         }
      }

      for (Module module : Main.getInstance().getModuleRepository().modules()) {
         Category cat = module.getCategory();
         if (cat instanceof CustomCategory && !addedCategories.contains(cat) && !cat.isHidden()) {
            addedCategories.add(cat);
            this.categoryComponents.add(new CategoryComponent(cat));
         }
      }

      this.categoryComponents.add(new CategoryComponent(ModuleCategory.HOME));
      this.categoryComponents.add(new CategoryComponent(ModuleCategory.PERSONAL_INFO));
      this.categoryComponents.add(new CategoryComponent(ModuleCategory.SEARCH));
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      Category currentCategory = menuScreen.getCategory();
      ScissorManager scissorManager = Main.getInstance().getScissorManager();
      float availableHeight = menuScreen.height - 49.0F - 8.0F;
      List<CategoryComponent> scrollableComponents = this.categoryComponents.stream().filter(c -> !EXCLUDED_CATEGORIES.contains(c.getCategory())).toList();
      float totalContentHeight = scrollableComponents.size() * 25.0F - (scrollableComponents.isEmpty() ? 0.0F : 14.0F);
      float maxScroll = Math.max(0.0F, totalContentHeight - availableHeight);
      this.categoryScrollTarget = MathHelper.clamp(this.categoryScrollTarget, 0.0F, maxScroll);
      this.categoryScrollSmoothed = MathUtil.interpolateSmooth(4.0, this.categoryScrollSmoothed, this.categoryScrollTarget);
      float offset = 0.0F;
      float targetX = 0.0F;
      float targetContentY = 0.0F;
      boolean foundTarget = false;

      for (CategoryComponent component : this.categoryComponents) {
         if (!EXCLUDED_CATEGORIES.contains(component.getCategory())) {
            component.x = this.x + 11.0F;
            component.y = this.y + 49.0F + offset - this.categoryScrollSmoothed;
            component.width = 11.0F;
            component.height = 11.0F;
            if (component.getCategory().equals(currentCategory)) {
               targetX = component.x;
               targetContentY = this.y + 49.0F + offset;
               foundTarget = true;
            }

            offset += 25.0F;
         }
      }

      if (!currentCategory.equals(this.previousCategory) && !EXCLUDED_CATEGORIES.contains(currentCategory)) {
         if (this.previousCategory != null && !EXCLUDED_CATEGORIES.contains(this.previousCategory)) {
            this.selectionColorAnimation.setDirection(Direction.BACKWARDS);
            this.selectionColorAnimation.reset();
            this.selectionColorAnimation.setDirection(Direction.FORWARDS);
         } else {
            this.selectionX = targetX;
            this.selectionContentY = targetContentY;
            this.selectionColorAnimation.setDirection(Direction.FORWARDS);
         }

         this.previousCategory = currentCategory;
      } else if (!currentCategory.equals(this.previousCategory)) {
         this.previousCategory = currentCategory;
      }

      if (foundTarget && !EXCLUDED_CATEGORIES.contains(currentCategory)) {
         if (menuScreen.isMenuDragging()) {
            this.selectionX = targetX;
            this.selectionContentY = targetContentY;
         } else {
            this.selectionX = MathUtil.interpolateSmooth(4.0, this.selectionX, targetX);
            this.selectionContentY = MathUtil.interpolateSmooth(4.0, this.selectionContentY, targetContentY);
         }
      }

      for (CategoryComponent componentx : this.categoryComponents) {
         componentx.render(context, mouseX, mouseY, delta);
      }

      float clipX = this.x + 5.0F;
      float clipY = this.y + 49.0F - 3.0F;
      float clipW = 28.0F;
      float clipH = availableHeight + 6.0F;
      scissorManager.push(renderMatrix, clipX, clipY, clipW, clipH);
      if (foundTarget && !EXCLUDED_CATEGORIES.contains(currentCategory)) {
         float squareSize = 17.0F;
         float iconSize = 11.0F;
         float selectionScreenY = this.selectionContentY - this.categoryScrollSmoothed;
         float squareX = this.selectionX + (iconSize - squareSize) / 2.0F;
         float squareY = selectionScreenY + (iconSize - squareSize) / 2.0F;
         float colorProgress = this.selectionColorAnimation.getOutput().floatValue();
         int outlineColor = ColorUtil.overCol(ColorUtil.getOutline(), ColorUtil.getClientColor(), colorProgress);
         rectangle.render(
            ShapeProperties.create(context.getMatrices(), squareX, squareY, squareSize, squareSize)
               .round(3.0F)
               .thickness(2.0F)
               .softness(1.0F)
               .outlineColor(outlineColor)
               .color(ColorUtil.getGuiRectColor(0.5F))
               .build()
         );
      }

      for (CategoryComponent componentx : this.categoryComponents) {
         componentx.renderIcon(context, mouseX, mouseY);
      }

      scissorManager.pop();
   }

   @Override
   public void tick() {
      for (CategoryComponent categoryComponent : this.categoryComponents) {
         categoryComponent.tick();
      }

      super.tick();
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (SelectComponent.handleMouseClicked(mouseX, mouseY, button)) {
         return true;
      } else if (MultiSelectComponent.handleMouseClicked(mouseX, mouseY, button)) {
         return true;
      } else {
         for (CategoryComponent categoryComponent : this.categoryComponents) {
            if (categoryComponent.mouseClicked(mouseX, mouseY, button)) {
               return true;
            }
         }

         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      for (CategoryComponent categoryComponent : this.categoryComponents) {
         categoryComponent.mouseReleased(mouseX, mouseY, button);
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      for (CategoryComponent categoryComponent : this.categoryComponents) {
         categoryComponent.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
      }

      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      boolean overSidebar = MathUtil.isHovered(mouseX, mouseY, this.x + 5.0F, this.y + 49.0F, 28.0, menuScreen.height - 49.0F - 8.0F);
      if (overSidebar) {
         this.categoryScrollTarget -= (float)amount * 20.0F;
         return true;
      } else {
         for (CategoryComponent categoryComponent : this.categoryComponents) {
            categoryComponent.mouseScrolled(mouseX, mouseY, amount);
         }

         return super.mouseScrolled(mouseX, mouseY, amount);
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      for (CategoryComponent categoryComponent : this.categoryComponents) {
         categoryComponent.keyPressed(keyCode, scanCode, modifiers);
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      for (CategoryComponent categoryComponent : this.categoryComponents) {
         categoryComponent.charTyped(chr, modifiers);
      }

      return super.charTyped(chr, modifiers);
   }

   public CategoryContainerComponent setSelectionX(float selectionX) {
      this.selectionX = selectionX;
      return this;
   }

   public CategoryContainerComponent setSelectionContentY(float selectionContentY) {
      this.selectionContentY = selectionContentY;
      return this;
   }

   public CategoryContainerComponent setPreviousCategory(Category previousCategory) {
      this.previousCategory = previousCategory;
      return this;
   }

   public CategoryContainerComponent setCategoryScrollTarget(float categoryScrollTarget) {
      this.categoryScrollTarget = categoryScrollTarget;
      return this;
   }

   public CategoryContainerComponent setCategoryScrollSmoothed(float categoryScrollSmoothed) {
      this.categoryScrollSmoothed = categoryScrollSmoothed;
      return this;
   }
}
