package padej.soup.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.LightmapTextureManager;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.render.LightmapUpdateEvent;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.features.modules.world.Bright;
import padej.soup.implement.features.modules.world.Light;

@ProtIgnore
@Mixin({LightmapTextureManager.class})
public class LightmapTextureManagerMixin {
   @Unique
   private final Vector3f cachedBlockLightVec = new Vector3f();
   @Unique
   private final Vector3f cachedSkyLightVec = new Vector3f();

   @ModifyExpressionValue(
      method = {"update(F)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"
      )}
   )
   private Object injectFullBright(Object original) {
      Bright bright = Bright.getInstance();
      return bright != null && bright.isState() ? Math.max((Double)original, (double)(bright.getBrightSetting().getValue() * 10.0F)) : original;
   }

   @ModifyArg(
      method = {"update(F)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gl/Uniform;set(Lorg/joml/Vector3f;)V",
         ordinal = 0
      ),
      index = 0
   )
   private Vector3f injectBlockLightColor(Vector3f original) {
      Light light = Light.getInstance();
      if (light != null && light.isState()) {
         int color = light.getUseCustomLightColor().isValue() ? light.getLightColor().getColor() : ColorUtil.getClientColor();
         this.cachedBlockLightVec.set(ColorUtil.redf(color), ColorUtil.greenf(color), ColorUtil.bluef(color));
         return this.cachedBlockLightVec;
      } else {
         return original;
      }
   }

   @ModifyArg(
      method = {"update(F)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gl/Uniform;set(Lorg/joml/Vector3f;)V",
         ordinal = 1
      ),
      index = 0
   )
   private Vector3f injectSkyLightColor(Vector3f original) {
      Light light = Light.getInstance();
      if (light != null && light.isState()) {
         int color = light.getUseCustomLightColor().isValue() ? light.getLightColor().getColor() : ColorUtil.getClientColor();
         this.cachedSkyLightVec.set(ColorUtil.redf(color), ColorUtil.greenf(color), ColorUtil.bluef(color));
         return this.cachedSkyLightVec;
      } else {
         return original;
      }
   }

   @Inject(
      method = {"update"},
      at = {@At("HEAD")}
   )
   private void onLightmapUpdate(float delta, CallbackInfo ci) {
      EventManager.callEvent(LightmapUpdateEvent.INSTANCE.set((LightmapTextureManager)(Object)this));
   }
}
