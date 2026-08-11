package padej.soup.mixins;

import net.minecraft.client.world.ClientWorld.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.base.QuickImports;
import padej.soup.implement.features.modules.world.Time;

@ProtIgnore
@Mixin({Properties.class})
public class ClientWorldPropertiesMixin implements QuickImports {
   @Shadow
   private long field_24439;

   @Inject(
      method = {"setTimeOfDay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void setTimeOfDayHook(long timeOfDay, CallbackInfo ci) {
      Time time = Time.getInstance();
      if (time != null && time.isEnabled()) {
         this.field_24439 = time.getTimeSetting().getInt() * 1000L;
         ci.cancel();
      }
   }
}
