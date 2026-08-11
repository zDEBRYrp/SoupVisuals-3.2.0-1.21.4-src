package padej.soup.implement.events.item;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@Deprecated
public class HandOffsetEvent extends padej.soup.api.event.events.item.HandOffsetEvent {
   public HandOffsetEvent(MatrixStack matrices, ItemStack stack, Hand hand) {
      super(matrices, stack, hand);
   }
}
