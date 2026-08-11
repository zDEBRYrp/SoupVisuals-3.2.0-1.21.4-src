package padej.soup.implement.events.item;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

@Deprecated
public class HandAnimationEvent extends padej.soup.api.event.events.item.HandAnimationEvent {
   public HandAnimationEvent(MatrixStack matrices, Hand hand, float swingProgress) {
      super(matrices, hand, swingProgress);
   }
}
