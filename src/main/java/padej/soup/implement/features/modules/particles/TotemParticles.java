package padej.soup.implement.features.modules.particles;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.player.TickEvent;
import padej.soup.api.event.events.render.WorldRenderEvent;
import padej.soup.api.feature.module.IParticleModule;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
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

public class TotemParticles extends Module implements IParticleModule {
   public final SelectSetting colorMode = new SelectSetting("setting.totemparticles.colormode.name", "setting.totemparticles.colormode.desc")
      .value("Sync", "Vanilla", "Custom")
      .selected("Vanilla");
   private final SelectSetting customColorsCount = new SelectSetting("setting.totemparticles.colorcount.name", "setting.totemparticles.colorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final MultiColorSetting customColors = new MultiColorSetting(
         "setting.totemparticles.gradientcolors.name", "setting.totemparticles.gradientcolors.desc"
      )
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.colorMode.isSelected("Custom"));
   public final SelectSetting colorAnimation = new SelectSetting("setting.totemparticles.coloranimation.name", "setting.totemparticles.coloranimation.desc")
      .value("Wave", "Vertex")
      .selected("Wave")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final MultiSelectSetting mode = new MultiSelectSetting("setting.totemparticles.mode.name", "setting.totemparticles.mode.desc")
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
      )
      .selected("Bloom");
   private final MultiSelectSetting physics = new MultiSelectSetting("setting.totemparticles.physics.name", "setting.totemparticles.physics.desc")
      .value("Fall", "Fly")
      .selected("Fly");
   private final ValueSetting particleCount = new ValueSetting("setting.totemparticles.particlecount.name", "setting.totemparticles.particlecount.desc")
      .setValue(30.0F)
      .range(10, 70);
   private final ValueSetting emitterDuration = new ValueSetting("setting.totemparticles.emitterduration.name", "setting.totemparticles.emitterduration.desc")
      .setValue(30.0F)
      .range(10, 60);
   private final ValueSetting lifeTime = new ValueSetting("setting.totemparticles.lifetime.name", "setting.totemparticles.lifetime.desc")
      .setValue(3.0F)
      .range(1, 10);
   private final ValueSetting speed = new ValueSetting("setting.totemparticles.speed.name", "setting.totemparticles.speed.desc")
      .setValue(0.5F)
      .range(0.1F, 3.0F);
   private final ValueSetting scale = new ValueSetting("setting.totemparticles.scale.name", "setting.totemparticles.scale.desc")
      .setValue(1.5F)
      .range(0.5F, 3.0F);
   public final ValueSetting linkDistance = new ValueSetting("setting.totemparticles.linkdistance.name", "setting.totemparticles.linkdistance.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .visible(() -> this.mode.getSelected().contains("Network"));
   private final List<WorldParticle> particles = new ArrayList<>();
   private final List<WorldParticle> networkParticles = new ArrayList<>();
   private final List<TotemParticles.Emitter> emitters = new ArrayList<>();
   private SpatialGrid3D<WorldParticle> spatialGrid = null;

   public static TotemParticles getInstance() {
      return Instance.get(TotemParticles.class);
   }

   public TotemParticles() {
      super("module.totemparticles.name", ModuleCategory.PARTICLES);
      GroupSetting colorGroup = new GroupSetting("group.totemparticles.colors.name", "group.totemparticles.colors.desc", false)
         .settings(this.colorMode, this.customColorsCount, this.customColors, this.colorAnimation);
      GroupSetting appearanceGroup = new GroupSetting("group.totemparticles.appearance.name", "group.totemparticles.appearance.desc", false)
         .settings(this.mode, this.physics);
      GroupSetting behaviorGroup = new GroupSetting("group.totemparticles.behavior.name", "group.totemparticles.behavior.desc", false)
         .settings(this.particleCount, this.emitterDuration, this.lifeTime, this.speed, this.scale);
      this.setup(new Setting[]{colorGroup, appearanceGroup, behaviorGroup, this.linkDistance});
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

   public void onTotemPop(Entity entity) {
      if (mc.player != null && mc.world != null) {
         this.emitters.add(new TotemParticles.Emitter(entity, System.currentTimeMillis(), (int)this.emitterDuration.getValue()));

         for (int i = 0; i < this.particleCount.getValue(); i++) {
            this.spawnParticle(entity);
         }
      }
   }

   private void spawnParticle(Entity entity) {
      String selectedMode = this.getRandomMode();
      String selectedPhysics = selectedMode.equals("Network") ? "Fly" : this.getRandomPhysics();
      float x = (float)entity.getX();
      float y = (float)(entity.getY() + entity.getHeight() / 2.0F);
      float z = (float)entity.getZ();
      int colorInt = this.getParticleColor();
      ThreadLocalRandom random = ThreadLocalRandom.current();
      WorldParticle particle = new WorldParticle(
         x,
         y,
         z,
         new Color(colorInt),
         (float)(random.nextDouble() * 180.0),
         (float)(random.nextDouble() * 25.0 + 5.0),
         0.0F,
         selectedMode,
         selectedPhysics,
         selectedMode.equals("Glyph") ? ParticleData.getRandomGlyphTexture() : (selectedMode.equals("Things") ? ParticleData.getRandomGlyphAltTexture() : null),
         this.lifeTime.getValue(),
         this.scale.getValue(),
         this.speed.getValue(),
         this
      ) {
         @Override
         protected void initMotion(float speed) {
            double scale = 0.1 * speed;
            ThreadLocalRandom rng = ThreadLocalRandom.current();
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

   private int getParticleColor() {
      if (this.colorMode.isSelected("Sync")) {
         return ColorUtil.getClientColor();
      } else if (this.colorMode.isSelected("Vanilla")) {
         ThreadLocalRandom random = ThreadLocalRandom.current();
         if (random.nextDouble() < 0.25) {
            float r = (float)random.nextDouble(0.6, 0.8);
            float g = (float)random.nextDouble(0.6, 0.9);
            float b = (float)random.nextDouble(0.0, 0.2);
            int rgb = (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
            return 0xFF000000 | rgb;
         } else {
            float r = (float)random.nextDouble(0.1, 0.3);
            float g = (float)random.nextDouble(0.4, 0.7);
            float b = (float)random.nextDouble(0.0, 0.2);
            int rgb = (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
            return 0xFF000000 | rgb;
         }
      } else {
         int[] colors = this.getCustomColors();
         return colors != null && colors.length > 0 ? colors[0] : -1;
      }
   }

   private String getRandomMode() {
      List<String> selected = this.mode.getSelected();
      return selected.isEmpty() ? "Stars" : selected.get(ThreadLocalRandom.current().nextInt(selected.size()));
   }

   private String getRandomPhysics() {
      List<String> selected = this.physics.getSelected();
      return selected.isEmpty() ? "Fall" : selected.get(ThreadLocalRandom.current().nextInt(selected.size()));
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         long currentTime = System.currentTimeMillis();
         this.emitters.removeIf(emitter -> {
            if (emitter.tick(currentTime)) {
               return true;
            } else {
               int perTick = Math.max(1, (int)this.particleCount.getValue() / (int)this.emitterDuration.getValue());

               for (int i = 0; i < perTick; i++) {
                  this.spawnParticle(emitter.entity);
               }

               return false;
            }
         });
         ParticleUpdateExecutor.updateParticlesInPlace(this.particles, currentTime, p -> true, null, particlex -> {
            if ("Network".equals(particlex.getParticleMode())) {
               if (this.spatialGrid != null) {
                  this.spatialGrid.remove(particlex, particlex.getX(), particlex.getY(), particlex.getZ());
               }

               this.networkParticles.remove(particlex);
            }
         });
         if (this.spatialGrid != null && !this.networkParticles.isEmpty()) {
            for (WorldParticle particle : this.networkParticles) {
               this.spatialGrid.update(particle, particle.getPx(), particle.getPy(), particle.getPz(), particle.getX(), particle.getY(), particle.getZ());
            }
         }
      }
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

   private record Emitter(Entity entity, long startTime, int duration) {
      public boolean tick(long currentTime) {
         long durationMs = this.duration * 50L;
         return currentTime - this.startTime > durationMs;
      }
   }
}
