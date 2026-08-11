package padej.soup.api.event.events.item;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import padej.soup.api.event.events.Event;

public class HandScaleEvent implements Event {
   private final MatrixStack matrices;
   private final ItemStack stack;
   private final Hand hand;
   private float scale = 1.0F;

   public HandScaleEvent(MatrixStack matrices, ItemStack stack, Hand hand) {
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

   public float getScale() {
      return this.scale;
   }

   public void setScale(float scale) {
      this.scale = scale;
   }
}
