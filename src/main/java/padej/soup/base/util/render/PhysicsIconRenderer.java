package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.joml.Matrix4f;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.features.draggables.particles.TargetHudParticle;
import padej.soup.implement.features.modules.other.Trinket;
import padej.soup.implement.features.modules.particles.render.ParticleBatchRenderer;

public class PhysicsIconRenderer implements QuickImports {
   private static final float TARGET_FRAME_TIME = 16.67F;
   private float posX;
   private float posY;
   private float velocityX;
   private float velocityY;
   private boolean isDragging = false;
   private float dragOffsetX;
   private float dragOffsetY;
   private final Deque<PhysicsIconRenderer.PositionRecord> positionHistory = new ArrayDeque<>();
   private final Deque<PhysicsIconRenderer.TrailFrame> trailFrames = new ArrayDeque<>();
   private float screenWidth;
   private float screenHeight;
   private long lastUpdateTime;
   private long lastBounceSoundTime = 0L;
   private static final long BOUNCE_SOUND_COOLDOWN = 100L;
   private long lastTrailSaveTime = 0L;
   private static final long TRAIL_SAVE_INTERVAL = 16L;
   private List<PhysicsIconRenderer.Rectangle> uiRectangles = new ArrayList<>();
   private final List<TargetHudParticle> particles = new ArrayList<>();
   private final PhysicsIconRenderer.CachedSettings cachedSettings = new PhysicsIconRenderer.CachedSettings();
   private long lastSettingsUpdate = 0L;
   private static final long SETTINGS_CACHE_TIME = 50L;
   private float cachedSpeed = 0.0F;

   public PhysicsIconRenderer(int screenWidth, int screenHeight) {
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      float iconSize = this.getIconSize();
      this.posX = screenWidth / 2.0F - iconSize / 2.0F;
      this.posY = 10.0F;
      this.velocityX = 0.0F;
      this.velocityY = 0.0F;
      long currentTime = System.currentTimeMillis();
      this.lastUpdateTime = currentTime;
      this.lastTrailSaveTime = currentTime;
   }

   public void updateScreenSize(int screenWidth, int screenHeight) {
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
   }

   private void updateSettingsCache() {
      long currentTime = System.currentTimeMillis();
      if (currentTime - this.lastSettingsUpdate >= 50L) {
         this.lastSettingsUpdate = currentTime;
         Trinket module = Trinket.getInstance();
         if (module != null && module.isEnabled()) {
            this.cachedSettings.iconSize = module.getIconSize().getValue();
            this.cachedSettings.gravity = module.getGravity().getValue();
            this.cachedSettings.weight = module.getWeight().getValue();
            this.cachedSettings.friction = module.getFriction().getValue();
            this.cachedSettings.bounce = module.getBounce().getValue();
            this.cachedSettings.groundFriction = module.getGroundFriction().getValue();
            this.cachedSettings.minSpeed = module.getMinSpeed().getValue();
            this.cachedSettings.trailLength = (int)module.getTrailLength().getValue();
            this.cachedSettings.trailEnabled = module.getEnableTrail().isValue();
            this.cachedSettings.trailSizeModifier = module.getTrailSizeModifier().getValue();
            this.cachedSettings.trailColor = module.getTrailColorMode().isSelected("Custom") ? module.getTrailColor().getColor() : ColorUtil.getClientColor();
            String iconType = module.getIconType().getSelected().toLowerCase();
            this.cachedSettings.iconTexture = Identifier.of("textures/trinket/" + iconType + ".png");
            this.cachedSettings.iconColor = module.getIconColorMode().isSelected("Custom") ? module.getIconColor().getColor() : ColorUtil.getClientColor();
            this.cachedSettings.moduleEnabled = true;
         } else {
            this.cachedSettings.iconSize = 16.0F;
            this.cachedSettings.gravity = 0.5F;
            this.cachedSettings.weight = 1.0F;
            this.cachedSettings.friction = 0.98F;
            this.cachedSettings.bounce = 0.7F;
            this.cachedSettings.groundFriction = 0.9F;
            this.cachedSettings.minSpeed = 0.5F;
            this.cachedSettings.trailLength = 15;
            this.cachedSettings.trailEnabled = false;
            this.cachedSettings.trailSizeModifier = 1.0F;
            this.cachedSettings.trailColor = ColorUtil.getClientColor();
            this.cachedSettings.iconTexture = Identifier.of("textures/trinket/atom.png");
            this.cachedSettings.iconColor = -1;
            this.cachedSettings.moduleEnabled = false;
         }
      }
   }

   private float getIconSize() {
      return this.cachedSettings.iconSize;
   }

   private float getGravity() {
      return this.cachedSettings.gravity;
   }

   private float getWeight() {
      return this.cachedSettings.weight;
   }

   private float getFriction() {
      return this.cachedSettings.friction;
   }

   private float getBounce() {
      return this.cachedSettings.bounce;
   }

   private float getGroundFriction() {
      return this.cachedSettings.groundFriction;
   }

   private float getMinSpeed() {
      return this.cachedSettings.minSpeed;
   }

   private int getTrailLength() {
      return this.cachedSettings.trailLength;
   }

   private boolean isTrailEnabled() {
      return this.cachedSettings.trailEnabled;
   }

   private int getTrailColor() {
      return this.cachedSettings.trailColor;
   }

   private float getTrailSizeModifier() {
      return this.cachedSettings.trailSizeModifier;
   }

   private Identifier getIconTexture() {
      return this.cachedSettings.iconTexture;
   }

   private int getIconColor() {
      return this.cachedSettings.iconColor;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.updateSettingsCache();
      this.screenWidth = mc.getWindow().getScaledWidth();
      this.screenHeight = mc.getWindow().getScaledHeight();
      long currentTime = System.currentTimeMillis();
      float deltaTime = (float)(currentTime - this.lastUpdateTime);
      this.lastUpdateTime = currentTime;
      float timeMultiplier = deltaTime / 16.67F;
      this.saveCurrentFrame();
      this.updatePhysics(timeMultiplier);
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      this.renderTrails(matrices);
      this.updateAndRenderParticles(matrices, timeMultiplier);
      float iconSize = this.getIconSize();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      this.drawTexture(
         matrices,
         this.posX,
         this.posX + iconSize,
         this.posY,
         this.posY + iconSize,
         (int)iconSize,
         (int)iconSize,
         (int)iconSize,
         (int)iconSize,
         this.getIconTexture(),
         this.getIconColor()
      );
      RenderSystem.disableBlend();
      matrices.pop();
   }

   private void drawTexture(
      MatrixStack matrix,
      float x1,
      float x2,
      float y1,
      float y2,
      int regionWidth,
      int regionHeight,
      int textureWidth,
      int textureHeight,
      Identifier texture,
      int color
   ) {
      float u1 = 0.0F / textureWidth;
      float u2 = (0.0F + regionWidth) / textureWidth;
      float v1 = 0.0F / textureHeight;
      float v2 = (0.0F + regionHeight) / textureHeight;
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      int a = color >> 24 & 0xFF;
      RenderSystem.setShaderTexture(0, texture);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Matrix4f matrix4f = matrix.peek().getPositionMatrix();
      buffer.vertex(matrix4f, x1, y1, 0.0F).texture(u1, v1).color(r, g, b, a);
      buffer.vertex(matrix4f, x1, y2, 0.0F).texture(u1, v2).color(r, g, b, a);
      buffer.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2).color(r, g, b, a);
      buffer.vertex(matrix4f, x2, y1, 0.0F).texture(u2, v1).color(r, g, b, a);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   private void renderTrails(MatrixStack matrices) {
      if (this.isTrailEnabled() && !this.trailFrames.isEmpty()) {
         float minSpeed = this.getMinSpeed();
         if (!(this.cachedSpeed < minSpeed)) {
            float speedFactor = Math.min(this.cachedSpeed / 10.0F, 1.0F);
            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SrcFactor.ONE, DstFactor.ONE_MINUS_SRC_COLOR);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, Identifier.of("textures/particles/bloom/bloom.png"));
            int trailColor = this.getTrailColor();
            int r = trailColor >> 16 & 0xFF;
            int g = trailColor >> 8 & 0xFF;
            int b = trailColor & 0xFF;
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float iconSize = this.getIconSize();
            float sizeModifier = this.getTrailSizeModifier();
            int interpolationSteps = 4;
            long currentTime = System.currentTimeMillis();
            int totalTrailPoints = this.trailFrames.size() * (interpolationSteps + 1) + interpolationSteps;
            int pointIndex = 0;
            this.renderTrailPointBatched(buffer, matrix4f, this.posX, this.posY, pointIndex++, totalTrailPoints, speedFactor, iconSize, sizeModifier, r, g, b);
            if (!this.trailFrames.isEmpty()) {
               PhysicsIconRenderer.TrailFrame firstFrame = this.trailFrames.getFirst();

               for (int j = 1; j <= interpolationSteps; j++) {
                  float t = (float)j / (interpolationSteps + 1);
                  float interpX = this.posX + (firstFrame.x - this.posX) * t;
                  float interpY = this.posY + (firstFrame.y - this.posY) * t;
                  this.renderTrailPointBatched(buffer, matrix4f, interpX, interpY, pointIndex++, totalTrailPoints, speedFactor, iconSize, sizeModifier, r, g, b);
               }
            }

            Object[] frames = this.trailFrames.toArray();

            for (int i = 0; i < frames.length; i++) {
               PhysicsIconRenderer.TrailFrame currentFrame = (PhysicsIconRenderer.TrailFrame)frames[i];
               this.renderTrailPointBatched(
                  buffer, matrix4f, currentFrame.x, currentFrame.y, pointIndex++, totalTrailPoints, speedFactor, iconSize, sizeModifier, r, g, b
               );
               if (i < frames.length - 1) {
                  PhysicsIconRenderer.TrailFrame nextFrame = (PhysicsIconRenderer.TrailFrame)frames[i + 1];

                  for (int j = 1; j <= interpolationSteps; j++) {
                     float t = (float)j / (interpolationSteps + 1);
                     float interpX = currentFrame.x + (nextFrame.x - currentFrame.x) * t;
                     float interpY = currentFrame.y + (nextFrame.y - currentFrame.y) * t;
                     this.renderTrailPointBatched(
                        buffer, matrix4f, interpX, interpY, pointIndex++, totalTrailPoints, speedFactor, iconSize, sizeModifier, r, g, b
                     );
                  }
               }
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
         }
      }
   }

   private void renderTrailPointBatched(
      BufferBuilder buffer,
      Matrix4f matrix4f,
      float posX,
      float posY,
      int index,
      int totalPoints,
      float speedFactor,
      float iconSize,
      float sizeModifier,
      int r,
      int g,
      int b
   ) {
      float progress = (float)index / totalPoints;
      float size = iconSize * (1.0F - progress * 0.66F) * speedFactor * sizeModifier + 10.0F;
      float trailGradient = 1.0F - progress;
      float opacity = trailGradient * speedFactor * 0.8F;
      int a = (int)(opacity * 255.0F);
      if (a > 0) {
         float x = posX + iconSize / 2.0F - size / 2.0F;
         float y = posY + iconSize / 2.0F - size / 2.0F;
         buffer.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(r, g, b, a);
         buffer.vertex(matrix4f, x, y + size, 0.0F).texture(0.0F, 1.0F).color(r, g, b, a);
         buffer.vertex(matrix4f, x + size, y + size, 0.0F).texture(1.0F, 1.0F).color(r, g, b, a);
         buffer.vertex(matrix4f, x + size, y, 0.0F).texture(1.0F, 0.0F).color(r, g, b, a);
      }
   }

   private void updatePhysics(float timeMultiplier) {
      if (this.isDragging) {
         this.cachedSpeed = 0.0F;
      } else {
         float iconSize = this.getIconSize();
         float gravity = this.getGravity();
         float weight = this.getWeight();
         float friction = this.getFriction();
         float groundFriction = this.getGroundFriction();
         timeMultiplier = Math.min(timeMultiplier, 3.0F);
         float restThreshold = 0.5F;
         boolean onGround = this.posY >= this.screenHeight - iconSize - 0.1F;
         if (onGround && !(Math.abs(this.velocityY) > restThreshold)) {
            this.velocityY = 0.0F;
            this.posY = this.screenHeight - iconSize;
         } else {
            this.velocityY += gravity * weight * timeMultiplier;
         }

         this.posX = this.posX + this.velocityX * timeMultiplier;
         this.posY = this.posY + this.velocityY * timeMultiplier;
         float frictionFactor = (float)Math.pow(friction, timeMultiplier);
         this.velocityX *= frictionFactor;
         this.velocityY *= frictionFactor;
         this.checkBoundaryCollisions();
         if (!this.uiRectangles.isEmpty()) {
            for (PhysicsIconRenderer.Rectangle rect : this.uiRectangles) {
               this.bounceFromRect(rect.min.x, rect.min.y, rect.max.x - rect.min.x, rect.max.y - rect.min.y);
            }
         }

         if (onGround && Math.abs(this.velocityY) < restThreshold) {
            float groundFrictionFactor = (float)Math.pow(groundFriction, timeMultiplier);
            this.velocityX *= groundFrictionFactor;
         }

         this.cachedSpeed = (float)Math.sqrt(this.velocityX * this.velocityX + this.velocityY * this.velocityY);
      }
   }

   private void bounceFromRect(float rectX, float rectY, float rectWidth, float rectHeight) {
      float iconSize = this.getIconSize();
      float bounce = this.getBounce();
      float minBounceSpeed = 2.0F;
      if (this.posX + iconSize > rectX && this.posX < rectX + rectWidth && this.posY + iconSize > rectY && this.posY < rectY + rectHeight) {
         float dxLeft = Math.abs(this.posX + iconSize - rectX);
         float dxRight = Math.abs(this.posX - (rectX + rectWidth));
         float dyTop = Math.abs(this.posY + iconSize - rectY);
         float dyBottom = Math.abs(this.posY - (rectY + rectHeight));
         float minDist = Math.min(Math.min(dxLeft, dxRight), Math.min(dyTop, dyBottom));
         if (minDist == dxLeft) {
            this.posX = rectX - iconSize;
            if (Math.abs(this.velocityX) > minBounceSpeed) {
               this.playBounceSound();
               this.spawnBounceParticles(this.posX, this.posY);
            }

            this.velocityX *= -bounce;
         } else if (minDist == dxRight) {
            this.posX = rectX + rectWidth;
            if (Math.abs(this.velocityX) > minBounceSpeed) {
               this.playBounceSound();
               this.spawnBounceParticles(this.posX, this.posY);
            }

            this.velocityX *= -bounce;
         } else if (minDist == dyTop) {
            this.posY = rectY - iconSize;
            if (Math.abs(this.velocityY) > minBounceSpeed) {
               this.playBounceSound();
               this.spawnBounceParticles(this.posX, this.posY);
            }

            this.velocityY *= -bounce;
         } else if (minDist == dyBottom) {
            this.posY = rectY + rectHeight;
            if (Math.abs(this.velocityY) > minBounceSpeed) {
               this.playBounceSound();
               this.spawnBounceParticles(this.posX, this.posY);
            }

            this.velocityY *= -bounce;
         }
      }
   }

   private void checkBoundaryCollisions() {
      float iconSize = this.getIconSize();
      float bounce = this.getBounce();
      float restThreshold = 0.5F;
      float minBounceSpeed = 2.0F;
      if (this.posX <= 0.0F) {
         this.posX = 0.0F;
         if (Math.abs(this.velocityX) > minBounceSpeed) {
            this.playBounceSound();
            this.spawnBounceParticles(this.posX, this.posY);
         }

         this.velocityX = Math.abs(this.velocityX) * bounce;
      } else if (this.posX >= this.screenWidth - iconSize) {
         this.posX = this.screenWidth - iconSize;
         if (Math.abs(this.velocityX) > minBounceSpeed) {
            this.playBounceSound();
            this.spawnBounceParticles(this.posX, this.posY);
         }

         this.velocityX = -Math.abs(this.velocityX) * bounce;
      }

      if (this.posY <= 0.0F) {
         this.posY = 0.0F;
         if (Math.abs(this.velocityY) > minBounceSpeed) {
            this.playBounceSound();
            this.spawnBounceParticles(this.posX, this.posY);
         }

         this.velocityY = Math.abs(this.velocityY) * bounce;
      } else if (this.posY >= this.screenHeight - iconSize) {
         this.posY = this.screenHeight - iconSize;
         if (Math.abs(this.velocityY) < restThreshold) {
            this.velocityY = 0.0F;
         } else {
            if (Math.abs(this.velocityY) > minBounceSpeed) {
               this.playBounceSound();
               this.spawnBounceParticles(this.posX, this.posY);
            }

            this.velocityY = -Math.abs(this.velocityY) * bounce;
         }
      }
   }

   private void playBounceSound() {
      if (this.cachedSettings.moduleEnabled) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastBounceSoundTime >= 100L) {
            Trinket module = Trinket.getInstance();
            if (module != null) {
               float baseVolume = Math.min(this.cachedSpeed / 10.0F, 1.0F);
               float volumeMultiplier = module.getSoundVolume().getValue();
               float finalVolume = baseVolume * volumeMultiplier;
               if (finalVolume > 0.05F) {
                  float pitch = 0.8F + (float)QuickImports.random().nextDouble() * 0.4F;
                  SoundEvent sound = this.getSelectedSound(module);
                  SoundManager.playSound(sound, finalVolume, pitch);
                  this.lastBounceSoundTime = currentTime;
               }
            }
         }
      }
   }

   private SoundEvent getSelectedSound(Trinket module) {
      String soundType = module.getSoundType().getSelected();

      return switch (soundType) {
         case "8Bit" -> SoundManager.TRINKET_8BIT;
         case "Glass" -> SoundManager.TRINKET_GLASS;
         case "DrumBass" -> SoundManager.TRINKET_DRUM_BASS;
         case "AimBooster" -> SoundManager.TRINKET_AIMBOOSTER;
         case "Plastic" -> SoundManager.TRINKET_PLASTIC;
         case "Blip" -> SoundManager.TRINKET_BLIP;
         case "Pop" -> SoundManager.TRINKET_POP;
         default -> SoundManager.TRINKET_HIT;
      };
   }

   private void saveCurrentFrame() {
      long currentTime = System.currentTimeMillis();
      if (currentTime - this.lastTrailSaveTime >= 16L) {
         this.trailFrames.addFirst(new PhysicsIconRenderer.TrailFrame(this.posX, this.posY, currentTime));
         this.lastTrailSaveTime = currentTime;
      }

      long maxAge = this.getTrailLength() * 16L;

      while (!this.trailFrames.isEmpty() && currentTime - this.trailFrames.getLast().timestamp > maxAge) {
         this.trailFrames.removeLast();
      }
   }

   public void handleMouseClick(int mouseX, int mouseY, int button) {
      if (button == 0) {
         float iconSize = this.getIconSize();
         if (mouseX >= this.posX && mouseX <= this.posX + iconSize && mouseY >= this.posY && mouseY <= this.posY + iconSize) {
            this.startDrag(mouseX, mouseY);
         }
      }
   }

   public void handleMouseRelease(int mouseX, int mouseY, int button) {
      if (button == 0) {
         this.endDrag();
      }
   }

   public void handleMouseDrag(int mouseX, int mouseY) {
      if (this.isDragging) {
         this.updateDrag(mouseX, mouseY);
      }
   }

   private void startDrag(int mouseX, int mouseY) {
      this.isDragging = true;
      this.velocityX = 0.0F;
      this.velocityY = 0.0F;
      this.dragOffsetX = mouseX - this.posX;
      this.dragOffsetY = mouseY - this.posY;
      this.positionHistory.clear();
   }

   private void updateDrag(int mouseX, int mouseY) {
      float iconSize = this.getIconSize();
      float newX = mouseX - this.dragOffsetX;
      float newY = mouseY - this.dragOffsetY;
      this.posX = Math.max(0.0F, Math.min(this.screenWidth - iconSize, newX));
      this.posY = Math.max(0.0F, Math.min(this.screenHeight - iconSize, newY));
      this.positionHistory.add(new PhysicsIconRenderer.PositionRecord(this.posX, this.posY, System.currentTimeMillis()));
      if (this.positionHistory.size() > 5) {
         this.positionHistory.removeFirst();
      }
   }

   private void endDrag() {
      if (this.isDragging) {
         this.isDragging = false;
         this.calculateThrowVelocity();
      }
   }

   private void calculateThrowVelocity() {
      if (this.positionHistory.size() >= 2) {
         PhysicsIconRenderer.PositionRecord recent = this.positionHistory.getLast();
         PhysicsIconRenderer.PositionRecord older = this.positionHistory.getFirst();
         float timeDiff = (float)(recent.time - older.time) / 16.67F;
         if (timeDiff > 0.0F) {
            this.velocityX = (recent.x - older.x) / timeDiff * 1.2F;
            this.velocityY = (recent.y - older.y) / timeDiff * 1.2F;
            float maxSpeed = 25.0F;
            float speed = (float)Math.sqrt(this.velocityX * this.velocityX + this.velocityY * this.velocityY);
            if (speed > maxSpeed) {
               this.velocityX = this.velocityX / speed * maxSpeed;
               this.velocityY = this.velocityY / speed * maxSpeed;
            }
         }
      }
   }

   public boolean isMouseOver(int mouseX, int mouseY) {
      float iconSize = this.getIconSize();
      return mouseX >= this.posX && mouseX <= this.posX + iconSize && mouseY >= this.posY && mouseY <= this.posY + iconSize;
   }

   public void setUIRectangles(List<PhysicsIconRenderer.Rectangle> rectangles) {
      this.uiRectangles = rectangles;
   }

   private void spawnBounceParticles(float bounceX, float bounceY) {
      Trinket module = Trinket.getInstance();
      if (module != null && module.isEnabled() && module.getEnableParticles().isValue()) {
         int count = (int)module.getParticleCount().getValue();
         float size = module.getParticleSize().getValue();
         float lifetime = module.getParticleLifetime().getValue();
         float speedMultiplier = module.getParticleSpeed().getValue();
         float maxRadius = module.getParticleMaxRadius().getValue();
         int[] customColors = module.getCustomColors();
         List<String> selectedTypes = module.getParticleMode().getSelected();
         if (!selectedTypes.isEmpty()) {
            float iconSize = this.getIconSize();

            for (int i = 0; i < count; i++) {
               String randomType = selectedTypes.get(QuickImports.random().nextInt(selectedTypes.size()));
               TargetHudParticle.ParticleType type = TargetHudParticle.parseType(randomType);
               double angle = QuickImports.random().nextDouble() * Math.PI * 2.0;
               double speed = (QuickImports.random().nextDouble() * 2.0 + 1.0) * speedMultiplier;
               float motionX = (float)(Math.cos(angle) * speed);
               float motionY = (float)(Math.sin(angle) * speed);
               float spawnX = bounceX + iconSize / 2.0F;
               float spawnY = bounceY + iconSize / 2.0F;
               Color particleColor;
               if (module.getParticleColorMode().isSelected("Custom") && customColors != null && customColors.length > 0) {
                  if (module.getParticleColorAnimation().isSelected("Vertex")) {
                     int colorIndex = i % customColors.length;
                     particleColor = new Color(customColors[colorIndex], true);
                  } else {
                     long time = System.currentTimeMillis();
                     float phase = ((float)time / 1000.0F + i * 0.1F) % 1.0F;
                     int colorIndex = (int)(phase * customColors.length);
                     particleColor = new Color(customColors[colorIndex], true);
                  }
               } else {
                  int colorInt = ColorUtil.getClientColor();
                  particleColor = new Color(colorInt, true);
               }

               TargetHudParticle particle = new TargetHudParticle(
                  spawnX, spawnY, motionX, motionY, size, particleColor, type, lifetime, speedMultiplier, maxRadius, "Fly"
               );
               this.particles.add(particle);
            }
         }
      }
   }

   private void updateAndRenderParticles(MatrixStack matrices, float delta) {
      float tickDelta = Math.max(0.0F, Math.min(delta / 3.0F, 3.0F));
      this.particles.removeIf(particlex -> particlex.update(tickDelta));

      for (TargetHudParticle particle : this.particles) {
         particle.render(matrices, 0.0F, 0.0F, 1.0F);
      }

      ParticleBatchRenderer.renderBatches();
   }

   private static class CachedSettings {
      float iconSize = 16.0F;
      float gravity = 0.5F;
      float weight = 1.0F;
      float friction = 0.98F;
      float bounce = 0.7F;
      float groundFriction = 0.9F;
      float minSpeed = 0.5F;
      int trailLength = 15;
      boolean trailEnabled = false;
      float trailSizeModifier = 1.0F;
      int trailColor = -1;
      Identifier iconTexture = Identifier.of("textures/trinket/atom.png");
      int iconColor = -1;
      boolean moduleEnabled = false;
   }

   private static class PositionRecord {
      float x;
      float y;
      long time;

      PositionRecord(float x, float y, long time) {
         this.x = x;
         this.y = y;
         this.time = time;
      }
   }

   public record Rectangle(Vec2f min, Vec2f max) {
   }

   private static class TrailFrame {
      float x;
      float y;
      long timestamp;

      TrailFrame(float x, float y, long timestamp) {
         this.x = x;
         this.y = y;
         this.timestamp = timestamp;
      }
   }
}
