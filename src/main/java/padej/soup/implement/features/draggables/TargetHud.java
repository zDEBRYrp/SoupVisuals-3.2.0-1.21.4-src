package padej.soup.implement.features.draggables;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.animation.implement.EaseInOutAnimation;
import padej.soup.api.system.animation.implement.LinearAnimation;
import padej.soup.api.system.pipeline.HudRenderPipeline;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;
import padej.soup.base.util.entity.VisibleUtils;
import padej.soup.base.util.item.ItemUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.math.ProjectionUtil;
import padej.soup.base.util.other.StopWatch;
import padej.soup.base.util.render.ScissorManager;
import padej.soup.base.util.render.TargetHudRenderer;
import padej.soup.base.util.spatial.SpatialGrid2D;
import padej.soup.core.Main;
import padej.soup.core.perftest.HudProfiler;
import padej.soup.core.server.ServerLimitCfg;
import padej.soup.implement.features.draggables.particles.TargetHudParticle;
import padej.soup.implement.features.modules.particles.render.ParticleBatchRenderer;

public class TargetHud extends AbstractDraggable {

   private static <T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> Identifier resolveFaceTextureTyped(
      LivingEntityRenderer<T, S, M> renderer, LivingEntity target
   ) {
      @SuppressWarnings("unchecked")
      T typedTarget = (T)target;
      S state = renderer.getAndUpdateRenderState(typedTarget, mc.getRenderTickCounter().getTickDelta(false));
      return renderer.getTexture(state);
   }
   private final Animation animation = new DecelerateAnimation().setMs(200).setValue(1.0);
   private final Animation healthAnimation = new DecelerateAnimation().setMs(150).setValue(1.0);
   private final StopWatch stopWatch = new StopWatch();
   private LivingEntity targetEntity;
   private LivingEntity lastTarget;
   private long lastTargetTime;
   private Item lastItem = Items.AIR;
   private float health;
   private float targetHealth;
   private float animatedHealth;
   private String lastAnimationType = "Decelerate";
   private final List<TargetHudParticle> particles = new ArrayList<>();
   private final List<TargetHudParticle> networkParticles = new ArrayList<>();
   private SpatialGrid2D<TargetHudParticle> spatialGrid;
   private boolean sentParticles = false;
   private int lastHurtTime = 0;
   private final Set<Long> reusableProcessedPairs = new HashSet<>();
   private final Map<TargetHudParticle, Integer> reusableLinkCounts = new HashMap<>();
   private final Map<Identifier, List<TargetHud.ParticleRenderData>> reusableTexturedBatches = new HashMap<>();
   private final List<TargetHud.ParticleRenderData> reusableBloomParticles = new ArrayList<>();
   private String cachedDisplayName = "";
   private String cachedHpString = "??";
   private boolean cachedShouldShowSkin = false;
   private boolean cachedIsPartiallyVisible = false;
   private boolean cachedShowHp = false;
   private boolean cachedShowItems = false;
   private boolean cachedShowItemsOverlay = false;
   private boolean cachedShowItemUsingProgress = false;
   private final List<ItemStack> cachedEquippedItems = new ArrayList<>(6);
   private Identifier cachedFaceTexture = null;
   private String cachedStyle = "Default";
   private String cachedAnchor = "BODY";
   private String cachedDisplayMode = "Static";
   private String cachedAnimationMode = "Both";
   private float cachedScale = 1.0F;
   private float cachedXOffset = 0.0F;
   private float cachedLinkDistance = 30.0F;
   private int cachedMaxLinks = 3;
   private float cachedParticleSize = 1.0F;

   public TargetHud() {
      super("TargetHud", 10, 40, 100, 36, true);
      this.health = 0.0F;
      this.targetHealth = 0.0F;
      this.animatedHealth = 0.0F;
   }

   @Override
   public boolean visible() {
      return this.scaleAnimation.isDirection(Direction.FORWARDS);
   }

   private void updateScaleAnimation() {
      padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
      String currentType = hudModule.animationType.getSelected();
      int currentSpeed = (int)hudModule.animationSpeed.getValue();
      if (!currentType.equals(this.lastAnimationType) || this.scaleAnimation.getMs() != currentSpeed) {
         this.lastAnimationType = currentType;
         Direction currentDirection = this.scaleAnimation.isDirection(Direction.FORWARDS) ? Direction.FORWARDS : Direction.BACKWARDS;

         Animation newAnimation = (Animation)(switch (currentType) {
            case "Linear" -> new LinearAnimation();
            case "EaseInOut" -> new EaseInOutAnimation();
            default -> new DecelerateAnimation();
         });
         newAnimation.setMs(currentSpeed);
         newAnimation.setValue(1.0);
         newAnimation.setDirection(currentDirection);
         this.scaleAnimation = newAnimation;
      }
   }

   public void renderParticlesAlways(DrawContext context) {
      if (!this.particles.isEmpty() || !this.networkParticles.isEmpty()) {
         MatrixStack matrices = context.getMatrices();
         long tp = HudProfiler.nano();
         this.renderParticlesIndependent(context);
         HudProfiler.recordComponent("TH:Particles", tp);
         long tn = HudProfiler.nano();
         this.renderNetworkLinksIndependent(matrices);
         HudProfiler.recordComponent("TH:NetworkLinks", tn);
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (this.lastTarget != null && VisibleUtils.canBeTargeted(this.lastTarget)) {
         MatrixStack matrix = context.getMatrices();
         this.updateDimensionsForStyle();
         float scale = this.cachedScale;
         matrix.push();
         if (this.cachedDisplayMode.equals("3D")) {
            Vector4d projection = ProjectionUtil.getVector4DForAnchor(this.lastTarget, this.cachedAnchor);
            if (projection == null || ProjectionUtil.cantSee(projection)) {
               matrix.pop();
               return;
            }

            double centerX = ProjectionUtil.centerX(projection);
            double centerY = projection.y;
            String anchorKey = this.cachedAnchor;
            switch (anchorKey) {
               case "HEAD":
                  centerY -= this.getHeight() / 2.0F + 5.0F;
               case "BODY":
               default:
                  break;
               case "FEET":
                  centerY += this.getHeight() / 2.0F + 5.0F;
            }

            double hudX = centerX + this.cachedXOffset;
            matrix.translate(hudX, centerY, 0.0);
         } else {
            matrix.translate(this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, 0.0F);
         }

         float animationValue = this.scaleAnimation.getOutputFloat();
         float scaleMultiplier = 1.0F;
         if (this.cachedAnimationMode.equals("Scale") || this.cachedAnimationMode.equals("Both")) {
            scaleMultiplier = animationValue;
         }

         matrix.scale(scale * scaleMultiplier, scale * scaleMultiplier, 1.0F);
         matrix.translate(-this.getWidth() / 2.0F, -this.getHeight() / 2.0F, 0.0F);
         this.drawUsingItem(context, matrix);
         float alpha = 1.0F;
         if (this.cachedAnimationMode.equals("Fade") || this.cachedAnimationMode.equals("Both")) {
            alpha = animationValue;
         }

         float currentHealth = this.health;
         if (this.targetHealth > 0.0F) {
            float healthProgress = this.healthAnimation.getOutputFloat();
            this.animatedHealth = MathHelper.lerp(healthProgress, currentHealth, this.targetHealth);
            this.animatedHealth = MathHelper.clamp(this.animatedHealth, 2.0F, 61.0F);
            if (this.healthAnimation.isDone()) {
               this.health = this.targetHealth;
            }
         } else {
            this.animatedHealth = currentHealth;
         }

         float displayHealth = this.cachedShowHp ? this.animatedHealth : 61.0F;
         MathUtil.setAlpha(
            alpha,
            () -> {
               String var3x = this.cachedStyle;
               switch (var3x) {
                  case "Default":
                     TargetHudRenderer.renderStyleZenith(
                        context,
                        this.lastTarget,
                        0.0F,
                        0.0F,
                        this.getWidth(),
                        this.getHeight(),
                        displayHealth,
                        this.cachedDisplayName,
                        this.cachedHpString,
                        this.cachedShouldShowSkin,
                        this.cachedIsPartiallyVisible,
                        this.cachedShowItems,
                        this.cachedShowItemsOverlay,
                        this.cachedEquippedItems,
                        this.cachedFaceTexture
                     );
                     break;
                  case "Round":
                     TargetHudRenderer.renderStyleAres(
                        context,
                        this.lastTarget,
                        0.0F,
                        0.0F,
                        this.getWidth(),
                        this.getHeight(),
                        displayHealth,
                        this.cachedDisplayName,
                        this.cachedHpString,
                        this.cachedShouldShowSkin,
                        this.cachedIsPartiallyVisible,
                        this.cachedFaceTexture
                     );
               }
            }
         );
         matrix.pop();
      }
   }

   private void renderNetworkLinksIndependent(MatrixStack matrices) {
      if (this.networkParticles.size() >= 2 && this.spatialGrid != null) {
         matrices.push();
         if (this.cachedDisplayMode.equals("3D") && this.lastTarget != null) {
            Vector4d projection = ProjectionUtil.getVector4DForAnchor(this.lastTarget, this.cachedAnchor);
            if (projection != null && !ProjectionUtil.cantSee(projection)) {
               double centerX = ProjectionUtil.centerX(projection);
               double centerY = projection.y;
               String anchorKey = this.cachedAnchor;
               switch (anchorKey) {
                  case "HEAD":
                     centerY -= this.getHeight() / 2.0F + 5.0F;
                  case "BODY":
                  default:
                     break;
                  case "FEET":
                     centerY += this.getHeight() / 2.0F + 5.0F;
               }

               double hudX = centerX + this.cachedXOffset;
               matrices.translate(hudX, centerY, 0.0);
               matrices.scale(this.cachedScale, this.cachedScale, 1.0F);
               matrices.translate(-this.getWidth() / 2.0F, -this.getHeight() / 2.0F, 0.0F);
               this.renderNetworkLinksInternal(matrices);
            }
         } else {
            matrices.translate(this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, 0.0F);
            matrices.scale(this.cachedScale, this.cachedScale, 1.0F);
            matrices.translate(-this.getWidth() / 2.0F, -this.getHeight() / 2.0F, 0.0F);
            this.renderNetworkLinksInternal(matrices);
         }

         matrices.pop();
      }
   }

   private void renderNetworkLinksInternal(MatrixStack matrices) {
      float maxLinkDistance = this.cachedLinkDistance;
      int maxLinksPerNode = this.cachedMaxLinks;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      this.reusableProcessedPairs.clear();
      this.reusableLinkCounts.clear();
      Set<Long> processedPairs = this.reusableProcessedPairs;
      Map<TargetHudParticle, Integer> linkCounts = this.reusableLinkCounts;
      int lineCount = 0;

      for (TargetHudParticle p1 : this.networkParticles) {
         int p1LinkCount = linkCounts.getOrDefault(p1, 0);
         if (p1LinkCount < maxLinksPerNode) {
            float posX1 = MathUtil.interpolate(p1.getPrevX(), p1.getX());
            float posY1 = MathUtil.interpolate(p1.getPrevY(), p1.getY());
            List<SpatialGrid2D.GridEntry2D<TargetHudParticle>> nearbyEntries = this.spatialGrid.queryRadiusWithPositions(posX1, posY1, maxLinkDistance);
            nearbyEntries.sort((ax, bx) -> {
               float dx1 = ax.getX() - posX1;
               float dy1 = ax.getY() - posY1;
               float dist1 = dx1 * dx1 + dy1 * dy1;
               float dx2 = bx.getX() - posX1;
               float dy2 = bx.getY() - posY1;
               float dist2 = dx2 * dx2 + dy2 * dy2;
               return Float.compare(dist1, dist2);
            });

            for (SpatialGrid2D.GridEntry2D<TargetHudParticle> entry : nearbyEntries) {
               TargetHudParticle p2 = entry.getObject();
               if (p1 != p2) {
                  int p2LinkCount = linkCounts.getOrDefault(p2, 0);
                  if (p2LinkCount < maxLinksPerNode) {
                     long pair = pairKey(p1, p2);
                     if (!processedPairs.contains(pair)) {
                        if (p1LinkCount >= maxLinksPerNode) {
                           break;
                        }

                        float posX2 = entry.getX();
                        float posY2 = entry.getY();
                        float dx = posX2 - posX1;
                        float dy = posY2 - posY1;
                        float dist = (float)Math.sqrt(dx * dx + dy * dy);
                        if (dist < maxLinkDistance && dist > 0.01F) {
                           float lineAlpha = 1.0F - dist / maxLinkDistance;
                           float particleAlpha1 = p1.getAlpha();
                           float particleAlpha2 = p2.getAlpha();
                           float alpha = Math.min(lineAlpha, Math.min(particleAlpha1, particleAlpha2));
                           int baseColor = ColorUtil.getClientColor();
                           int color = ColorUtil.multAlpha(baseColor, alpha);
                           int r = color >> 16 & 0xFF;
                           int g = color >> 8 & 0xFF;
                           int b = color & 0xFF;
                           int a = (int)(alpha * 255.0F);
                           bufferBuilder.vertex(matrix, posX1, posY1, 0.0F).color(r, g, b, a);
                           bufferBuilder.vertex(matrix, posX2, posY2, 0.0F).color(r, g, b, a);
                           lineCount++;
                           processedPairs.add(pair);
                           linkCounts.put(p1, p1LinkCount + 1);
                           linkCounts.put(p2, p2LinkCount + 1);
                           p1LinkCount++;
                        }
                     }
                  }
               }
            }
         }
      }

      if (lineCount > 0) {
         BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      }

      RenderSystem.disableBlend();
   }

   private static long pairKey(TargetHudParticle a, TargetHudParticle b) {
      int h1 = System.identityHashCode(a);
      int h2 = System.identityHashCode(b);
      if (h1 > h2) {
         int tmp = h1;
         h1 = h2;
         h2 = tmp;
      }

      return (long)h1 << 32 | h2 & 4294967295L;
   }

   private void renderParticlesIndependent(DrawContext context) {
      if (!this.particles.isEmpty()) {
         MatrixStack matrices = context.getMatrices();
         matrices.push();
         if (this.cachedDisplayMode.equals("3D") && this.lastTarget != null) {
            Vector4d projection = ProjectionUtil.getVector4DForAnchor(this.lastTarget, this.cachedAnchor);
            if (projection != null && !ProjectionUtil.cantSee(projection)) {
               double centerX = ProjectionUtil.centerX(projection);
               double centerY = projection.y;
               String anchorKey = this.cachedAnchor;
               switch (anchorKey) {
                  case "HEAD":
                     centerY -= this.getHeight() / 2.0F + 5.0F;
                  case "BODY":
                  default:
                     break;
                  case "FEET":
                     centerY += this.getHeight() / 2.0F + 5.0F;
               }

               double hudX = centerX + this.cachedXOffset;
               matrices.translate(hudX, centerY, 0.0);
               matrices.scale(this.cachedScale, this.cachedScale, 1.0F);
               matrices.translate(-this.getWidth() / 2.0F, -this.getHeight() / 2.0F, 0.0F);
               this.renderParticlesInternal(matrices);
            }
         } else {
            matrices.translate(this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, 0.0F);
            matrices.scale(this.cachedScale, this.cachedScale, 1.0F);
            matrices.translate(-this.getWidth() / 2.0F, -this.getHeight() / 2.0F, 0.0F);
            this.renderParticlesInternal(matrices);
         }

         matrices.pop();
      }
   }

   private void renderParticlesInternal(MatrixStack matrices) {
      float depthFactor = 1.0F;
      if (this.cachedDisplayMode.equals("3D") && this.lastTarget != null) {
         Vector4d projection = ProjectionUtil.getVector4DForAnchor(this.lastTarget, this.cachedAnchor);
         if (projection != null) {
            depthFactor = MathHelper.clamp(1.0F - (float)projection.z / 10.0F, 0.1F, 1.0F);
         }
      }

      for (List<TargetHud.ParticleRenderData> list : this.reusableTexturedBatches.values()) {
         list.clear();
      }

      this.reusableBloomParticles.clear();
      Map<Identifier, List<TargetHud.ParticleRenderData>> texturedBatches = this.reusableTexturedBatches;
      List<TargetHud.ParticleRenderData> bloomParticles = this.reusableBloomParticles;

      for (TargetHudParticle particle : this.particles) {
         if (!(particle.getAlpha() <= 0.01F) && !particle.isNetworkParticle()) {
            float interpolatedX = MathUtil.interpolate(particle.getPrevX(), particle.getX());
            float interpolatedY = MathUtil.interpolate(particle.getPrevY(), particle.getY());
            float finalScale = this.cachedParticleSize * depthFactor;
            TargetHud.ParticleRenderData data = new TargetHud.ParticleRenderData(particle, interpolatedX, interpolatedY, finalScale);
            if (particle.getType() != TargetHudParticle.ParticleType.CUBE && particle.getType() != TargetHudParticle.ParticleType.PYRAMID) {
               Identifier texture = particle.getTextureForType();
               if (texture != null) {
                  texturedBatches.computeIfAbsent(texture, k -> new ArrayList<>()).add(data);
               }
            } else {
               bloomParticles.add(data);
               particle.render(matrices, 0.0F, 0.0F, depthFactor);
            }
         }
      }

      this.renderTexturedBatches(matrices, texturedBatches);
      ParticleBatchRenderer.renderBatches();
   }

   private void renderTexturedBatches(MatrixStack matrices, Map<Identifier, List<TargetHud.ParticleRenderData>> batches) {
      if (!batches.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(770, 1);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         Matrix4f parentMatrix = matrices.peek().getPositionMatrix();

         for (Entry<Identifier, List<TargetHud.ParticleRenderData>> entry : batches.entrySet()) {
            List<TargetHud.ParticleRenderData> batchData = entry.getValue();
            if (!batchData.isEmpty()) {
               Identifier texture = entry.getKey();
               RenderSystem.setShaderTexture(0, texture);
               BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

               for (TargetHud.ParticleRenderData data : batchData) {
                  float rotDeg = MathUtil.interpolate(data.particle.getPrevRotation(), data.particle.getRotation());
                  float rotRad = (float)Math.toRadians(rotDeg);
                  float cos = MathHelper.cos(rotRad);
                  float sin = MathHelper.sin(rotRad);
                  float s = data.scale;
                  float halfSize = 0.5F;
                  float sc = s * halfSize * cos;
                  float ss = s * halfSize * sin;
                  float tx = data.x;
                  float ty = data.y;
                  float x0 = tx + (-sc - ss);
                  float y0 = ty + (-ss + sc);
                  float x1 = tx + (sc - ss);
                  float y1 = ty + (ss + sc);
                  float x2 = tx + (sc + ss);
                  float y2 = ty + (ss - sc);
                  float x3 = tx + (-sc + ss);
                  float y3 = ty + (-ss - sc);
                  int color = data.particle.getColor().getRGB();
                  int r = color >> 16 & 0xFF;
                  int g = color >> 8 & 0xFF;
                  int b = color & 0xFF;
                  int a = (int)(data.particle.getAlpha() * 255.0F);
                  bufferBuilder.vertex(parentMatrix, x0, y0, 0.0F).texture(0.0F, 1.0F).color(r, g, b, a);
                  bufferBuilder.vertex(parentMatrix, x1, y1, 0.0F).texture(1.0F, 1.0F).color(r, g, b, a);
                  bufferBuilder.vertex(parentMatrix, x2, y2, 0.0F).texture(1.0F, 0.0F).color(r, g, b, a);
                  bufferBuilder.vertex(parentMatrix, x3, y3, 0.0F).texture(0.0F, 0.0F).color(r, g, b, a);
               }

               BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            }
         }

         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   private void updateParticles() {
      this.particles.removeIf(particlex -> {
         boolean isDead = particlex.update();
         if (isDead && particlex.isNetworkParticle()) {
            this.networkParticles.remove(particlex);
            if (this.spatialGrid != null) {
               this.spatialGrid.remove(particlex, particlex.getX(), particlex.getY());
            }
         }

         return isDead;
      });
      if (this.spatialGrid != null && !this.networkParticles.isEmpty()) {
         for (TargetHudParticle particle : this.networkParticles) {
            this.spatialGrid.update(particle, particle.getX(), particle.getY(), particle.getX(), particle.getY());
         }
      }
   }

   private String getRandomMode() {
      padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
      List<String> selected = hudModule.particleMode.getSelected();
      return selected.isEmpty() ? "Stars" : selected.get(QuickImports.random().nextInt(selected.size()));
   }

   private void spawnParticlesOnHurt() {
      padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
      if (hudModule.particles.isValue() && this.lastTarget != null) {
         int count = (int)hudModule.particleCount.getValue();
         float size = hudModule.particleSize.getValue();
         float speed = hudModule.particleSpeed.getValue();
         float lifetime = hudModule.particleLifetime.getValue();
         int[] customColors = hudModule.getCustomColors();
         double spawnX;
         double spawnY;
         if (hudModule.particleSpawnLoc.isSelected("HP Bar")) {
            float currentHealth = this.animatedHealth;
            String hudStyleKey = hudModule.style.getSelected();

            spawnX = switch (hudStyleKey) {
               case "Default" -> 34.0F + currentHealth;
               case "Round" -> {
                  float widthHp = 68.0F;
                  float healthBarWidth = currentHealth * widthHp / 61.0F;
                  yield 48.0F + healthBarWidth;
               }
               default -> 34.0F + currentHealth;
            };
            hudStyleKey = hudModule.style.getSelected();

            spawnY = switch (hudStyleKey) {
               case "Default" -> 28.0;
               case "Round" -> 36.5;
               default -> 28.0;
            };
         } else {
            String var27 = hudModule.style.getSelected();

            spawnX = switch (var27) {
               case "Default" -> 15.0;
               case "Round" -> 23.0;
               default -> 15.0;
            };
            var27 = hudModule.style.getSelected();

            spawnY = switch (var27) {
               case "Default" -> 18.0;
               case "Round" -> 23.0;
               default -> 18.0;
            };
         }

         boolean isCustomColor = hudModule.particleColorMode.isSelected("Custom") && customColors != null && customColors.length > 0;
         boolean isVertexAnim = isCustomColor && hudModule.particleColorAnimation.isSelected("Vertex");
         int syncColor1 = 0;
         int syncColor2 = 0;
         if (!isCustomColor) {
            syncColor1 = ColorUtil.getClientColor();
            syncColor2 = ColorUtil.getClientColor(200.0F);
         }

         for (int i = 0; i < count; i++) {
            String randomMode = this.getRandomMode();
            TargetHudParticle.ParticleType type = TargetHudParticle.parseType(randomMode);
            double motionX = (QuickImports.random().nextDouble() * 2.0 - 1.0) * speed;
            double motionY = (QuickImports.random().nextDouble() * 2.0 - 1.0) * speed;
            Color particleColor;
            if (isCustomColor) {
               int colorIndex;
               if (isVertexAnim) {
                  colorIndex = i % customColors.length;
               } else {
                  long time = System.currentTimeMillis();
                  float phase = ((float)time / 1000.0F + i * 0.1F) % 1.0F;
                  colorIndex = (int)(phase * customColors.length);
               }

               particleColor = new Color(customColors[colorIndex], true);
            } else {
               double colorMix = (Math.sin(System.currentTimeMillis() * 0.001 + i) + 1.0) * 0.5;
               particleColor = new Color(mixColorsRgb(syncColor1, syncColor2, colorMix));
            }

            TargetHudParticle particle = new TargetHudParticle(
               (float)spawnX,
               (float)spawnY,
               (float)motionX,
               (float)motionY,
               size,
               particleColor,
               type,
               lifetime,
               hudModule.particleSpeed.getValue(),
               hudModule.particleMaxRadius.getValue(),
               "Fly"
            );
            this.particles.add(particle);
            if (type == TargetHudParticle.ParticleType.NETWORK2D) {
               if (this.spatialGrid == null) {
                  this.spatialGrid = new SpatialGrid2D<>(hudModule.linkDistance.getValue());
               }

               this.networkParticles.add(particle);
               this.spatialGrid.insert(particle, particle.getX(), particle.getY());
            }
         }
      }
   }

   private static int mixColorsRgb(int argb1, int argb2, double percent) {
      double inverse = 1.0 - percent;
      int r = (int)((argb1 >> 16 & 0xFF) * percent + (argb2 >> 16 & 0xFF) * inverse);
      int g = (int)((argb1 >> 8 & 0xFF) * percent + (argb2 >> 8 & 0xFF) * inverse);
      int b = (int)((argb1 & 0xFF) * percent + (argb2 & 0xFF) * inverse);
      return r << 16 | g << 8 | b;
   }

   @Override
   public void tick() {
      super.tick();
      padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
      this.updateScaleAnimation();
      this.updateParticles();
      LivingEntity previousTarget = this.targetEntity;
      if (mc.crosshairTarget instanceof EntityHitResult entityHit) {
         if (!(entityHit.getEntity() instanceof PlayerEntity player && !player.isDead() && player.isAlive())) {
            this.targetEntity = null;
         } else if (VisibleUtils.canBeTargeted(player)) {
            this.targetEntity = player;
         } else {
            this.targetEntity = null;
         }
      } else {
         this.targetEntity = null;
      }

      if (this.targetEntity != null && (this.targetEntity.isDead() || !this.targetEntity.isAlive())) {
         this.targetEntity = null;
         if (previousTarget != null) {
            this.lastTargetTime = System.currentTimeMillis();
         }
      }

      if (previousTarget != null && this.targetEntity == null) {
         this.lastTargetTime = System.currentTimeMillis();
      }

      LivingEntity displayTarget = this.getDisplayTarget();
      if (displayTarget != null) {
         this.lastTarget = displayTarget;
         if (hudModule.isEnabled() && ServerLimitCfg.showHp(displayTarget)) {
            int currentHurtTime = displayTarget.hurtTime;
            if (currentHurtTime == 9 && !this.sentParticles && currentHurtTime != this.lastHurtTime) {
               this.spawnParticlesOnHurt();
               this.sentParticles = true;
            }

            if (currentHurtTime == 8) {
               this.sentParticles = false;
            }

            this.lastHurtTime = currentHurtTime;
         }

         float hp = PlayerIntersectionUtil.getHealth(displayTarget);
         float widthHp = 61.0F;
         float newTargetHealth = hp / displayTarget.getMaxHealth() * widthHp;
         if (this.health == 0.0F || Math.abs(this.targetHealth - newTargetHealth) > 0.1F) {
            if (this.health == 0.0F) {
               this.health = newTargetHealth;
               this.targetHealth = newTargetHealth;
               this.animatedHealth = newTargetHealth;
            } else {
               this.targetHealth = newTargetHealth;
               this.healthAnimation.reset();
               this.healthAnimation.setDirection(Direction.FORWARDS);
            }
         }
      }

      if (mc.currentScreen != null && mc.player != null && mc.player.isAlive() && PlayerIntersectionUtil.isChat(mc.currentScreen)) {
         this.lastTarget = mc.player;
         this.startAnimation();
      } else {
         boolean shouldShow = displayTarget != null;
         if (shouldShow && !VisibleUtils.canBeTargeted(displayTarget)) {
            shouldShow = false;
         }

         if (shouldShow && hudModule.displayMode.getSelected().equals("3D")) {
            shouldShow = ProjectionUtil.canSeeEntity(displayTarget);
         }

         if (shouldShow) {
            this.startAnimation();
         } else {
            this.stopAnimation();
         }
      }

      this.updateRenderCache(hudModule);
   }

   private void updateRenderCache(padej.soup.implement.features.modules.hud.TargetHud hudModule) {
      this.cachedStyle = hudModule.style.getSelected();
      this.cachedAnchor = hudModule.anchor.getSelected();
      this.cachedDisplayMode = hudModule.displayMode.getSelected();
      this.cachedAnimationMode = hudModule.animationMode.getSelected();
      this.cachedScale = hudModule.scale.getValue();
      this.cachedXOffset = hudModule.xOffset.getValue();
      this.cachedLinkDistance = hudModule.linkDistance.getValue();
      this.cachedMaxLinks = (int)hudModule.maxLinks.getValue();
      this.cachedParticleSize = hudModule.particleSize.getValue();
      this.cachedShowItems = ServerLimitCfg.showItems();
      this.cachedShowItemsOverlay = ServerLimitCfg.showItemsOverlay();
      this.cachedShowItemUsingProgress = ServerLimitCfg.showItemUsingProgress();
      if (this.lastTarget == null) {
         this.cachedDisplayName = "";
         this.cachedHpString = "??";
         this.cachedShouldShowSkin = false;
         this.cachedIsPartiallyVisible = false;
         this.cachedShowHp = false;
         this.cachedFaceTexture = null;
         this.cachedEquippedItems.clear();
      } else {
         this.cachedDisplayName = VisibleUtils.getDisplayName(this.lastTarget);
         this.cachedShouldShowSkin = VisibleUtils.shouldShowSkin(this.lastTarget);
         this.cachedIsPartiallyVisible = VisibleUtils.isPartiallyVisible(this.lastTarget);
         this.cachedShowHp = ServerLimitCfg.showHp(this.lastTarget);
         if (!this.cachedShouldShowSkin && !this.cachedIsPartiallyVisible) {
            this.cachedFaceTexture = null;
         } else if (mc.getEntityRenderDispatcher().getRenderer(this.lastTarget) instanceof LivingEntityRenderer<?, ?, ?> livingRenderer) {
            this.cachedFaceTexture = resolveFaceTextureTyped(livingRenderer, this.lastTarget);
         } else {
            this.cachedFaceTexture = null;
         }

         if (this.cachedShowHp) {
            float hp = PlayerIntersectionUtil.getHealth(this.lastTarget);
            this.cachedHpString = PlayerIntersectionUtil.getHealthString(hp);
         } else {
            this.cachedHpString = "??";
         }

         this.cachedEquippedItems.clear();
         if (this.cachedShowItems) {
            for (ItemStack stack : this.lastTarget.getEquippedItems()) {
               if (!stack.isEmpty()) {
                  this.cachedEquippedItems.add(stack);
               }
            }
         }
      }
   }

   private void drawUsingItem(DrawContext context, MatrixStack matrix) {
      if (this.lastTarget != null && this.cachedShowItemUsingProgress) {
         this.animation.setDirection(this.lastTarget.isUsingItem() ? Direction.FORWARDS : Direction.BACKWARDS);
         if (!this.lastTarget.getActiveItem().isEmpty() && this.lastTarget.getActiveItem().getCount() != 0) {
            this.lastItem = this.lastTarget.getActiveItem().getItem();
         }

         if (!this.animation.isFinished(Direction.BACKWARDS) && !this.lastItem.equals(Items.AIR)) {
            int size = 24;
            float anim = this.animation.getOutputFloat();
            float progress = (this.lastTarget.getItemUseTime() + tickCounter.getTickDelta(false)) / ItemUtil.maxUseTick(this.lastItem) * 360.0F;
            float x = -(size + 5) * anim;
            float y = 6.0F;
            ScissorManager scissorManager = Main.getInstance().getScissorManager();
            scissorManager.push(matrix.peek().getPositionMatrix(), -50.0F, 0.0F, 50.0F, this.getHeight());
            MathUtil.setAlpha(
               anim,
               () -> {
                  blur.render(
                     ShapeProperties.create(matrix, x, y, size, size)
                        .round(12.0F)
                        .softness(1.0F)
                        .thickness(2.0F)
                        .outlineColor(ColorUtil.getOutline())
                        .color(ColorUtil.getBlurRect(0.7F))
                        .build()
                  );
                  arc.render(
                     ShapeProperties.create(matrix, x, y, size, size)
                        .round(0.4F)
                        .thickness(0.2F)
                        .end(progress)
                        .color(ColorUtil.fade(0), ColorUtil.fade(200), ColorUtil.fade(0), ColorUtil.fade(200))
                        .build()
                  );
               }
            );
            Matrix4f itemPosMatrix = new Matrix4f(matrix.peek().getPositionMatrix());
            Matrix3f itemNrmMatrix = new Matrix3f(matrix.peek().getNormalMatrix());
            HudRenderPipeline.getInstance().recordVanilla(() -> {
               matrix.push();
               matrix.peek().getPositionMatrix().set(itemPosMatrix);
               matrix.peek().getNormalMatrix().set(itemNrmMatrix);
               matrix.push();
               matrix.translate(x + 4.0F, y + 4.0F, 0.0F);
               context.drawItem(this.lastItem.getDefaultStack(), 0, 0);
               context.draw();
               matrix.pop();
               matrix.pop();
            }, HudRenderPipeline.VanillaLayer.AFTER_ARC);
            scissorManager.pop();
         }
      }
   }

   private void updateDimensionsForStyle() {
      String var1 = this.cachedStyle;
      switch (var1) {
         case "Default":
            this.setWidth(100);
            this.setHeight(36);
            break;
         case "Round":
            this.setWidth(120);
            this.setHeight(46);
      }
   }

   private LivingEntity getDisplayTarget() {
      if (this.targetEntity != null) {
         return this.targetEntity;
      } else {
         if (this.lastTarget != null) {
            padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
            long currentTime = System.currentTimeMillis();
            long liveTimeMs = (long)(hudModule.liveTime.getValue() * 1000.0F);
            if (hudModule.liveTime.getValue() == 0.0F) {
               return null;
            }

            if (currentTime - this.lastTargetTime < liveTimeMs) {
               if (!this.lastTarget.isDead() && this.lastTarget.isAlive()) {
                  return this.lastTarget;
               }

               return null;
            }
         }

         return null;
      }
   }

   @Override
   protected float getInteractionScale() {
      return this.cachedScale;
   }

   @Override
   public float getDraggableScale() {
      return 1.0F;
   }

   public StopWatch getStopWatch() {
      return this.stopWatch;
   }

   private record ParticleRenderData(TargetHudParticle particle, float x, float y, float scale) {
   }
}
