package padej.soup.implement.features.modules.particles;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.player.EventAttack;
import padej.soup.api.event.events.player.TickEvent;
import padej.soup.api.event.events.render.WorldRenderEvent;
import padej.soup.api.feature.module.IParticleModule;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.particle.ParticleUpdateExecutor;
import padej.soup.base.util.spatial.SpatialGrid3D;
import padej.soup.implement.features.modules.particles.render.NetworkRenderer;
import padej.soup.implement.features.modules.particles.types.WorldParticle;

public class HitParticles extends Module implements IParticleModule {
   public final SelectSetting colorMode = new SelectSetting("setting.hitparticles.colormode.name", "setting.hitparticles.colormode.desc")
      .value("Sync", "Custom")
      .selected("Sync");
   private final SelectSetting customColorsCount = new SelectSetting("setting.hitparticles.colorcount.name", "setting.hitparticles.colorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final MultiColorSetting customColors = new MultiColorSetting("setting.hitparticles.gradientcolors.name", "setting.hitparticles.gradientcolors.desc")
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.colorMode.isSelected("Custom"));
   public final SelectSetting colorAnimation = new SelectSetting("setting.hitparticles.coloranimation.name", "setting.hitparticles.coloranimation.desc")
      .value("Wave", "Vertex")
      .selected("Wave")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final MultiSelectSetting mode = new MultiSelectSetting("setting.hitparticles.mode.name", "setting.hitparticles.mode.desc")
      .selected("Stars", "Hearts", "Bloom")
      .value(
         "Stars",
         "Hearts",
         "Bloom",
         "Glyph",
         "Things",
         "Blink",
         "Coron",
         "Dollar",
         "Flame",
         "Geometric",
         "Snowflake",
         "Logo",
         "Virus",
         "SoupAPI Old",
         "Sword",
         "Network",
         "Cube",
         "Pyramid"
      );
   private final MultiSelectSetting physics = new MultiSelectSetting("setting.hitparticles.physics.name", "setting.hitparticles.physics.desc")
      .selected("Fly")
      .value("Fall", "Fly");
   private final ValueSetting scale = new ValueSetting("setting.hitparticles.scale.name", "setting.hitparticles.scale.desc").setValue(3.0F).range(1.0F, 10.0F);
   private final ColorSetting healColor = new ColorSetting("setting.hitparticles.healcolor.name", "setting.hitparticles.healcolor.desc").value(-13641408);
   private final ColorSetting damageColor = new ColorSetting("setting.hitparticles.damagecolor.name", "setting.hitparticles.damagecolor.desc").value(-973005);
   public final ValueSetting linkDistance = new ValueSetting("setting.hitparticles.linkdistance.name", "setting.hitparticles.linkdistance.desc")
      .setValue(3.0F)
      .range(1.0F, 10.0F)
      .visible(() -> this.mode.getSelected().contains("Network"));
   private final SelectSetting spawnLocation = new SelectSetting("setting.hitparticles.spawnloc.name", "setting.hitparticles.spawnloc.desc")
      .value("Center", "Hit Loc")
      .selected("Hit Loc");
   private final BooleanSetting showSelf = new BooleanSetting("setting.hitparticles.showself.name", "setting.hitparticles.showself.desc").setValue(true);
   private final BooleanSetting onlyCrits = new BooleanSetting("setting.hitparticles.onlycrits.name", "setting.hitparticles.onlycrits.desc").setValue(false);
   private final ValueSetting amount = new ValueSetting("setting.hitparticles.amount.name", "setting.hitparticles.amount.desc").setValue(2.0F).range(1, 5);
   private final ValueSetting spawnFrequency = new ValueSetting("setting.hitparticles.spawnfrequency.name", "setting.hitparticles.spawnfrequency.desc")
      .setValue(1.0F)
      .range(1, 10);
   private final ValueSetting lifeTime = new ValueSetting("setting.hitparticles.lifetime.name", "setting.hitparticles.lifetime.desc")
      .setValue(2.0F)
      .range(1, 15);
   private final ValueSetting speed = new ValueSetting("setting.hitparticles.speed.name", "setting.hitparticles.speed.desc").setValue(2.0F).range(1, 20);
   private final HashMap<Integer, Float> healthMap = new HashMap<>();
   private boolean wasLastAttackCrit = false;
   private final List<WorldParticle> particles = new ArrayList<>();
   private final List<WorldParticle> networkParticles = new ArrayList<>();
   private final List<HitParticles.Emitter> emitters = new ArrayList<>();
   private SpatialGrid3D<WorldParticle> spatialGrid = null;

   public static HitParticles getInstance() {
      return Instance.get(HitParticles.class);
   }

   public HitParticles() {
      super("module.hitparticles.name", ModuleCategory.PARTICLES);
      GroupSetting colorGroup = new GroupSetting("group.hitparticles.colors.name", "group.hitparticles.colors.desc", false)
         .settings(this.colorMode, this.customColorsCount, this.customColors, this.colorAnimation);
      GroupSetting textColorGroup = new GroupSetting("group.hitparticles.textcolors.name", "group.hitparticles.textcolors.desc", false)
         .settings(this.healColor, this.damageColor)
         .visible(() -> this.mode.getSelected().contains("Text"));
      GroupSetting appearanceGroup = new GroupSetting("group.hitparticles.appearance.name", "group.hitparticles.appearance.desc", false)
         .settings(this.mode, this.physics, this.scale);
      GroupSetting behaviorGroup = new GroupSetting("group.hitparticles.behavior.name", "group.hitparticles.behavior.desc", false)
         .settings(this.lifeTime, this.speed);
      GroupSetting spawnGroup = new GroupSetting("group.hitparticles.spawn.name", "group.hitparticles.spawn.desc", false)
         .settings(this.spawnLocation, this.showSelf, this.onlyCrits, this.amount, this.spawnFrequency);
      this.setup(new Setting[]{colorGroup, textColorGroup, appearanceGroup, behaviorGroup, spawnGroup, this.linkDistance});
   }

   @Override
   public SelectSetting getColorMode() {
      return this.colorMode;
   }

   @Override
   public SelectSetting getColorAnimation() {
      return this.colorAnimation;
   }

   @Override
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

   @Override
   public int getHealColor() {
      return this.healColor.getColor();
   }

   @Override
   public int getDamageColor() {
      return this.damageColor.getColor();
   }

   private String getRandomMode() {
      List<String> selected = this.mode.getSelected();
      int allowedCount = 0;

      for (String particleMode : selected) {
         if (!"Text".equals(particleMode)) {
            allowedCount++;
         }
      }

      if (allowedCount == 0) {
         return "Stars";
      } else {
         int targetIndex = ThreadLocalRandom.current().nextInt(allowedCount);

         for (String particleModex : selected) {
            if (!"Text".equals(particleModex) && targetIndex-- == 0) {
               return particleModex;
            }
         }

         return "Stars";
      }
   }

   private String getRandomPhysics() {
      List<String> selected = this.physics.getSelected();
      return selected.isEmpty() ? "Fall" : selected.get(ThreadLocalRandom.current().nextInt(selected.size()));
   }

   private boolean isCrit() {
      PlayerEntity player = mc.player;
      if (player == null) {
         return false;
      } else {
         boolean bl = player.getAttackCooldownProgress(0.5F) > 0.9F;
         return bl
            && player.fallDistance > 0.0F
            && !player.isOnGround()
            && !player.isClimbing()
            && !player.isTouchingWater()
            && !player.hasStatusEffect(StatusEffects.BLINDNESS)
            && !player.hasVehicle()
            && !player.isSprinting();
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      long currentTime = System.currentTimeMillis();
      ParticleUpdateExecutor.updateParticlesInPlace(this.particles, currentTime, p -> true, null, particle -> {
         if ("Network".equals(particle.getParticleMode())) {
            if (this.spatialGrid != null) {
               this.spatialGrid.remove(particle, particle.getX(), particle.getY(), particle.getZ());
            }

            this.networkParticles.remove(particle);
         }
      });
      if (this.spatialGrid != null && !this.networkParticles.isEmpty()) {
         for (WorldParticle particle : this.networkParticles) {
            this.spatialGrid.update(particle, particle.getPx(), particle.getPy(), particle.getPz(), particle.getX(), particle.getY(), particle.getZ());
         }
      }

      if (mc.player != null) {
         float currentCooldown = mc.player.getAttackCooldownProgress(0.5F);
         if (currentCooldown > 0.9F) {
            this.wasLastAttackCrit = this.isCrit();
         }
      }

      if (!this.emitters.isEmpty()) {
         for (HitParticles.Emitter emitter : this.emitters) {
            this.spawnParticlesFromEmitter(emitter);
         }

         for (int i = this.emitters.size() - 1; i >= 0; i--) {
            if (this.emitters.get(i).tick(currentTime)) {
               this.emitters.remove(i);
            }
         }
      }

      if (this.mode.isSelected("Text")) {
         for (Entity entity : mc.world.getEntities()) {
            if (entity != null && !(mc.player.squaredDistanceTo(entity) > 256.0) && entity.isAlive() && entity instanceof LivingEntity lent) {
               int colorInt;
               if (this.colorMode.isSelected("Sync")) {
                  colorInt = ColorUtil.getClientColor();
               } else {
                  int[] colors = this.getCustomColors();
                  colorInt = colors != null && colors.length > 0 ? colors[0] : -1;
               }

               float health = lent.getHealth() + lent.getAbsorptionAmount();
               float lastHealth = this.healthMap.getOrDefault(entity.getId(), health);
               this.healthMap.put(entity.getId(), health);
               if (lastHealth != health) {
                  ThreadLocalRandom random = ThreadLocalRandom.current();
                  this.particles
                     .add(
                        new WorldParticle(
                           (float)lent.getX(),
                           (float)(lent.getY() + random.nextDouble() * lent.getHeight()),
                           (float)lent.getZ(),
                           new Color(colorInt),
                           (float)(random.nextDouble() * 180.0),
                           (float)(random.nextDouble() * 25.0 + 5.0),
                           health - lastHealth,
                           "Text",
                           this.getRandomPhysics(),
                           null,
                           this.lifeTime.getValue(),
                           this.scale.getValue(),
                           this.speed.getValue(),
                           this
                        )
                     );
               }
            }
         }
      }
   }

   @EventHandler
   public void onAttack(EventAttack event) {
      if (mc.player != null && mc.world != null) {
         if (event.isPre()) {
            if (mc.player.getAttackCooldownProgress(0.5F) > 0.9F) {
               this.wasLastAttackCrit = this.isCrit();
            }
         } else if (event.getTarget() != null) {
            if (this.showSelf.isValue() || event.getTarget() != mc.player) {
               if (!this.onlyCrits.isValue() || this.wasLastAttackCrit) {
                  Vec3d point = this.spawnLocation.isSelected("Hit Loc") ? event.getHitPos() : this.getEntityCenter(event.getTarget());
                  if (point != null) {
                     this.emitters.add(new HitParticles.Emitter(point, System.currentTimeMillis(), (int)this.spawnFrequency.getValue()));
                  }
               }
            }
         }
      }
   }

   private Vec3d getEntityCenter(Entity target) {
      return target == null ? null : target.getPos().add(0.0, target.getHeight() / 2.0, 0.0);
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      MatrixStack stack = event.getStack();
      if (mc.player != null && mc.world != null) {
         long currentTime = System.currentTimeMillis();

         for (WorldParticle particle : this.particles) {
            particle.render(stack, currentTime);
         }

         if (this.mode.isSelected("Network") && this.spatialGrid != null && !this.networkParticles.isEmpty()) {
            NetworkRenderer.renderNetworkLinks(stack, this.networkParticles, this.spatialGrid, currentTime, this.linkDistance.getValue(), this);
         }
      }
   }

   private void spawnParticlesFromEmitter(HitParticles.Emitter emitter) {
      ThreadLocalRandom random = ThreadLocalRandom.current();

      for (int i = 0; i < this.amount.getValue(); i++) {
         String selectedMode = this.getRandomMode();
         String selectedPhysics = selectedMode.equals("Network") ? "Fly" : this.getRandomPhysics();
         int colorInt;
         if (this.colorMode.isSelected("Sync")) {
            colorInt = ColorUtil.getClientColor();
         } else {
            int[] colors = this.getCustomColors();
            colorInt = colors != null && colors.length > 0 ? colors[0] : -1;
         }

         float x = (float)emitter.position.x;
         float y = (float)emitter.position.y;
         float z = (float)emitter.position.z;
         WorldParticle particle = new WorldParticle(
            x,
            y,
            z,
            new Color(colorInt),
            (float)(random.nextDouble() * 360.0),
            (float)(random.nextDouble() * 25.0 + 5.0),
            0.0F,
            selectedMode,
            selectedPhysics,
            selectedMode.equals("Glyph")
               ? ParticleData.getRandomGlyphTexture()
               : (selectedMode.equals("Things") ? ParticleData.getRandomGlyphAltTexture() : null),
            this.lifeTime.getValue(),
            this.scale.getValue(),
            this.speed.getValue(),
            this
         ) {
            @Override
            protected void initMotion(float speed) {
               ThreadLocalRandom rng = ThreadLocalRandom.current();
               float scale = speed * 0.05F;
               this.motionX = (float)(rng.nextGaussian() * scale);
               this.motionY = (float)(rng.nextGaussian() * scale);
               this.motionZ = (float)(rng.nextGaussian() * scale);
            }
         };
         this.particles.add(particle);
         if (selectedMode.equals("Network")) {
            if (this.spatialGrid == null) {
               this.spatialGrid = new SpatialGrid3D<>(this.linkDistance.getValue());
            }

            this.networkParticles.add(particle);
            this.spatialGrid.insert(particle, x, y, z);
         }
      }
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.particles.clear();
      this.networkParticles.clear();
      this.emitters.clear();
      if (this.spatialGrid != null) {
         this.spatialGrid = null;
      }
   }

   public SpatialGrid3D<WorldParticle> getSpatialGrid() {
      return this.spatialGrid;
   }

   private record Emitter(Vec3d position, long startTime, int duration) {
      public boolean tick(long currentTime) {
         long durationMs = this.duration * 50L;
         return currentTime - this.startTime > durationMs;
      }
   }
}
