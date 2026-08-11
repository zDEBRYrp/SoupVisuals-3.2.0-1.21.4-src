package padej.soup.api.event.events.item;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import padej.soup.api.event.events.Event;

public class HandOffsetEvent implements Event {
   private MatrixStack matrices;
   private ItemStack stack;
   private Hand hand;

   public HandOffsetEvent(MatrixStack matrices, ItemStack stack, Hand hand) {
      this.matrices = matrices;
      this.stack = stack;
      this.hand = hand;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public Hand getHand() {
      return this.hand;
   }

   public void setMatrices(MatrixStack matrices) {
      this.matrices = matrices;
   }

   public void setStack(ItemStack stack) {
      this.stack = stack;
   }

   public void setHand(Hand hand) {
      this.hand = hand;
   }
}
