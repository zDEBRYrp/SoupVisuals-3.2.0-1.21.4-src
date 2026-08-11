package padej.soup.mixins;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.implement.features.modules.hud.NoFire;

@ProtIgnore
@Mixin({InGameOverlayRenderer.class})
public class InGameOverlayRendererMixin {
   @Inject(
      method = {"renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void cancelFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
      NoFire noFire = NoFire.getInstance();
      if (noFire != null && noFire.isEnabled()) {
         if (noFire.hideFire.isValue()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V",
         ordinal = 0,
         shift = Shift.AFTER
      )}
   )
   private static void injectAfterTranslate(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
      NoFire noFire = NoFire.getInstance();
      if (noFire != null && noFire.isEnabled()) {
         if (!noFire.hideFire.isValue()) {
            matrices.translate(0.0F, noFire.yOffset.getValue(), 0.0F);
         }
      }
   }

   @Redirect(
      method = {"renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/VertexConsumer;color(FFFF)Lnet/minecraft/client/render/VertexConsumer;"
      )
   )
   private static VertexConsumer redirectColor(VertexConsumer instance, float r, float g, float b, float a) {
      NoFire noFire = NoFire.getInstance();
      if (noFire == null || !noFire.isEnabled()) {
         return instance.color(r, g, b, a);
      } else if (noFire.hideFire.isValue()) {
         return instance.color(r, g, b, a);
      } else {
         float alpha = noFire.fireAlpha.getValue();
         return instance.color(r, g, b, alpha);
      }
   }
}
