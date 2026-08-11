package padej.soup.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.base.util.render.ChinaHatFeatureRenderer;

@ProtIgnore
@Mixin({PlayerEntityRenderer.class})
@Environment(EnvType.CLIENT)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<PlayerEntity, PlayerEntityRenderState, PlayerEntityModel> {
   public PlayerEntityRendererMixin(Context context, PlayerEntityModel model, float shadowRadius) {
      super(context, model, shadowRadius);
   }

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void addChinaHatFeature(Context ctx, boolean slim, CallbackInfo ci) {
      this.addFeature(new ChinaHatFeatureRenderer(this));
   }
}
