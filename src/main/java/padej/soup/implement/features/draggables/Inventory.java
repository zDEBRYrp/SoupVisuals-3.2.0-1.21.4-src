package padej.soup.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.pipeline.HudRenderPipeline;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;
import padej.soup.core.perftest.HudProfiler;
import padej.soup.implement.features.modules.hud.Icons;

public class Inventory extends AbstractDraggable {
   private final List<ItemStack> stacks = new ArrayList<>(27);
   private boolean cachedShowHeader = false;
   private boolean cachedDarkenHeader = false;
   private int cachedIconColor = 0;

   public Inventory() {
      super("Inventory", 390, 10, 123, 60, true);
   }

   @Override
   public boolean visible() {
      for (ItemStack stack : this.stacks) {
         if (!stack.isEmpty()) {
            return true;
         }
      }

      return PlayerIntersectionUtil.isChatOrMenu(mc.currentScreen);
   }

   @Override
   public void tick() {
      this.stacks.clear();
      if (mc.player != null) {
         for (int i = 9; i < 36; i++) {
            this.stacks.add(mc.player.inventory.getStack(i));
         }

         padej.soup.implement.features.modules.hud.Inventory inventoryModule = padej.soup.implement.features.modules.hud.Inventory.getInstance();
         this.cachedShowHeader = inventoryModule.getShowHeader().isValue();
         this.cachedDarkenHeader = inventoryModule.getDarkenHeader().isValue();
         Icons iconsModule = Icons.getInstance();
         if (!iconsModule.getColoredIcons().isValue()) {
            this.cachedIconColor = ColorUtil.getText();
         } else if (iconsModule.getIconGradient().isValue()) {
            this.cachedIconColor = ColorUtil.fade(8);
         } else {
            this.cachedIconColor = ColorUtil.getClientColor();
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      float headerHeight = 16.0F;
      float padding = 5.0F;
      boolean showHeader = this.cachedShowHeader;
      boolean darkenHeader = this.cachedDarkenHeader;
      int itemsPerRow = 9;
      int totalItems = 27;
      int itemSize = 13;
      int itemPadding = 4;
      int rows = (int)Math.ceil((double)totalItems / itemsPerRow);
      float contentHeight = rows * itemSize + itemPadding * 2;
      float totalHeight = showHeader ? headerHeight + contentHeight : contentHeight;
      long t0 = HudProfiler.nano();
      blur.render(
         ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), totalHeight)
            .round(4.0F)
            .softness(1.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getBlurRect(0.7F))
            .build()
      );
      if (showHeader && darkenHeader) {
         rectangle.render(
            ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), headerHeight)
               .round(4.0F, 0.0F, 4.0F, 0.0F)
               .softness(-0.5F)
               .thickness(0.0F)
               .color(ColorUtil.getRectDarker(0.9F))
               .build()
         );
      }

      if (showHeader) {
         Fonts.getSize(15, Fonts.Type.INTER_DEFAULT)
            .drawString(matrix, this.getName(), (int)(this.getX() + padding), (int)(this.getY() + 6.5F), ColorUtil.getText());
         float iconSize = 8.0F;
         float iconPadding = 4.0F;
         image.setIcon(61449)
            .render(
               ShapeProperties.create(matrix, this.getX() + this.getWidth() - iconSize - iconPadding, this.getY() + 4, iconSize, iconSize)
                  .color(this.cachedIconColor)
                  .build()
            );
      }

      HudProfiler.recordComponent("INV:BG+Header", t0);
      int startOffsetY = showHeader ? (int)(headerHeight + itemPadding) : itemPadding;
      HudRenderPipeline.getInstance().recordVanilla(() -> {
         long ti = HudProfiler.nano();
         int oy = startOffsetY;
         int ox = itemPadding;

         for (ItemStack stack : this.stacks) {
            if (!stack.isEmpty()) {
               context.getMatrices().push();
               context.getMatrices().translate(this.getX() + ox + 1, this.getY() + oy + 1, 0.0F);
               context.getMatrices().scale(0.5F, 0.5F, 1.0F);
               context.drawItem(stack, 0, 0);
               context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
               context.draw();
               context.getMatrices().pop();
            }

            ox += itemSize;
            if (ox > this.getWidth() - itemSize) {
               oy += itemSize;
               ox = itemPadding;
            }
         }

         HudProfiler.recordComponent("INV:Items", ti);
      }, HudRenderPipeline.VanillaLayer.AFTER_RECT);
      this.setHeight((int)totalHeight);
   }
}
