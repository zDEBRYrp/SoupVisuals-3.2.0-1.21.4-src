package padej.soup.mixins;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.block.entity.LightmapCoordinatesRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import padej.protect.ProtIgnore;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.features.modules.world.Light;

@ProtIgnore
@Environment(EnvType.CLIENT)
@Mixin({LightmapCoordinatesRetriever.class})
public class LightmapCoordinatesRetrieverMixin<S extends BlockEntity> {
   @Inject(
      method = {"getFromBoth(Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/block/entity/BlockEntity;)Lit/unimi/dsi/fastutil/ints/Int2IntFunction;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void modifyLightmapFromBoth(S blockEntity, S blockEntity2, CallbackInfoReturnable<Int2IntFunction> cir) {
      Light light = Light.getInstance();
      if (light != null && light.isState()) {
         Int2IntFunction original = (Int2IntFunction)cir.getReturnValue();
         cir.setReturnValue((Int2IntFunction)i -> {
            int originalValue = original.applyAsInt(i);
            return this.applyColorToLightmap(originalValue, light);
         });
      }
   }

   @Inject(
      method = {"getFrom(Lnet/minecraft/block/entity/BlockEntity;)Lit/unimi/dsi/fastutil/ints/Int2IntFunction;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void modifyLightmapFrom(S blockEntity, CallbackInfoReturnable<Int2IntFunction> cir) {
      Light light = Light.getInstance();
      if (light != null && light.isState()) {
         Int2IntFunction original = (Int2IntFunction)cir.getReturnValue();
         cir.setReturnValue((Int2IntFunction)i -> {
            int originalValue = original.applyAsInt(i);
            return this.applyColorToLightmap(originalValue, light);
         });
      }
   }

   @Inject(
      method = {"getFallback()Lit/unimi/dsi/fastutil/ints/Int2IntFunction;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void modifyLightmapFallback(CallbackInfoReturnable<Int2IntFunction> cir) {
      Light light = Light.getInstance();
      if (light != null && light.isState()) {
         Int2IntFunction original = (Int2IntFunction)cir.getReturnValue();
         cir.setReturnValue((Int2IntFunction)i -> {
            int originalValue = original.applyAsInt(i);
            return this.applyColorToLightmap(originalValue, light);
         });
      }
   }

   @Unique
   private int applyColorToLightmap(int lightmapCoords, Light light) {
      int blockLight = LightmapTextureManager.getBlockLightCoordinates(lightmapCoords);
      int skyLight = LightmapTextureManager.getSkyLightCoordinates(lightmapCoords);
      int color = light.getUseCustomLightColor().isValue() ? light.getLightColor().getColor() : ColorUtil.getClientColor();
      float red = ColorUtil.redf(color);
      float green = ColorUtil.greenf(color);
      float blue = ColorUtil.bluef(color);
      float brightness = (red + green + blue) / 3.0F;
      int modifiedBlockLight = Math.min(15, (int)(blockLight * (1.0F + brightness * 0.5F)));
      int modifiedSkyLight = Math.min(15, (int)(skyLight * (1.0F + brightness * 0.5F)));
      return LightmapTextureManager.pack(modifiedBlockLight, modifiedSkyLight);
   }
}
