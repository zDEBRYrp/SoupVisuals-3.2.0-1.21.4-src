package padej.soup.implement.features.modules.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
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
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.math.ProjectionUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.particle.ParticleColorUtil;
import padej.soup.base.util.particle.ParticleUpdateExecutor;
import padej.soup.base.util.spatial.SpatialGrid3D;
import padej.soup.implement.features.modules.particles.types.FireFlyParticle;
import padej.soup.implement.features.modules.particles.types.WorldParticle;

public class AmbientParticles extends Module implements IParticleModule {
   private final SelectSetting colorMode = new SelectSetting("setting.ambientparticles.colormode.name", "setting.ambientparticles.colormode.desc")
      .value("Sync", "Custom")
      .selected("Sync");
   private final SelectSetting customColorsCount = new SelectSetting("setting.ambientparticles.colorcount.name", "setting.ambientparticles.colorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final MultiColorSetting customColors = new MultiColorSetting(
         "setting.ambientparticles.gradientcolors.name", "setting.ambientparticles.gradientcolors.desc"
      )
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final SelectSetting colorAnimation = new SelectSetting(
         "setting.ambientparticles.coloranimation.name", "setting.ambientparticles.coloranimation.desc"
      )
      .value("Wave", "Vertex")
      .selected("Wave")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final MultiSelectSetting mode = new MultiSelectSetting("setting.ambientparticles.mode.name", "setting.ambientparticles.mode.desc")
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
         "Firefly",
         "Network",
         "Cube",
         "Pyramid"
      )
      .selected("Stars");
   private final MultiSelectSetting physics = new MultiSelectSetting("setting.ambientparticles.physics.name", "setting.ambientparticles.physics.desc")
      .value("Fall", "Fly", "Emerge")
      .selected("Fly");
   private final ValueSetting regularCount = new ValueSetting("setting.ambientparticles.regularcount.name", "setting.ambientparticles.regularcount.desc")
      .setValue(100.0F)
      .range(20, 500);
   private final ValueSetting regularScale = new ValueSetting("setting.ambientparticles.regularscale.name", "setting.ambientparticles.regularscale.desc")
      .setValue(2.5F)
      .range(0.1F, 15.0F);
   private final ValueSetting fireflyCount = new ValueSetting("setting.ambientparticles.fireflycount.name", "setting.ambientparticles.fireflycount.desc")
      .setValue(30.0F)
      .range(10, 100);
   private final ValueSetting fireflyScale = new ValueSetting("setting.ambientparticles.fireflyscale.name", "setting.ambientparticles.fireflyscale.desc")
      .setValue(1.0F)
      .range(0.3F, 3.0F);
   private final ValueSetting fireflyTrailLength = new ValueSetting(
         "setting.ambientparticles.firefly.traillength.name", "setting.ambientparticles.firefly.traillength.desc"
      )
      .setValue(20.0F)
      .range(5, 60);
   private final ValueSetting geometricsCount = new ValueSetting(
         "setting.ambientparticles.geometricscount.name", "setting.ambientparticles.geometricscount.desc"
      )
      .setValue(50.0F)
      .range(10, 200);
   private final ValueSetting geometricsScale = new ValueSetting(
         "setting.ambientparticles.geometricsscale.name", "setting.ambientparticles.geometricsscale.desc"
      )
      .setValue(2.0F)
      .range(0.5F, 15.0F);
   private final ValueSetting linkDistance = new ValueSetting("setting.ambientparticles.linkdistance.name", "setting.ambientparticles.linkdistance.desc")
      .setValue(5.0F)
      .range(2, 8);
   private final ValueSetting maxLinksPerParticle = new ValueSetting("setting.ambientparticles.maxlinks.name", "setting.ambientparticles.maxlinks.desc")
      .setValue(5.0F)
      .range(1, 10);
   private final ValueSetting spawnRadius = new ValueSetting("setting.ambientparticles.spawnradius.name", "setting.ambientparticles.spawnradius.desc")
      .setValue(25.0F)
      .range(10, 40);
   private final ValueSetting spawnHeight = new ValueSetting("setting.ambientparticles.spawnheight.name", "setting.ambientparticles.spawnheight.desc")
      .setValue(4.0F)
      .range(2, 20);
   private final List<WorldParticle> particles = new ArrayList<>();
   private final List<WorldParticle> networkParticles = new ArrayList<>();
   private final List<WorldParticle> geometricsParticles = new ArrayList<>();
   private final List<FireFlyParticle> fireflyParticles = new ArrayList<>();
   private SpatialGrid3D<WorldParticle> spatialGrid = null;

   public static AmbientParticles getInstance() {
      return Instance.get(AmbientParticles.class);
   }

   public AmbientParticles() {
      super("module.ambientparticles.name", ModuleCategory.PARTICLES);
      GroupSetting colorGroup = new GroupSetting("group.ambientparticles.colors.name", "group.ambientparticles.colors.desc", false)
         .settings(this.colorMode, this.customColorsCount, this.customColors, this.colorAnimation);
      GroupSetting appearanceGroup = new GroupSetting("group.ambientparticles.appearance.name", "group.ambientparticles.appearance.desc", false)
         .settings(this.mode, this.physics);
      GroupSetting regularGroup = new GroupSetting("group.ambientparticles.regular.name", "group.ambientparticles.regular.desc", false)
         .settings(this.regularCount, this.regularScale)
         .visible(
            () -> this.mode
               .getSelected()
               .stream()
               .anyMatch(
                  m -> m.equals("Stars")
                     || m.equals("Hearts")
                     || m.equals("Bloom")
                     || m.equals("Glyph")
                     || m.equals("Things")
                     || m.equals("Blink")
                     || m.equals("Coron")
                     || m.equals("Dollar")
                     || m.equals("Flame")
                     || m.equals("Geometric")
                     || m.equals("Snowflake")
                     || m.equals("Logo")
                     || m.equals("Virus")
                     || m.equals("SoupAPI Old")
                     || m.equals("Sword")
                     || m.equals("Network")
               )
         );
      GroupSetting fireflyGroup = new GroupSetting("group.ambientparticles.firefly.name", "group.ambientparticles.firefly.desc", false)
         .settings(this.fireflyCount, this.fireflyScale, this.fireflyTrailLength)
         .visible(() -> this.mode.getSelected().contains("Firefly"));
      GroupSetting geometricsGroup = new GroupSetting("group.ambientparticles.geometrics.name", "group.ambientparticles.geometrics.desc", false)
         .settings(this.geometricsCount, this.geometricsScale)
         .visible(() -> this.mode.getSelected().contains("Cube") || this.mode.getSelected().contains("Pyramid"));
      GroupSetting networkGroup = new GroupSetting("group.ambientparticles.network.name", "group.ambientparticles.network.desc", false)
         .settings(this.linkDistance, this.maxLinksPerParticle)
         .visible(() -> this.mode.getSelected().contains("Network"));
      GroupSetting spawnGroup = new GroupSetting("group.ambientparticles.spawn.name", "group.ambientparticles.spawn.desc", false)
         .settings(this.spawnRadius, this.spawnHeight);
      this.setup(new Setting[]{colorGroup, appearanceGroup, regularGroup, fireflyGroup, geometricsGroup, networkGroup, spawnGroup});
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

   private String getRandomMode() {
      List<String> selected = this.mode.getSelected();
      int allowedCount = 0;

      for (String particleMode : selected) {
         if (!"Firefly".equals(particleMode) && !"Cube".equals(particleMode) && !"Pyramid".equals(particleMode)) {
            allowedCount++;
         }
      }

      if (allowedCount == 0) {
         return null;
      } else {
         int targetIndex = ThreadLocalRandom.current().nextInt(allowedCount);

         for (String particleModex : selected) {
            if (!"Firefly".equals(particleModex) && !"Cube".equals(particleModex) && !"Pyramid".equals(particleModex) && targetIndex-- == 0) {
               return particleModex;
            }
         }

         return null;
      }
   }

   private String getRandomGeometricMode() {
      List<String> selected = this.mode.getSelected();
      int allowedCount = 0;

      for (String particleMode : selected) {
         if ("Cube".equals(particleMode) || "Pyramid".equals(particleMode)) {
            allowedCount++;
         }
      }

      if (allowedCount == 0) {
         return "Cube";
      } else {
         int targetIndex = ThreadLocalRandom.current().nextInt(allowedCount);

         for (String particleModex : selected) {
            if (("Cube".equals(particleModex) || "Pyramid".equals(particleModex)) && targetIndex-- == 0) {
               return particleModex;
            }
         }

         return "Cube";
      }
   }

   private String getRandomPhysics() {
      List<String> selected = this.physics.getSelected();
      return selected.isEmpty() ? "Fall" : selected.get(ThreadLocalRandom.current().nextInt(selected.size()));
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         long currentTime = System.currentTimeMillis();
         if (this.mode.isSelected("Firefly")) {
            this.fireflyParticles.removeIf(firefly -> firefly.tick(this::isFireflyVisible));
         } else {
            this.fireflyParticles.clear();
         }

         ParticleUpdateExecutor.updateParticlesInPlace(this.particles, currentTime, this::isParticleVisible, null, particle -> {
            if ("Network".equals(particle.getParticleMode())) {
               if (this.spatialGrid != null) {
                  this.spatialGrid.remove(particle, particle.getX(), particle.getY(), particle.getZ());
               }

               this.networkParticles.remove(particle);
            }
         });
         ParticleUpdateExecutor.updateParticlesInPlace(this.geometricsParticles, currentTime, this::isParticleVisible);
         if (this.spatialGrid != null && !this.networkParticles.isEmpty()) {
            for (WorldParticle particle : this.networkParticles) {
               this.spatialGrid.update(particle, particle.getPx(), particle.getPy(), particle.getPz(), particle.getX(), particle.getY(), particle.getZ());
            }
         }

         List<String> selectedModes = this.mode.getSelected();
         List<String> selectedPhysics = this.physics.getSelected();
         if (!selectedModes.isEmpty() && !selectedPhysics.isEmpty()) {
            boolean hasRegularParticles = selectedModes.stream().anyMatch(m -> !m.equals("Firefly") && !m.equals("Cube") && !m.equals("Pyramid"));
            if (hasRegularParticles) {
               int currentRegularCount = this.particles.size();
               int targetRegularCount = (int)this.regularCount.getValue();

               for (int i = currentRegularCount; i < targetRegularCount; i++) {
                  this.spawnParticle();
               }
            } else {
               this.particles.clear();
               this.networkParticles.clear();
               if (this.spatialGrid != null) {
                  this.spatialGrid = null;
               }
            }

            if (!selectedModes.contains("Cube") && !selectedModes.contains("Pyramid")) {
               this.geometricsParticles.clear();
            } else {
               int currentGeometricsCount = this.geometricsParticles.size();
               int targetGeometricsCount = (int)this.geometricsCount.getValue();

               for (int i = currentGeometricsCount; i < targetGeometricsCount; i++) {
                  this.spawnGeometric();
               }
            }

            if (selectedModes.contains("Firefly")) {
               int currentFireflyCount = this.fireflyParticles.size();
               int targetFireflyCount = (int)this.fireflyCount.getValue();

               for (int i = currentFireflyCount; i < targetFireflyCount; i++) {
                  this.spawnFirefly();
               }
            }
         } else {
            this.particles.clear();
            this.geometricsParticles.clear();
            this.fireflyParticles.clear();
            this.networkParticles.clear();
            if (this.spatialGrid != null) {
               this.spatialGrid = null;
            }
         }
      }
   }

   private void spawnFirefly() {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      float radius = this.spawnRadius.getValue();
      float height = this.spawnHeight.getValue();
      float x = (float)(mc.player.getX() + (random.nextDouble() * 2.0 - 1.0) * radius);
      float y = (float)(mc.player.getY() + 2.0 + random.nextDouble() * (height - 2.0F));
      float z = (float)(mc.player.getZ() + (random.nextDouble() * 2.0 - 1.0) * radius);
      int[] customColorsArray = null;
      int colorInt;
      if (this.colorMode.isSelected("Sync")) {
         colorInt = ColorUtil.getClientColor();
      } else {
         customColorsArray = this.getCustomColors();
         colorInt = customColorsArray != null && customColorsArray.length > 0 ? customColorsArray[0] : -256;
      }

      FireFlyParticle firefly = new FireFlyParticle(
         x, y, z, new Color(colorInt), customColorsArray, this.fireflyScale.getValue(), (int)this.fireflyTrailLength.getValue()
      );
      this.fireflyParticles.add(firefly);
   }

   private void spawnGeometric() {
      String selectedMode = this.getRandomGeometricMode();
      String selectedPhysics = this.getRandomPhysics();
      boolean emerge = selectedPhysics.equals("Emerge");
      ThreadLocalRandom random = ThreadLocalRandom.current();
      float radius = this.spawnRadius.getValue();
      float height = this.spawnHeight.getValue();
      float x = (float)(mc.player.getX() + (random.nextDouble() * 2.0 - 1.0) * radius);
      float y;
      if (emerge) {
         y = (float)(mc.player.getY() - 1.0 - random.nextDouble() * 2.0);
      } else {
         y = (float)(mc.player.getY() + 2.0 + random.nextDouble() * (height - 2.0F));
      }

      float z = (float)(mc.player.getZ() + (random.nextDouble() * 2.0 - 1.0) * radius);
      int colorInt;
      if (this.colorMode.isSelected("Sync")) {
         colorInt = ColorUtil.getClientColor();
      } else {
         int[] colors = this.getCustomColors();
         colorInt = colors != null && colors.length > 0 ? colors[0] : -1;
      }

      WorldParticle particle = new WorldParticle(
         x,
         y,
         z,
         new Color(colorInt),
         (float)(random.nextDouble() * 180.0),
         (float)(random.nextDouble() * 5.0 + 5.0),
         0.0F,
         selectedMode,
         selectedPhysics,
         null,
         0.0F,
         this.geometricsScale.getValue(),
         0.0F,
         this
      );
      this.geometricsParticles.add(particle);
   }

   private void spawnParticle() {
      String selectedMode = this.getRandomMode();
      if (selectedMode != null) {
         String selectedPhysics = selectedMode.equals("Network") ? "Fly" : this.getRandomPhysics();
         boolean emerge = selectedPhysics.equals("Emerge");
         ThreadLocalRandom random = ThreadLocalRandom.current();
         float radius = this.spawnRadius.getValue();
         float height = this.spawnHeight.getValue();
         float x = (float)(mc.player.getX() + (random.nextDouble() * 2.0 - 1.0) * radius);
         float y;
         if (emerge) {
            y = (float)(mc.player.getY() - 1.0 - random.nextDouble() * 2.0);
         } else {
            y = (float)(mc.player.getY() + 2.0 + random.nextDouble() * (height - 2.0F));
         }

         float z = (float)(mc.player.getZ() + (random.nextDouble() * 2.0 - 1.0) * radius);
         int colorInt;
         if (this.colorMode.isSelected("Sync")) {
            colorInt = ColorUtil.getClientColor();
         } else {
            int[] colors = this.getCustomColors();
            colorInt = colors != null && colors.length > 0 ? colors[0] : -1;
         }

         WorldParticle particle = new WorldParticle(
            x,
            y,
            z,
            new Color(colorInt),
            (float)(random.nextDouble() * 180.0),
            (float)(random.nextDouble() * 5.0 + 5.0),
            0.0F,
            selectedMode,
            selectedPhysics,
            selectedMode.equals("Glyph")
               ? ParticleData.getRandomGlyphTexture()
               : (selectedMode.equals("Things") ? ParticleData.getRandomGlyphAltTexture() : null),
            0.0F,
            this.regularScale.getValue(),
            0.0F,
            this
         );
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

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      MatrixStack stack = event.getStack();
      if (mc.player != null && mc.world != null) {
         long currentTime = System.currentTimeMillis();
         if (this.mode.isSelected("Firefly") && !this.fireflyParticles.isEmpty()) {
            for (FireFlyParticle fireflyParticle : this.fireflyParticles) {
               if (fireflyParticle.isVisible()) {
                  fireflyParticle.render(stack);
               }
            }
         }

         for (WorldParticle particle : this.particles) {
            if (particle.isVisible()) {
               particle.render(stack, currentTime);
            }
         }

         for (WorldParticle particlex : this.geometricsParticles) {
            if (particlex.isVisible()) {
               particlex.render(stack, currentTime);
            }
         }

         if (this.mode.isSelected("Network") && this.spatialGrid != null && !this.networkParticles.isEmpty()) {
            this.renderNetworkLinks(stack, currentTime);
         }
      }
   }

   private boolean isParticleVisible(WorldParticle particle) {
      float size = particle.getScale() * 0.07F;
      Box particleBox = new Box(
         particle.getX() - size, particle.getY() - size, particle.getZ() - size, particle.getX() + size, particle.getY() + size, particle.getZ() + size
      );
      return ProjectionUtil.canSee(particleBox);
   }

   private boolean isFireflyVisible(FireFlyParticle firefly) {
      float size = this.fireflyScale.getValue() * 0.5F;
      Box particleBox = new Box(
         firefly.getPosX() - size,
         firefly.getPosY() - size,
         firefly.getPosZ() - size,
         firefly.getPosX() + size,
         firefly.getPosY() + size,
         firefly.getPosZ() + size
      );
      return ProjectionUtil.canSee(particleBox);
   }

   private void renderNetworkLinks(MatrixStack matrices, long currentTime) {
      int particleCount = this.networkParticles.size();
      if (particleCount >= 2 && this.spatialGrid != null) {
         float maxLinkDistance = this.linkDistance.getValue();
         float maxLinkDistanceSq = maxLinkDistance * maxLinkDistance;
         int maxLinks = Math.max(1, (int)this.maxLinksPerParticle.getValue());
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         Matrix4f matrix = new Matrix4f(matrices.peek().getPositionMatrix()).setTranslation(0.0F, 0.0F, 0.0F);
         BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
         RenderSystem.enableDepthTest();
         int lineCount = 0;
         IdentityHashMap<WorldParticle, Integer> indexByParticle = new IdentityHashMap<>(particleCount * 2);

         for (int i = 0; i < particleCount; i++) {
            indexByParticle.put(this.networkParticles.get(i), i);
         }

         WorldParticle[] nearestParticles = new WorldParticle[maxLinks];
         float[] nearestDistSq = new float[maxLinks];
         double cameraX = mc.gameRenderer.getCamera().getPos().x;
         double cameraY = mc.gameRenderer.getCamera().getPos().y;
         double cameraZ = mc.gameRenderer.getCamera().getPos().z;

         for (int i = 0; i < particleCount; i++) {
            WorldParticle p1 = this.networkParticles.get(i);
            if (p1.isVisible()) {
               double posX1 = MathUtil.interpolate(p1.getPx(), p1.getX());
               double posY1 = MathUtil.interpolate(p1.getPy(), p1.getY()) + 0.1;
               double posZ1 = MathUtil.interpolate(p1.getPz(), p1.getZ());
               double camDistSq = (posX1 - cameraX) * (posX1 - cameraX) + (posY1 - cameraY) * (posY1 - cameraY) + (posZ1 - cameraZ) * (posZ1 - cameraZ);
               int effectiveMaxLinks;
               if (camDistSq < 100.0) {
                  effectiveMaxLinks = maxLinks;
               } else if (camDistSq < 400.0) {
                  effectiveMaxLinks = Math.max(1, maxLinks / 2);
               } else {
                  effectiveMaxLinks = Math.max(1, maxLinks / 4);
               }

               for (int j = 0; j < effectiveMaxLinks; j++) {
                  nearestParticles[j] = null;
                  nearestDistSq[j] = Float.POSITIVE_INFINITY;
               }

               int[] nearestCount = new int[]{0};
               int currentIndex = i;
               this.spatialGrid.forEachInRadiusWithPositions((float)posX1, (float)posY1, (float)posZ1, maxLinkDistance, entry -> {
                  WorldParticle p2x = entry.getObject();
                  if (p2x != p1 && p2x.isVisible()) {
                     Integer p2Index = indexByParticle.get(p2x);
                     if (p2Index != null && p2Index > currentIndex) {
                        float dxx = entry.getX() - (float)posX1;
                        float dyx = entry.getY() - (float)posY1;
                        float dzx = entry.getZ() - (float)posZ1;
                        float distSqx = dxx * dxx + dyx * dyx + dzx * dzx;
                        if (!(distSqx > maxLinkDistanceSq) && !(distSqx <= 1.0E-4F)) {
                           if (nearestCount[0] < effectiveMaxLinks) {
                              nearestParticles[nearestCount[0]] = p2x;
                              nearestDistSq[nearestCount[0]] = distSqx;
                              nearestCount[0]++;
                           } else {
                              int worstIndex = 0;
                              float worstDist = nearestDistSq[0];

                              for (int k = 1; k < effectiveMaxLinks; k++) {
                                 if (nearestDistSq[k] > worstDist) {
                                    worstDist = nearestDistSq[k];
                                    worstIndex = k;
                                 }
                              }

                              if (distSqx < worstDist) {
                                 nearestParticles[worstIndex] = p2x;
                                 nearestDistSq[worstIndex] = distSqx;
                              }
                           }
                        }
                     }
                  }
               });

               for (int j = 0; j < nearestCount[0]; j++) {
                  WorldParticle p2 = nearestParticles[j];
                  if (p2 != null) {
                     double posX2 = MathUtil.interpolate(p2.getPx(), p2.getX());
                     double posY2 = MathUtil.interpolate(p2.getPy(), p2.getY()) + 0.1;
                     double posZ2 = MathUtil.interpolate(p2.getPz(), p2.getZ());
                     double dx = posX2 - posX1;
                     double dy = posY2 - posY1;
                     double dz = posZ2 - posZ1;
                     double distSq = dx * dx + dy * dy + dz * dz;
                     if (!(distSq > maxLinkDistanceSq) && !(distSq <= 1.0E-4)) {
                        double dist = Math.sqrt(distSq);
                        float lineAlpha = (float)(1.0 - dist / maxLinkDistance);
                        float particleAlpha1 = p1.getAlpha();
                        float particleAlpha2 = p2.getAlpha();
                        float alpha = Math.min(lineAlpha, Math.min(particleAlpha1, particleAlpha2));
                        if (camDistSq > 100.0) {
                           float distFade = Math.max(0.1F, 1.0F - (float)(camDistSq - 100.0) / 300.0F);
                           alpha *= distFade;
                        }

                        int color2;
                        int color1;
                        if (this.colorMode.isSelected("Sync")) {
                           int baseColor = ColorUtil.getClientColor();
                           color1 = ColorUtil.multAlpha(baseColor, alpha);
                           color2 = color1;
                        } else {
                           int[] colors = this.getCustomColors();
                           if (colors == null || colors.length <= 0) {
                              color1 = ColorUtil.multAlpha(-1, alpha);
                              color2 = color1;
                           } else if (this.colorAnimation.isSelected("Vertex")) {
                              int offset1 = System.identityHashCode(p1) % 360;
                              int offset2 = System.identityHashCode(p2) % 360;
                              color1 = ParticleColorUtil.getVertexGradientColor(offset1, colors, alpha);
                              color2 = ParticleColorUtil.getVertexGradientColor(offset2, colors, alpha);
                           } else {
                              color1 = ParticleColorUtil.getWaveColor(colors, alpha, p1.getColorOffset());
                              color2 = ParticleColorUtil.getWaveColor(colors, alpha, p2.getColorOffset());
                           }
                        }

                        float localPosX1 = (float)(posX1 - cameraX);
                        float localPosY1 = (float)(posY1 - cameraY);
                        float localPosZ1 = (float)(posZ1 - cameraZ);
                        float localPosX2 = (float)(posX2 - cameraX);
                        float localPosY2 = (float)(posY2 - cameraY);
                        float localPosZ2 = (float)(posZ2 - cameraZ);
                        bufferBuilder.vertex(matrix, localPosX1, localPosY1, localPosZ1)
                           .color((color1 >> 16 & 0xFF) / 255.0F, (color1 >> 8 & 0xFF) / 255.0F, (color1 & 0xFF) / 255.0F, (color1 >> 24 & 0xFF) / 255.0F);
                        bufferBuilder.vertex(matrix, localPosX2, localPosY2, localPosZ2)
                           .color((color2 >> 16 & 0xFF) / 255.0F, (color2 >> 8 & 0xFF) / 255.0F, (color2 & 0xFF) / 255.0F, (color2 >> 24 & 0xFF) / 255.0F);
                        lineCount++;
                     }
                  }
               }
            }
         }

         if (lineCount > 0) {
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         }

         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
         RenderSystem.disableDepthTest();
      }
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.particles.clear();
      this.networkParticles.clear();
      this.geometricsParticles.clear();
      this.fireflyParticles.clear();
      if (this.spatialGrid != null) {
         this.spatialGrid = null;
      }
   }

   public SelectSetting getCustomColorsCount() {
      return this.customColorsCount;
   }

   public MultiSelectSetting getMode() {
      return this.mode;
   }

   public MultiSelectSetting getPhysics() {
      return this.physics;
   }

   public ValueSetting getRegularCount() {
      return this.regularCount;
   }

   public ValueSetting getRegularScale() {
      return this.regularScale;
   }

   public ValueSetting getFireflyCount() {
      return this.fireflyCount;
   }

   public ValueSetting getFireflyScale() {
      return this.fireflyScale;
   }

   public ValueSetting getFireflyTrailLength() {
      return this.fireflyTrailLength;
   }

   public ValueSetting getGeometricsCount() {
      return this.geometricsCount;
   }

   public ValueSetting getGeometricsScale() {
      return this.geometricsScale;
   }

   public ValueSetting getLinkDistance() {
      return this.linkDistance;
   }

   public ValueSetting getMaxLinksPerParticle() {
      return this.maxLinksPerParticle;
   }

   public ValueSetting getSpawnRadius() {
      return this.spawnRadius;
   }

   public ValueSetting getSpawnHeight() {
      return this.spawnHeight;
   }

   public List<WorldParticle> getParticles() {
      return this.particles;
   }

   public List<WorldParticle> getNetworkParticles() {
      return this.networkParticles;
   }

   public List<WorldParticle> getGeometricsParticles() {
      return this.geometricsParticles;
   }

   public List<FireFlyParticle> getFireflyParticles() {
      return this.fireflyParticles;
   }

   public SpatialGrid3D<WorldParticle> getSpatialGrid() {
      return this.spatialGrid;
   }
}
