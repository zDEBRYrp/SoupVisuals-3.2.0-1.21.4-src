package padej.soup.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import padej.soup.api.accessor.ICapabilityTracker;

@Mixin(
   targets = {"com.mojang.blaze3d.platform.GlStateManager$CapabilityTracker"}
)
public abstract class CapabilityTrackerMixin implements ICapabilityTracker {
   @Shadow
   private boolean field_5051;

   @Shadow
   public abstract void method_4470(boolean var1);

   @Override
   public boolean soup$get() {
      return this.field_5051;
   }

   @Override
   public void soup$set(boolean state) {
      this.method_4470(state);
   }
}
