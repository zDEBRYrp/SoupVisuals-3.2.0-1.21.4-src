package padej.soup.mixins;

import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.render.BackgroundRenderer.FogType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.render.FogEvent;
import padej.soup.base.util.color.ColorUtil;

@ProtIgnore
@Mixin({BackgroundRenderer.class})
public class BackGroundRendererMixin {
   @Unique
   private static boolean shouldntApplyCustomFog(Camera camera, FogType fogType) {
      if (fogType != FogType.FOG_TERRAIN) {
         return true;
      } else {
         CameraSubmersionType submersionType = camera.getSubmersionType();
         if (submersionType != CameraSubmersionType.NONE) {
            return true;
         } else if (camera.getFocusedEntity() instanceof LivingEntity livingEntity) {
            return livingEntity.hasStatusEffect(StatusEffects.BLINDNESS) ? true : livingEntity.hasStatusEffect(StatusEffects.DARKNESS);
         } else {
            return false;
         }
      }
   }

   @Inject(
      method = {"getFogColor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void getFogColorHook(
      Camera camera, float tickDelta, ClientWorld world, int clampedViewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir
   ) {
      if (!shouldntApplyCustomFog(camera, FogType.FOG_TERRAIN)) {
         FogEvent event = FogEvent.INSTANCE.reset();
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            int color = event.getColor();
            cir.setReturnValue(new Vector4f(ColorUtil.redf(color), ColorUtil.greenf(color), ColorUtil.bluef(color), ColorUtil.alphaf(color)));
         }
      }
   }

   @Inject(
      method = {"applyFog"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void modifyFog(
      Camera camera, FogType fogType, Vector4f vector4f, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir
   ) {
      if (!shouldntApplyCustomFog(camera, fogType)) {
         FogEvent event = FogEvent.INSTANCE.reset();
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            int color = event.getColor();
            cir.setReturnValue(
               new Fog(
                  2.0F, event.getDistance(), FogShape.CYLINDER, ColorUtil.redf(color), ColorUtil.greenf(color), ColorUtil.bluef(color), ColorUtil.alphaf(color)
               )
            );
         }
      }
   }
}
