package padej.soup.implement.features.draggables;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.pipeline.HudRenderPipeline;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;

public class Consumable extends AbstractDraggable {
   private static final Map<String, Item> ITEM_MAP = new LinkedHashMap<>();
   private final Map<String, Integer> itemCounts = new LinkedHashMap<>();
   private final Map<String, ItemStack> cachedStacks = new LinkedHashMap<>();
   private boolean cachedShowBackground = false;
   private boolean cachedShowCount = false;
   private String cachedLayout = "Line";
   private float cachedScale = 1.0F;

   public Consumable() {
      super("Consumable", 400, 200, 140, 80, true);
   }

   @Override
   public boolean visible() {
      return mc.player != null || PlayerIntersectionUtil.isChat(mc.currentScreen);
   }

   @Override
   public void tick() {
      if (mc.player != null) {
         this.itemCounts.clear();
         this.cachedStacks.clear();
         padej.soup.implement.features.modules.hud.Consumable consumableModule = padej.soup.implement.features.modules.hud.Consumable.getInstance();
         this.cachedShowBackground = consumableModule.getShowBackground().isValue();
         this.cachedShowCount = consumableModule.getShowCount().isValue();
         this.cachedLayout = consumableModule.getLayout().getSelected();
         this.cachedScale = consumableModule.getScale().getValue();

         for (String itemType : consumableModule.getItemTypes().getSelected()) {
            Item item = ITEM_MAP.get(itemType);
            if (item != null) {
               int count = this.countItem(item);
               if (count > 0) {
                  this.itemCounts.put(itemType, count);
                  this.cachedStacks.put(itemType, new ItemStack(item, count));
               }
            }
         }
      }
   }

   private int countItem(Item item) {
      if (mc.player == null) {
         return 0;
      } else {
         int sum = 0;
         int size = mc.player.getInventory().size();

         for (int i = 0; i < size; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
               sum += stack.getCount();
            }
         }

         return sum;
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      boolean showBackground = this.cachedShowBackground;
      boolean showCount = this.cachedShowCount;
      String layout = this.cachedLayout;
      float scale = this.cachedScale;
      float padding = 3.0F;
      float baseItemSize = 16.0F;
      float itemSize = baseItemSize * scale + 2.0F;
      int visibleItems = this.itemCounts.size();
      if (visibleItems != 0) {
         int cols = 1;
         int rows = 1;
         float width = 0.0F;
         float height = 0.0F;
         switch (layout) {
            case "Line":
               cols = visibleItems;
               rows = 1;
               width = visibleItems * itemSize + padding * 2.0F;
               height = itemSize + padding * 2.0F;
               break;
            case "Column":
               cols = 1;
               width = itemSize + padding * 2.0F;
               height = visibleItems * itemSize + padding * 2.0F;
               break;
            case "Table":
               cols = (int)Math.ceil(Math.sqrt(visibleItems));
               rows = (int)Math.ceil((double)visibleItems / cols);
               width = cols * itemSize + padding * 2.0F;
               height = rows * itemSize + padding * 2.0F;
         }

         this.setWidth((int)width);
         this.setHeight((int)height);
         if (showBackground) {
            blur.render(
               ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), this.getHeight())
                  .quality(25.0F)
                  .round(4.0F)
                  .softness(1.0F)
                  .thickness(2.0F)
                  .outlineColor(ColorUtil.getOutline())
                  .color(ColorUtil.getBlurRect(0.7F))
                  .build()
            );
         }

         float startY = this.getY() + padding;
         float startX = this.getX() + padding;
         int fCols = cols;
         HudRenderPipeline.getInstance().recordVanilla(() -> {
            int idx = 0;

            for (Entry<String, Integer> entry : this.itemCounts.entrySet()) {
               String itemType = entry.getKey();
               int row = 0;
               int col = 0;
               switch (layout) {
                  case "Line":
                     col = idx;
                     row = 0;
                     break;
                  case "Column":
                     col = 0;
                     row = idx;
                     break;
                  case "Table":
                     row = idx / fCols;
                     col = idx % fCols;
               }

               float x = startX + col * itemSize;
               float y = startY + row * itemSize;
               ItemStack stack = this.cachedStacks.get(itemType);
               if (stack != null) {
                  matrix.push();
                  matrix.translate(x, y, 0.0F);
                  matrix.scale(scale, scale, 1.0F);
                  context.drawItem(stack, 0, 0);
                  if (showCount) {
                     context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
                  }

                  matrix.pop();
               }

               idx++;
            }
         }, HudRenderPipeline.VanillaLayer.AFTER_BLUR);
      }
   }

   static {
      ITEM_MAP.put("Snowball", Items.SNOWBALL);
      ITEM_MAP.put("Egg", Items.EGG);
      ITEM_MAP.put("Wind Charge", Items.WIND_CHARGE);
      ITEM_MAP.put("Golden Apple", Items.GOLDEN_APPLE);
      ITEM_MAP.put("Enchanted Golden Apple", Items.ENCHANTED_GOLDEN_APPLE);
      ITEM_MAP.put("Arrow", Items.ARROW);
      ITEM_MAP.put("Spectral Arrow", Items.SPECTRAL_ARROW);
      ITEM_MAP.put("Tipped Arrow", Items.TIPPED_ARROW);
      ITEM_MAP.put("Totem", Items.TOTEM_OF_UNDYING);
      ITEM_MAP.put("Chorus Fruit", Items.CHORUS_FRUIT);
      ITEM_MAP.put("Ender Pearl", Items.ENDER_PEARL);
      ITEM_MAP.put("Firework Rocket", Items.FIREWORK_ROCKET);
      ITEM_MAP.put("Experience Bottle", Items.EXPERIENCE_BOTTLE);
   }
}
