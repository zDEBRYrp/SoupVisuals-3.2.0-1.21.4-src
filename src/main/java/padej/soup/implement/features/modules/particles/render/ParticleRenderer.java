package padej.soup.implement.features.modules.particles.render;

import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import padej.soup.api.feature.module.IParticleModule;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.particle.ParticleColorUtil;

public class ParticleRenderer {
   public static void render(
      MatrixStack matrices, String mode, float size, float alpha, Color baseColor, String glyphTexture, float health, IParticleModule module, int colorOffset
   ) {
      switch (mode) {
         case "Stars":
            queueTexturedParticle(matrices, Identifier.of("textures/particles/star.png"), size, alpha, module, baseColor, colorOffset);
            break;
         case "Hearts":
            queueTexturedParticle(matrices, Identifier.of("textures/particles/heart.png"), size, alpha, module, baseColor, colorOffset);
            break;
         case "Bloom":
            queueTexturedParticle(matrices, Identifier.of("textures/particles/firefly.png"), size, alpha, module, baseColor, colorOffset);
            break;
         case "Glyph":
            queueTexturedParticle(matrices, Identifier.of("textures/particles/glyph/" + glyphTexture), size, alpha, module, baseColor, colorOffset);
            break;
         case "Things":
            queueTexturedParticle(matrices, Identifier.of("textures/particles/glyph_alt/" + glyphTexture), size, alpha, module, baseColor, colorOffset);
            break;
         case "Blink":
            queueTexturedParticle(matrices, Identifier.of("textures/particles/blink.png"), size, alpha, module, baseColor, colorOffset);
            break;
         case "Coron":
            queueTexturedParticle(matrices, Identifier.of("textures/particles/coron.png"), size, alpha, module, baseColor, colorOffset);
            break;
         case "Dollar":
            queueTexturedParticle(matrices, Identifier.of("textures/particles/dollar.png"), size, alpha, module, baseColor, colorOffset);
            break;
         case "Flame":
            queueTexturedParticle(matrices, Identifier.of("textures/particles/flame.png"), size, alpha, module, baseColor, colorOffset);
            break;
         case "Geometric":
            queueTexturedParticle(matrices, Identifier.of("textures/particles/geometric.png"), size, alpha, module, baseColor, colorOffset);
            break;
         case "Snowflake":
            queueTexturedParticle(matrices, Identifier.of("textures/particles/snowflake.png"), size, alpha, module, baseColor, colorOffset);
            break;
         case "Logo":
            queueTexturedParticle(matrices, Identifier.of("textures/particles/soupapi_3.png"), size, alpha, module, baseColor, colorOffset);
            break;
         case "Virus":
            queueTexturedParticle(matrices, Identifier.of("textures/particles/virus.png"), size, alpha, module, baseColor, colorOffset);
            break;
         case "SoupAPI Old":
            queueTexturedParticle(matrices, Identifier.of("textures/vanilla/soupapi_old.png"), size, alpha, module, baseColor, colorOffset);
            break;
         case "Sword":
            queueTexturedParticle(matrices, Identifier.of("textures/vanilla/sword.png"), size, alpha, module, baseColor, colorOffset);
            break;
         case "Network":
            matrices.push();
            matrices.translate(size / 2.0F, size / 2.0F, 0.0F);
            matrices.pop();
            break;
         case "Text":
            String text = String.format("%.1f", health);
            Color textColor = health > 0.0F ? new Color(module.getHealColor()) : new Color(module.getDamageColor());
            int colorWithAlpha = ColorUtil.replAlpha(textColor.getRGB(), (int)(textColor.getAlpha() * alpha));
            ParticleBatchRenderer.queueTextParticle(matrices, text, colorWithAlpha);
            break;
         case "Cube": {
            int color = getColor(alpha, module, baseColor, colorOffset);
            ParticleBatchRenderer.queueCubeParticle(matrices, size, alpha, color);
            break;
         }
         case "Pyramid": {
            int color = getColor(alpha, module, baseColor, colorOffset);
            ParticleBatchRenderer.queuePyramidParticle(matrices, size, alpha, color);
         }
      }
   }

   private static void queueTexturedParticle(
      MatrixStack matrices, Identifier texture, float size, float alpha, IParticleModule module, Color baseColor, int colorOffset
   ) {
      if (module.getColorMode().isSelected("Sync")) {
         int color = ColorUtil.multAlpha(ColorUtil.getClientColor(), alpha);
         ParticleBatchRenderer.queueTexturedParticle(matrices, texture, size, alpha, color, false, null, null);
      } else if (module.getColorMode().isSelected("Vanilla")) {
         int color = ColorUtil.multAlpha(baseColor.getRGB(), alpha);
         ParticleBatchRenderer.queueTexturedParticle(matrices, texture, size, alpha, color, false, null, null);
      } else {
         int[] colors = module.getCustomColors();
         if (colors != null && colors.length > 0) {
            if (module.getColorAnimation().isSelected("Vertex")) {
               ParticleBatchRenderer.queueTexturedParticle(matrices, texture, size, alpha, 0, true, colors, module);
            } else {
               int color = ParticleColorUtil.getWaveColor(colors, alpha, colorOffset);
               ParticleBatchRenderer.queueTexturedParticle(matrices, texture, size, alpha, color, false, null, null);
            }
         } else {
            int color = ColorUtil.multAlpha(-1, alpha);
            ParticleBatchRenderer.queueTexturedParticle(matrices, texture, size, alpha, color, false, null, null);
         }
      }
   }

   private static int getColor(float alpha, IParticleModule module, Color baseColor, int colorOffset) {
      if (module.getColorMode().isSelected("Sync")) {
         return ColorUtil.multAlpha(ColorUtil.getClientColor(), alpha);
      } else if (module.getColorMode().isSelected("Vanilla")) {
         return ColorUtil.multAlpha(baseColor.getRGB(), alpha);
      } else {
         int[] colors = module.getCustomColors();
         if (colors != null && colors.length > 0) {
            return module.getColorAnimation().isSelected("Wave")
               ? ParticleColorUtil.getWaveColor(colors, alpha, colorOffset)
               : ParticleColorUtil.getVertexGradientColor(colorOffset, colors, alpha);
         } else {
            return ColorUtil.multAlpha(-1, alpha);
         }
      }
   }

   public static void drawCubeBloom(MatrixStack matrices, float size, float alpha, IParticleModule module, Color baseColor, int colorOffset) {
      matrices.push();
      float bloomSize = size * 5.0F;
      matrices.translate(-bloomSize / 2.0F, -bloomSize / 2.0F, 0.0F);
      queueBloomEffect(matrices, bloomSize, alpha, module, baseColor, colorOffset);
      matrices.pop();
   }

   public static void drawPyramidBloom(MatrixStack matrices, float size, float alpha, IParticleModule module, Color baseColor, int colorOffset) {
      matrices.push();
      float bloomSize = size * 5.0F;
      matrices.translate(-bloomSize / 2.0F, -bloomSize / 2.0F, 0.0F);
      queueBloomEffect(matrices, bloomSize, alpha, module, baseColor, colorOffset);
      matrices.pop();
   }

   private static void queueBloomEffect(MatrixStack matrices, float size, float alpha, IParticleModule module, Color baseColor, int colorOffset) {
      if (module.getColorMode().isSelected("Custom")) {
         int[] colors = module.getCustomColors();
         if (colors != null && colors.length > 0 && module.getColorAnimation().isSelected("Vertex")) {
            ParticleBatchRenderer.queueBloomParticle(matrices, size, alpha, true, colors, module, colorOffset);
            return;
         }
      }

      if (module.getColorMode().isSelected("Vanilla")) {
         int[] vanillaColor = new int[]{baseColor.getRGB()};
         ParticleBatchRenderer.queueBloomParticle(matrices, size, alpha, true, vanillaColor, module, colorOffset);
      } else {
         ParticleBatchRenderer.queueBloomParticle(matrices, size, alpha, false, null, module, colorOffset);
      }
   }
}
