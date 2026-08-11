package padej.soup.implement.features.modules.visuals;

import java.awt.Color;
import java.util.List;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import padej.soup.api.accessor.IEntity;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.player.TickEvent;
import padej.soup.api.event.events.render.WorldRenderEvent;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.api.repository.friend.FriendUtils;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.VisibleUtils;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.render.TrailRenderer;
import padej.soup.core.server.ServerConfigManager;
import padej.soup.core.server.ServerLimitCfg;

public class Trails extends Module {
   private static final float CROUCH_HEIGHT = 1.5F;
   private long lastTrailUpdateTime = 0L;
   private boolean clearedForNoneMode = false;
   private final SelectSetting colorMode = new SelectSetting("setting.trails.colormode.name", "setting.trails.colormode.desc")
      .value("Sync", "Custom")
      .selected("Sync");
   private final SelectSetting customColorsCount = new SelectSetting("setting.trails.colorcount.name", "setting.trails.colorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final MultiColorSetting customColors = new MultiColorSetting("setting.trails.gradientcolors.name", "setting.trails.gradientcolors.desc")
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final SelectSetting colorAnimation = new SelectSetting("setting.trails.coloranimation.name", "setting.trails.coloranimation.desc")
      .value("Wave", "Vertex")
      .selected("Wave")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final SelectSetting players = new SelectSetting("setting.trails.players.name", "setting.trails.players.desc")
      .value("Trail", "Tail", "None")
      .selected("Trail");
   private final BooleanSetting friends = new BooleanSetting("setting.trails.friends.name", "setting.trails.friends.desc")
      .setValue(false)
      .visible(() -> !this.players.isSelected("None"));
   private final BooleanSetting onlyF5 = new BooleanSetting("setting.trails.onlyf5.name", "setting.trails.onlyf5.desc")
      .setValue(false)
      .visible(() -> this.players.isSelected("Trail"));
   private final SelectSetting style = new SelectSetting("setting.trails.style.name", "setting.trails.style.desc")
      .value("Solid", "Faded", "Invert")
      .selected("Solid")
      .visible(() -> this.players.isSelected("Trail"));
   private final BooleanSetting renderHalf = new BooleanSetting("setting.trails.renderhalf.name", "setting.trails.renderhalf.desc")
      .setValue(false)
      .visible(() -> this.players.isSelected("Trail"));
   private final ValueSetting alphaFactor = new ValueSetting("setting.trails.alphafactor.name", "setting.trails.alphafactor.desc")
      .setValue(50.0F)
      .range(0.0F, 100.0F)
      .visible(() -> this.players.isSelected("Trail") && this.style.isSelected("Invert"));
   private final ValueSetting trailAlpha = new ValueSetting("setting.trails.alpha.name", "setting.trails.alpha.desc")
      .setValue(100.0F)
      .range(0.0F, 100.0F)
      .visible(() -> this.players.isSelected("Trail"));
   private final ValueSetting down = new ValueSetting("setting.trails.down.name", "setting.trails.down.desc").setValue(0.0F).range(-1.0F, 1.0F);
   private final ValueSetting trailDuration = new ValueSetting("setting.trails.duration.name", "setting.trails.duration.desc")
      .setValue(1.5F)
      .range(1.0F, 4.0F)
      .visible(() -> this.players.isSelected("Trail"));
   private final BooleanSetting debugLines = new BooleanSetting("setting.trails.debuglines.name", "setting.trails.debuglines.desc").setValue(false);
   private final ValueSetting lineWidth = new ValueSetting("setting.trails.linewidth.name", "setting.trails.linewidth.desc")
      .setValue(3.0F)
      .range(1.5F, 5.0F)
      .visible(this.debugLines::isValue);

   public static Trails getInstance() {
      return Instance.get(Trails.class);
   }

   public Trails() {
      super("module.trails.name", ModuleCategory.VISUALS);
      GroupSetting colorGroup = new GroupSetting("group.trails.colors.name", "group.trails.colors.desc", false)
         .settings(this.colorMode, this.customColorsCount, this.customColors, this.colorAnimation);
      GroupSetting renderGroup = new GroupSetting("group.trails.render.name", "group.trails.render.desc", false)
         .settings(this.players, this.friends, this.onlyF5);
      GroupSetting styleGroup = new GroupSetting("group.trails.style.name", "group.trails.style.desc", false)
         .settings(this.style, this.renderHalf, this.alphaFactor, this.trailAlpha)
         .visible(() -> this.players.isSelected("Trail"));
      GroupSetting dimensionGroup = new GroupSetting("group.trails.dimensions.name", "group.trails.dimensions.desc", false)
         .settings(this.down, this.trailDuration, this.debugLines, this.lineWidth)
         .visible(() -> this.players.isSelected("Trail"));
      this.setup(new Setting[]{colorGroup, renderGroup, styleGroup, dimensionGroup});
   }

   public int[] getCustomColors() {
      if (!this.colorMode.isSelected("Custom")) {
         return null;
      } else {
         String var1 = this.customColorsCount.getSelected();

         return switch (var1) {
            case "Solo" -> new int[]{this.customColors.getColor1().getColor()};
            case "Duo" -> new int[]{this.customColors.getColor1().getColor(), this.customColors.getColor2().getColor()};
            case "Triple" -> new int[]{
               this.customColors.getColor1().getColor(), this.customColors.getColor2().getColor(), this.customColors.getColor3().getColor()
            };
            case "Quartet" -> this.customColors.getColorValues();
            default -> null;
         };
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.players.isSelected("None")) {
         if (!this.clearedForNoneMode) {
            this.clearAllTrails();
            this.clearedForNoneMode = true;
         }
      } else {
         this.clearedForNoneMode = false;
         if (this.players.isSelected("Tail")) {
            this.updateTrails();
         }

         this.cleanupExpiredTrails();
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      MatrixStack stack = event.getStack();
      if (this.players.isSelected("Trail")) {
         long currentTime = System.currentTimeMillis();
         long frameDuration = currentTime - this.lastTrailUpdateTime;
         if (frameDuration > 0L) {
            this.lastTrailUpdateTime = currentTime;
            this.updateTrails();
         }

         this.queuePlayerTrails(stack);
      } else if (this.players.isSelected("Tail")) {
         this.queuePlayerTails(stack);
      }

      TrailRenderer.renderBatches();
   }

   private void queuePlayerTrails(MatrixStack stack) {
      if (mc.world != null) {
         Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
         double maxRenderDistSq = 4096.0;

         for (AbstractClientPlayerEntity entity : mc.world.getPlayers()) {
            if ((
                  entity == mc.player
                     ? !this.onlyF5.isValue() || !mc.options.getPerspective().isFirstPerson()
                     : this.friends.isValue() && FriendUtils.isFriend(entity)
               )
               && !(entity.squaredDistanceTo(cameraPos) > maxRenderDistSq)
               && (!entity.isGliding() || ServerLimitCfg.showElytraTrail())) {
               VisibleUtils.VisibilityLevel level = VisibleUtils.getVisibilityLevel(entity);
               if (ServerConfigManager.showTrails(level.name())) {
                  List<Trails.Trail> trails = ((IEntity)entity).getTrails();
                  if (!trails.isEmpty()) {
                     TrailRenderer.queueTrails(stack, trails, this, entity);
                     if (this.debugLines.isValue()) {
                        TrailRenderer.queueDebugLines(stack, trails, this, entity);
                     }
                  }
               }
            }
         }
      }
   }

   private void queuePlayerTails(MatrixStack stack) {
      if (mc.world != null) {
         Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
         double maxRenderDistSq = 4096.0;

         for (AbstractClientPlayerEntity entity : mc.world.getPlayers()) {
            if ((entity == mc.player ? !mc.options.getPerspective().isFirstPerson() : this.friends.isValue() && FriendUtils.isFriend(entity))
               && !(entity.squaredDistanceTo(cameraPos) > maxRenderDistSq)
               && (!entity.isGliding() || ServerLimitCfg.showElytraTrail())) {
               VisibleUtils.VisibilityLevel level = VisibleUtils.getVisibilityLevel(entity);
               if (ServerConfigManager.showTrails(level.name())) {
                  List<Trails.Trail> trails = ((IEntity)entity).getTrails();
                  if (!trails.isEmpty()) {
                     TrailRenderer.queueTails(stack, trails, this);
                  }
               }
            }
         }
      }
   }

   private void updateTrails() {
      if (mc.world != null && mc.player != null) {
         Color c;
         if (this.colorMode.isSelected("Custom")) {
            int[] colors = this.getCustomColors();
            c = new Color(colors != null && colors.length > 0 ? colors[0] : -1);
         } else {
            c = new Color(ColorUtil.getClientColor());
         }

         long lifetime = (long)(this.trailDuration.getValue() * 1000.0F);
         double minDistance = 0.005;
         float fadeStrength = this.trailDuration.getValue();
         float opacity = this.trailAlpha.getValue() / 100.0F;
         boolean isCustomColor = this.colorMode.isSelected("Custom");
         boolean isWaveAnimation = this.colorAnimation.isSelected("Wave");
         int[] customColors = isCustomColor ? this.getCustomColors() : null;
         long currentTime = System.currentTimeMillis();

         for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            List<Trails.Trail> trails = ((IEntity)player).getTrails();
            float currentHitboxHeight = this.getPlayerHitboxHeight(player);
            boolean shouldCreateTrail = false;
            if (!player.isGliding() || ServerLimitCfg.showElytraTrail()) {
               VisibleUtils.VisibilityLevel level = VisibleUtils.getVisibilityLevel(player);
               if (ServerConfigManager.showTrails(level.name())) {
                  if (player != mc.player
                     && (player.getPos().x != player.prevX || player.getPos().z != player.prevZ)
                     && this.friends.isValue()
                     && FriendUtils.isFriend(player)) {
                     shouldCreateTrail = true;
                  }

                  if (player == mc.player
                     && (mc.player.getVelocity().x != 0.0 || mc.player.getVelocity().z != 0.0)
                     && (!this.onlyF5.isValue() || !mc.options.getPerspective().isFirstPerson())) {
                     shouldCreateTrail = true;
                  }

                  if (shouldCreateTrail) {
                     Vec3d interpolatedPos = MathUtil.interpolate(player);
                     if (trails.isEmpty()) {
                        Vec3d prevPos = new Vec3d(player.prevX, player.prevY, player.prevZ);
                        Trails.Trail trail = new Trails.Trail(prevPos, interpolatedPos, c, currentHitboxHeight, lifetime);
                        trail.setAlphaParams(fadeStrength, opacity);
                        trail.update(currentTime);
                        if (isCustomColor) {
                           trail.updateColor(customColors, isWaveAnimation, currentTime);
                        }

                        trails.add(trail);
                     } else {
                        Trails.Trail last = trails.get(trails.size() - 1);
                        double distance = last.currentPos.distanceTo(interpolatedPos);
                        if (distance >= minDistance) {
                           Trails.Trail trail = new Trails.Trail(last.currentPos, interpolatedPos, c, currentHitboxHeight, lifetime);
                           trail.setAlphaParams(fadeStrength, opacity);
                           trail.update(currentTime);
                           if (isCustomColor) {
                              trail.updateColor(customColors, isWaveAnimation, currentTime);
                           }

                           trails.add(trail);
                        }
                     }
                  }

                  for (Trails.Trail trail : trails) {
                     trail.setEntityHeight(currentHitboxHeight);
                     trail.update(currentTime);
                     if (isCustomColor && isWaveAnimation) {
                        trail.updateColor(customColors, true, currentTime);
                     }
                  }
               }
            }
         }
      }
   }

   private void cleanupExpiredTrails() {
      if (mc.world != null) {
         long currentTime = System.currentTimeMillis();

         for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            List<Trails.Trail> trails = ((IEntity)player).getTrails();
            trails.removeIf(trail -> trail.shouldRemove(currentTime));
         }
      }
   }

   private float getPlayerHitboxHeight(PlayerEntity player) {
      float hitboxHeight = (float)player.getBoundingBox().getLengthY();
      return (player != mc.player || !mc.options.sneakKey.isPressed()) && !player.isSneaking() ? hitboxHeight : Math.min(hitboxHeight, 1.5F);
   }

   @Override
   public void deactivate() {
      this.clearAllTrails();
      this.clearedForNoneMode = false;
      this.lastTrailUpdateTime = 0L;
   }

   private void clearAllTrails() {
      if (mc.world != null) {
         for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            ((IEntity)player).getTrails().clear();
         }
      }
   }

   public SelectSetting getColorMode() {
      return this.colorMode;
   }

   public SelectSetting getColorAnimation() {
      return this.colorAnimation;
   }

   public SelectSetting getStyle() {
      return this.style;
   }

   public BooleanSetting getRenderHalf() {
      return this.renderHalf;
   }

   public ValueSetting getAlphaFactor() {
      return this.alphaFactor;
   }

   public ValueSetting getTrailAlpha() {
      return this.trailAlpha;
   }

   public ValueSetting getDown() {
      return this.down;
   }

   public ValueSetting getTrailDuration() {
      return this.trailDuration;
   }

   public ValueSetting getLineWidth() {
      return this.lineWidth;
   }

   public static class Trail implements QuickImports {
      private final Vec3d prevPos;
      public Vec3d currentPos;
      private final Color color;
      public float entityHeight;
      private final long creationTime;
      private final long maxLifetime;
      private long lastCacheTime = -1L;
      private float cachedProgress = 0.0F;
      private float cachedAlpha = 1.0F;
      private float cachedRenderAlpha = 1.0F;
      public int cachedColor;
      private float fadeStrength = 1.5F;
      private float opacity = 1.0F;

      public Trail(Vec3d from, Vec3d to, Color color, float entityHeight, long maxLifetimeMs) {
         this.prevPos = from;
         this.currentPos = to;
         this.color = color;
         this.entityHeight = entityHeight;
         this.creationTime = System.currentTimeMillis();
         this.maxLifetime = maxLifetimeMs;
         this.cachedColor = color.getRGB();
      }

      public void setAlphaParams(float fadeStrength, float opacity) {
         this.fadeStrength = fadeStrength;
         this.opacity = opacity;
      }

      public void setEntityHeight(float entityHeight) {
         this.entityHeight = entityHeight;
      }

      public void updateColor(int[] customColors, boolean isWave, long currentTime) {
         float progress = this.getProgress();
         this.cachedColor = this.computeColor(customColors, isWave, progress, currentTime);
      }

      private int computeColor(int[] colors, boolean isWave, float progress, long currentTime) {
         if (colors == null || colors.length <= 0) {
            return this.color.getRGB();
         } else if (isWave) {
            return this.getWaveColorStatic(colors, currentTime);
         } else {
            float colorIndex = progress * colors.length;
            int index1 = Math.min((int)colorIndex, colors.length - 1);
            int index2 = Math.min(index1 + 1, colors.length - 1);
            float lerp = colorIndex - (int)colorIndex;
            return ColorUtil.overCol(colors[index1], colors[index2], lerp);
         }
      }

      private int getWaveColorStatic(int[] colors, long time) {
         if (colors.length == 1) {
            return colors[0];
         } else {
            int angle = (int)(time / 8L % 360L);
            if (colors.length == 2) {
               angle = angle >= 180 ? 360 - angle : angle;
               return ColorUtil.overCol(colors[0], colors[1], angle / 180.0F);
            } else {
               float timeProgress = (float)(time / 10L % (colors.length * 360L)) / 360.0F;
               int index1 = (int)Math.floor(timeProgress) % colors.length;
               int index2 = (index1 + 1) % colors.length;
               float lerp = timeProgress - (int)Math.floor(timeProgress);
               return ColorUtil.overCol(colors[index1], colors[index2], lerp);
            }
         }
      }

      public Vec3d interpolate(float pt) {
         double x = this.prevPos.x + (this.currentPos.x - this.prevPos.x) * pt;
         double y = this.prevPos.y + (this.currentPos.y - this.prevPos.y) * pt;
         double z = this.prevPos.z + (this.currentPos.z - this.prevPos.z) * pt;
         return new Vec3d(x, y, z);
      }

      public Color color() {
         return this.color;
      }

      private void updateCache(long currentTime) {
         if (currentTime != this.lastCacheTime) {
            this.lastCacheTime = currentTime;
            long age = currentTime - this.creationTime;
            this.cachedProgress = Math.min(1.0F, (float)age / (float)this.maxLifetime);
            this.cachedAlpha = 1.0F - this.cachedProgress;
            float smoothAlpha = (float)Math.pow(this.cachedAlpha, this.fadeStrength);
            this.cachedRenderAlpha = Math.max(0.0F, Math.min(1.0F, smoothAlpha * this.opacity));
         }
      }

      public float getProgress() {
         return this.cachedProgress;
      }

      public float getAlpha() {
         return this.cachedRenderAlpha;
      }

      public void update(long currentTime) {
         this.updateCache(currentTime);
      }

      public boolean shouldRemove(long currentTime) {
         return currentTime - this.creationTime >= this.maxLifetime;
      }
   }
}
