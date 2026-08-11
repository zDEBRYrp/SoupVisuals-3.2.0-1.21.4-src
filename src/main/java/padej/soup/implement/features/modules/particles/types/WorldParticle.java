package padej.soup.implement.features.modules.particles.types;

import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import padej.soup.api.feature.module.IParticleModule;
import padej.soup.base.QuickImports;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.particle.AbstractParticle;
import padej.soup.implement.features.modules.particles.render.ParticleRenderer;

public class WorldParticle extends AbstractParticle implements QuickImports {
   private final String particleMode;
   private final String glyphTexture;
   private final float health;
   private final IParticleModule module;

   public WorldParticle(
      float x,
      float y,
      float z,
      Color color,
      float rotationAngle,
      float rotationSpeed,
      float health,
      String mode,
      String physics,
      String glyphTexture,
      float lifeTime,
      float scale,
      float speed,
      IParticleModule module
   ) {
      super(x, y, z, color, rotationAngle, rotationSpeed, lifeTime, scale, physics, !mode.equals("Network"));
      this.health = health;
      this.particleMode = mode;
      this.glyphTexture = glyphTexture;
      this.module = module;
      this.initMotion(speed);
   }

   @Override
   public void render(MatrixStack matrixStack, long currentTime) {
      float size = this.scale;
      float renderScale = this.particleMode.equals("Text") ? 0.025F * size : 0.07F;
      float alpha = this.cachedAlpha;
      double posX = MathUtil.interpolate(this.px, this.x);
      double posY = MathUtil.interpolate(this.py, this.y);
      double posZ = MathUtil.interpolate(this.pz, this.z);
      Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
      float interpolatedAngle = MathUtil.interpolate(this.prevRotationAngle, this.rotationAngle);
      MatrixStack localStack = new MatrixStack();
      localStack.peek().getPositionMatrix().set(matrixStack.peek().getPositionMatrix());
      localStack.peek().getPositionMatrix().setTranslation(0.0F, 0.0F, 0.0F);
      localStack.peek().getNormalMatrix().set(matrixStack.peek().getNormalMatrix());
      localStack.push();
      localStack.translate(posX - cameraPos.x, posY - cameraPos.y, posZ - cameraPos.z);
      localStack.scale(renderScale, renderScale, renderScale);
      localStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
      localStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
      boolean isZAxisLocked = this.particleMode.equals("Text") || this.particleMode.equals("Flame");
      if (isZAxisLocked) {
         localStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
      } else if (!this.particleMode.equals("Cube")) {
         localStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(interpolatedAngle));
      }

      if (!this.particleMode.equals("Cube") && !this.particleMode.equals("Pyramid")) {
         localStack.translate(-size / 2.0F, -size / 2.0F, 0.0F);
         ParticleRenderer.render(localStack, this.particleMode, size, alpha, this.color, this.glyphTexture, this.health, this.module, this.colorOffset);
      } else if (this.particleMode.equals("Cube")) {
         ParticleRenderer.drawCubeBloom(localStack, size, alpha, this.module, this.color, this.colorOffset);
         localStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(interpolatedAngle));
         localStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(interpolatedAngle * 0.7F));
         ParticleRenderer.render(localStack, this.particleMode, size, alpha, this.color, this.glyphTexture, this.health, this.module, this.colorOffset);
      } else {
         ParticleRenderer.drawPyramidBloom(localStack, size, alpha, this.module, this.color, this.colorOffset);
         localStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(interpolatedAngle));
         ParticleRenderer.render(localStack, this.particleMode, size, alpha, this.color, this.glyphTexture, this.health, this.module, this.colorOffset);
      }

      localStack.pop();
   }

   public String getParticleMode() {
      return this.particleMode;
   }

   public float getHealth() {
      return this.health;
   }
}
