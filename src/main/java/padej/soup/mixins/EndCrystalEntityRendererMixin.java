package padej.soup.mixins;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.implement.features.modules.other.EndCrystal;

@ProtIgnore
@Mixin({EndCrystalEntityRenderer.class})
public class EndCrystalEntityRendererMixin {
   @Shadow
   @Final
   private EndCrystalEntityModel field_53187;

   @Inject(
      method = {"render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void render(EndCrystalEntityRenderState state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
      if (EndCrystal.getInstance().isEnabled()) {
         ci.cancel();
         EndCrystal.getInstance().renderCrystal(state, matrixStack, light, this.field_53187, vertexConsumerProvider);
      }
   }
}
