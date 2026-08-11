package padej.soup.mixins;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import padej.protect.ProtIgnore;

@ProtIgnore
@Mixin({KeyBinding.class})
public class KeyBindingMixin {
   @Shadow
   private Key field_1655;

   @Inject(
      method = {"getBoundKeyLocalizedText"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetBoundKeyLocalizedText(CallbackInfoReturnable<Text> cir) {
      cir.setReturnValue(this.field_1655.getLocalizedText());
   }
}
