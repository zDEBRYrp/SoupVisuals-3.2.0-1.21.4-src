package padej.soup.implement.features.draggables.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import padej.soup.base.QuickImports;
import padej.soup.base.util.math.MathUtil;
import padej.soup.core.Main;
import padej.soup.implement.features.modules.hud.TargetHud;
import padej.soup.implement.features.modules.particles.ParticleData;
import padej.soup.implement.features.modules.particles.render.ParticleBatchRenderer;

public class TargetHudParticle implements QuickImports {
   private static final MinecraftClient mc = Main.mc;
   public float x;
   public float y;
   public float px;
   public float py;
   private final float spawnX;
   private final float spawnY;
   public float motionX;
   public float motionY;
   private final float baseSpeed;
   private final float maxRadius;
   public float size;
   private float cachedAlpha;
   private float rotation;
   private float prevRotation;
   private float rotationSpeed;
   private float age;
   private final float maxAge;
   private final TargetHudParticle.ParticleType type;
   private final Color color;
   private final Identifier cachedTexture;
   private final String physicsMode;

   public TargetHudParticle(
      float x,
      float y,
      float motionX,
      float motionY,
      float size,
      Color color,
      TargetHudParticle.ParticleType type,
      float lifetime,
      float baseSpeed,
      float maxRadius,
      String physicsMode
   ) {
      this.x = x;
      this.y = y;
      this.px = x;
      this.py = y;
      this.spawnX = x;
      this.spawnY = y;
      this.motionX = motionX;
      this.motionY = motionY;
      this.baseSpeed = baseSpeed;
      this.maxRadius = maxRadius;
      this.size = size;
      this.color = color;
      this.type = type;
      this.physicsMode = physicsMode;
      this.cachedAlpha = 1.0F;
      this.cachedTexture = this.getCachedTextureForType();
      int baseAge = (int)(lifetime * 20.0F);
      int variance = (int)(baseAge * 0.2);
      float initialAge = baseAge + QuickImports.random().nextInt(variance + 1);
      this.age = initialAge;
      this.maxAge = initialAge;
      this.rotation = (float)(QuickImports.random().nextDouble() * 360.0);
      this.prevRotation = this.rotation;
      this.rotationSpeed = (float)(QuickImports.random().nextDouble() * 6.0 - 2.0);
   }

   private Identifier getCachedTextureForType() {
      return switch (this.type) {
         case STARS -> Identifier.of("textures/particles/star.png");
         case HEARTS -> Identifier.of("textures/particles/heart.png");
         case BLOOM -> Identifier.of("textures/particles/firefly.png");
         case GLYPH -> {
            String glyphTexture = ParticleData.getRandomGlyphTexture();
            yield Identifier.of("textures/particles/glyph/" + glyphTexture);
         }
         case THINGS -> {
            String glyphAltTexture = ParticleData.getRandomGlyphAltTexture();
            yield Identifier.of("textures/particles/glyph_alt/" + glyphAltTexture);
         }
         case BLINK -> Identifier.of("textures/particles/blink.png");
         case CORON -> Identifier.of("textures/particles/coron.png");
         case DOLLAR -> Identifier.of("textures/particles/dollar.png");
         case FLAME -> Identifier.of("textures/particles/flame.png");
         case GEOMETRIC -> Identifier.of("textures/particles/geometric.png");
         case SNOWFLAKE -> Identifier.of("textures/particles/snowflake.png");
         case LOGO -> Identifier.of("textures/particles/soupapi_3.png");
         case VIRUS -> Identifier.of("textures/particles/virus.png");
         case SOUPAPI_OLD -> Identifier.of("textures/vanilla/soupapi_old.png");
         case SWORD -> Identifier.of("textures/vanilla/sword.png");
         default -> null;
      };
   }

   public boolean update() {
      return this.update(1.0F);
   }

   public boolean update(float deltaTicks) {
      if (deltaTicks <= 0.0F) {
         return false;
      } else {
         this.age -= deltaTicks;
         if (this.age < 0.0F) {
            this.cachedAlpha = 0.0F;
            return true;
         } else {
            this.px = this.x;
            this.py = this.y;
            float dx = this.x - this.spawnX;
            float dy = this.y - this.spawnY;
            float currentDistance = (float)Math.sqrt(dx * dx + dy * dy);
            float distanceRatio = Math.min(1.0F, currentDistance / this.maxRadius);
            float speedMultiplier = this.baseSpeed * (1.0F - distanceRatio * distanceRatio);
            float adjustedMotionX = this.motionX * speedMultiplier * deltaTicks;
            float adjustedMotionY = this.motionY * speedMultiplier * deltaTicks;
            this.x += adjustedMotionX;
            this.y += adjustedMotionY;
            float damping = (float)Math.pow(0.9F, deltaTicks);
            this.motionX *= damping;
            this.motionY *= damping;
            if ("Fly".equals(this.physicsMode)) {
               this.motionY *= damping;
            }

            if (currentDistance < this.maxRadius * 0.8F) {
               this.motionY += 0.001F * deltaTicks;
            }

            this.prevRotation = this.rotation;
            this.rotation = this.rotation + this.rotationSpeed * deltaTicks;
            this.cachedAlpha = Math.max(0.0F, Math.min(1.0F, this.age / this.maxAge));
            return false;
         }
      }
   }

   public void render(MatrixStack matrices, float hudX, float hudY, float depthFactor) {
      TargetHud module = TargetHud.getInstance();
      if (module.particles.isValue()) {
         matrices.push();
         float interpolatedX = MathUtil.interpolate(this.px, this.x);
         float interpolatedY = MathUtil.interpolate(this.py, this.y);
         float renderX = interpolatedX + hudX;
         float renderY = interpolatedY + hudY;
         matrices.translate(renderX, renderY, 0.0F);
         float finalScale = this.size * depthFactor;
         matrices.scale(finalScale, finalScale, finalScale);
         if (this.is2DParticle()) {
            float interpolatedRotation = MathUtil.interpolate(this.prevRotation, this.rotation);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(interpolatedRotation));
            this.render2D(matrices);
         } else {
            this.renderBloomEffect(matrices);
            float interpolatedRotation = MathUtil.interpolate(this.prevRotation, this.rotation);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(interpolatedRotation));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(interpolatedRotation * 0.7F));
            this.render3D(matrices);
         }

         matrices.pop();
      }
   }

   private boolean is2DParticle() {
      return this.type != TargetHudParticle.ParticleType.CUBE
         && this.type != TargetHudParticle.ParticleType.PYRAMID
         && this.type != TargetHudParticle.ParticleType.NETWORK2D;
   }

   public boolean isNetworkParticle() {
      return this.type == TargetHudParticle.ParticleType.NETWORK2D;
   }

   private void render2D(MatrixStack matrices) {
      if (this.type != TargetHudParticle.ParticleType.NETWORK2D) {
         this.renderTexturedParticle(matrices);
      }
   }

   private void renderTexturedParticle(MatrixStack matrices) {
      Identifier texture = this.getTextureForType();
      if (texture != null) {
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(770, 1);
         RenderSystem.setShaderTexture(0, texture);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
         RenderSystem.setShaderColor(this.color.getRed() / 255.0F, this.color.getGreen() / 255.0F, this.color.getBlue() / 255.0F, this.cachedAlpha);
         float halfSize = 0.5F;
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder bufferBuilder = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         bufferBuilder.vertex(matrix, -halfSize, halfSize, 0.0F).texture(0.0F, 1.0F);
         bufferBuilder.vertex(matrix, halfSize, halfSize, 0.0F).texture(1.0F, 1.0F);
         bufferBuilder.vertex(matrix, halfSize, -halfSize, 0.0F).texture(1.0F, 0.0F);
         bufferBuilder.vertex(matrix, -halfSize, -halfSize, 0.0F).texture(0.0F, 0.0F);
         BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   private void render3D(MatrixStack matrices) {
      int colorWithAlpha = this.color.getRGB() & 16777215 | (int)(this.cachedAlpha * 255.0F) << 24;
      if (this.type == TargetHudParticle.ParticleType.CUBE) {
         ParticleBatchRenderer.queueCubeParticle(matrices, 1.0F, this.cachedAlpha, colorWithAlpha);
      } else if (this.type == TargetHudParticle.ParticleType.PYRAMID) {
         ParticleBatchRenderer.queuePyramidParticle(matrices, 1.0F, this.cachedAlpha, colorWithAlpha);
      }
   }

   private void renderBloomEffect(MatrixStack matrices) {
      Identifier bloomTexture = Identifier.of("textures/particles/bloom/bloom_small.png");
      matrices.push();
      float bloomSize = 5.0F;
      matrices.translate(-bloomSize / 2.0F, -bloomSize / 2.0F, 0.0F);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 1);
      RenderSystem.setShaderTexture(0, bloomTexture);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      int r = this.color.getRed();
      int g = this.color.getGreen();
      int b = this.color.getBlue();
      int a = (int)(this.cachedAlpha * 255.0F);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferBuilder = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      bufferBuilder.vertex(matrix, 0.0F, bloomSize, 0.0F).texture(0.0F, 1.0F).color(r, g, b, a);
      bufferBuilder.vertex(matrix, bloomSize, bloomSize, 0.0F).texture(1.0F, 1.0F).color(r, g, b, a);
      bufferBuilder.vertex(matrix, bloomSize, 0.0F, 0.0F).texture(1.0F, 0.0F).color(r, g, b, a);
      bufferBuilder.vertex(matrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(r, g, b, a);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      matrices.pop();
   }

   public float getAlpha() {
      return this.cachedAlpha;
   }

   public static TargetHudParticle.ParticleType parseType(String modeName) {
      return switch (modeName) {
         case "Stars" -> TargetHudParticle.ParticleType.STARS;
         case "Hearts" -> TargetHudParticle.ParticleType.HEARTS;
         case "Bloom" -> TargetHudParticle.ParticleType.BLOOM;
         case "Glyph" -> TargetHudParticle.ParticleType.GLYPH;
         case "Things" -> TargetHudParticle.ParticleType.THINGS;
         case "Blink" -> TargetHudParticle.ParticleType.BLINK;
         case "Coron" -> TargetHudParticle.ParticleType.CORON;
         case "Dollar" -> TargetHudParticle.ParticleType.DOLLAR;
         case "Flame" -> TargetHudParticle.ParticleType.FLAME;
         case "Geometric" -> TargetHudParticle.ParticleType.GEOMETRIC;
         case "Snowflake" -> TargetHudParticle.ParticleType.SNOWFLAKE;
         case "Logo" -> TargetHudParticle.ParticleType.LOGO;
         case "Virus" -> TargetHudParticle.ParticleType.VIRUS;
         case "SoupAPI Old" -> TargetHudParticle.ParticleType.SOUPAPI_OLD;
         case "Sword" -> TargetHudParticle.ParticleType.SWORD;
         case "Cube" -> TargetHudParticle.ParticleType.CUBE;
         case "Pyramid" -> TargetHudParticle.ParticleType.PYRAMID;
         case "Network2D" -> TargetHudParticle.ParticleType.NETWORK2D;
         default -> TargetHudParticle.ParticleType.STARS;
      };
   }

   public TargetHudParticle.ParticleType getType() {
      return this.type;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public float getPrevX() {
      return this.px;
   }

   public float getPrevY() {
      return this.py;
   }

   public float getRotation() {
      return this.rotation;
   }

   public float getPrevRotation() {
      return this.prevRotation;
   }

   public Color getColor() {
      return this.color;
   }

   public Identifier getTextureForType() {
      return this.cachedTexture;
   }

   public static enum ParticleType {
      STARS,
      HEARTS,
      BLOOM,
      GLYPH,
      THINGS,
      BLINK,
      CORON,
      DOLLAR,
      FLAME,
      GEOMETRIC,
      SNOWFLAKE,
      LOGO,
      VIRUS,
      SOUPAPI_OLD,
      SWORD,
      CUBE,
      PYRAMID,
      NETWORK2D;
   }
}
