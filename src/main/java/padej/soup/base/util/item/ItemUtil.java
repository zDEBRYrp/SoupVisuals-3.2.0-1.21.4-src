package padej.soup.base.util.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import padej.soup.base.QuickImports;

public final class ItemUtil implements QuickImports {
   public static int maxUseTick(Item item) {
      return maxUseTick(item.getDefaultStack());
   }

   public static int maxUseTick(ItemStack stack) {
      return switch (stack.getUseAction()) {
         case EAT, DRINK -> 32;
         case CROSSBOW, SPEAR -> 10;
         case BOW -> 20;
         case BLOCK -> 0;
         default -> stack.getMaxUseTime(mc.player);
      };
   }

   private ItemUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
