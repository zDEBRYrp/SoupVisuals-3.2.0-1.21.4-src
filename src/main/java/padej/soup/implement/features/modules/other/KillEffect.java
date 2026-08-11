package padej.soup.implement.features.modules.other;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.entity.EntityDamageEvent;
import padej.soup.api.event.events.entity.EntityDeathEvent;
import padej.soup.api.event.events.player.EventAttack;
import padej.soup.api.event.events.player.TickEvent;
import padej.soup.api.event.events.render.WorldRenderEvent;
import padej.soup.api.feature.module.IParticleModule;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.api.repository.friend.FriendUtils;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.particle.ParticleUpdateExecutor;
import padej.soup.base.util.render.CircleExplosionEffect;
import padej.soup.base.util.render.CrystalExplosionEffect;
import padej.soup.base.util.render.GhostExplosionEffect;
import padej.soup.base.util.render.TargetESPCrystals;
import padej.soup.base.util.render.TargetRenderer;
import padej.soup.base.util.spatial.SpatialGrid3D;
import padej.soup.implement.features.modules.particles.ParticleData;
import padej.soup.implement.features.modules.particles.render.NetworkRenderer;
import padej.soup.implement.features.modules.particles.types.WorldParticle;
import padej.soup.implement.features.modules.visuals.TargetRender;

public class KillEffect extends Module implements IParticleModule {
   private final Map<Integer, KillEffect.AttackRecord> recentlyAttacked = new ConcurrentHashMap<>();
   private final Map<Integer, KillEffect.AttackRecord> recentlyAttackedByFriends = new ConcurrentHashMap<>();
   private final Map<Integer, KillEffect.PendingKill> pendingKills = new ConcurrentHashMap<>();
   private final Map<Integer, Long> firedKills = new ConcurrentHashMap<>();
   private static final long ATTACK_TIMEOUT_MS = 5000L;
   private static final int PENDING_DELAY_TICKS = 2;
   private static final long DEDUP_COOLDOWN_MS = 2000L;
   private static final double TELEPORT_DIST_SQ = 64.0;
   private Vec3d playerLastTickPos = null;
   private final List<WorldParticle> particles = new ArrayList<>();
   private final List<WorldParticle> networkParticles = new ArrayList<>();
   private SpatialGrid3D<WorldParticle> spatialGrid = null;
   private final SelectSetting effectType = new SelectSetting("setting.killeffect.effecttype.name", "setting.killeffect.effecttype.desc")
      .value("Particles", "Thunder", "TR Sync")
      .selected("Particles");
   private final SelectSetting friendEffectType = new SelectSetting("setting.killeffect.friendeffecttype.name", "setting.killeffect.friendeffecttype.desc")
      .value("Particles", "Thunder", "TR Sync")
      .selected("Thunder");
   private final BooleanSetting showForFriends = new BooleanSetting("setting.killeffect.showforfriends.name", "setting.killeffect.showforfriends.desc")
      .setValue(true);
   private final SelectSetting targetType = new SelectSetting("setting.killeffect.targettype.name", "setting.killeffect.targettype.desc")
      .value("Mobs", "Player", "Both")
      .selected("Both");
   public final SelectSetting colorMode = new SelectSetting("setting.killeffect.colormode.name", "setting.killeffect.colormode.desc")
      .value("Sync", "Custom")
      .selected("Sync")
      .visible(this::isParticleMode);
   private final SelectSetting customColorsCount = new SelectSetting("setting.killeffect.colorcount.name", "setting.killeffect.colorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.colorMode.isSelected("Custom") && this.isParticleMode());
   private final MultiColorSetting customColors = new MultiColorSetting("setting.killeffect.gradientcolors.name", "setting.killeffect.gradientcolors.desc")
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.colorMode.isSelected("Custom") && this.isParticleMode());
   public final SelectSetting colorAnimation = new SelectSetting("setting.killeffect.coloranimation.name", "setting.killeffect.coloranimation.desc")
      .value("Wave", "Vertex")
      .selected("Wave")
      .visible(() -> this.colorMode.isSelected("Custom") && this.isParticleMode());
   private final MultiSelectSetting mode = new MultiSelectSetting("setting.killeffect.mode.name", "setting.killeffect.mode.desc")
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
      .selected("Stars", "Hearts", "Bloom")
      .visible(this::isParticleMode);
   private final MultiSelectSetting physics = new MultiSelectSetting("setting.killeffect.physics.name", "setting.killeffect.physics.desc")
      .value("Fall", "Fly", "Emerge")
      .selected("Emerge")
      .visible(this::isParticleMode);
   private final ValueSetting scale = new ValueSetting("setting.killeffect.scale.name", "setting.killeffect.scale.desc")
      .setValue(1.5F)
      .range(0.5F, 5.0F)
      .visible(this::isParticleMode);
   private final ValueSetting lifeTime = new ValueSetting("setting.killeffect.lifetime.name", "setting.killeffect.lifetime.desc")
      .setValue(3.0F)
      .range(1, 10)
      .visible(this::isParticleMode);
   private final ValueSetting speed = new ValueSetting("setting.killeffect.speed.name", "setting.killeffect.speed.desc")
      .setValue(1.0F)
      .range(0.1F, 3.0F)
      .visible(this::isParticleMode);
   private final ValueSetting amount = new ValueSetting("setting.killeffect.amount.name", "setting.killeffect.amount.desc")
      .setValue(30.0F)
      .range(10, 70)
      .visible(this::isParticleMode);
   public final ValueSetting linkDistance = new ValueSetting("setting.killeffect.linkdistance.name", "setting.killeffect.linkdistance.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .visible(() -> this.isParticleMode() && this.mode.getSelected().contains("Network"));
   private final BooleanSetting enableSound = new BooleanSetting("setting.killeffect.enablesound.name", "setting.killeffect.enablesound.desc").setValue(false);
   private final SelectSetting soundType = new SelectSetting("setting.killeffect.soundtype.name", "setting.killeffect.soundtype.desc")
      .value("Abmiss", "Critow", "Final Blink", "Final Pok", "Neptune", "Rust Headshot", "Whii")
      .selected("Neptune")
      .visible(this.enableSound::isValue);
   private final ValueSetting soundVolume = new ValueSetting("setting.killeffect.soundvolume.name", "setting.killeffect.soundvolume.desc")
      .setValue(1.0F)
      .range(0.1F, 2.0F)
      .visible(this.enableSound::isValue);
   private final ValueSetting soundPitch = new ValueSetting("setting.killeffect.soundpitch.name", "setting.killeffect.soundpitch.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .visible(this.enableSound::isValue);
   private final ValueSetting trsLifetime = new ValueSetting("setting.killeffect.trslifetime.name", "setting.killeffect.trslifetime.desc")
      .setValue(1.8F)
      .range(0.5F, 5.0F)
      .visible(this::isTargetRenderSyncMode);
   private final ValueSetting trsExtraSpeed = new ValueSetting("setting.killeffect.trsextraspeed.name", "setting.killeffect.trsextraspeed.desc")
      .setValue(1.0F)
      .range(0.1F, 3.0F)
      .visible(this::isTargetRenderSyncMode);
   private final BooleanSetting trsDisableRedEffect = new BooleanSetting("setting.killeffect.trsdisablered.name", "setting.killeffect.trsdisablered.desc")
      .setValue(false)
      .visible(this::isTargetRenderSyncMode);

   private boolean isParticleMode() {
      return this.effectType.isSelected("Particles") || this.showForFriends.isValue() && this.friendEffectType.isSelected("Particles");
   }

   private boolean isTargetRenderSyncMode() {
      return this.effectType.isSelected("TR Sync") || this.showForFriends.isValue() && this.friendEffectType.isSelected("TR Sync");
   }

   public static KillEffect getInstance() {
      return Instance.get(KillEffect.class);
   }

   public KillEffect() {
      super("module.killeffect.name", ModuleCategory.OTHER);
      GroupSetting effectGroup = new GroupSetting("group.killeffect.effect.name", "group.killeffect.effect.desc", false)
         .settings(this.effectType, this.friendEffectType, this.showForFriends, this.targetType);
      GroupSetting colorGroup = new GroupSetting("group.killeffect.colors.name", "group.killeffect.colors.desc", false)
         .settings(this.colorMode, this.customColorsCount, this.customColors, this.colorAnimation)
         .visible(this::isParticleMode);
      GroupSetting appearanceGroup = new GroupSetting("group.killeffect.appearance.name", "group.killeffect.appearance.desc", false)
         .settings(this.mode, this.physics, this.scale)
         .visible(this::isParticleMode);
      GroupSetting behaviorGroup = new GroupSetting("group.killeffect.behavior.name", "group.killeffect.behavior.desc", false)
         .settings(this.lifeTime, this.speed, this.amount)
         .visible(this::isParticleMode);
      GroupSetting trsGroup = new GroupSetting("group.killeffect.trs.name", "group.killeffect.trs.desc", false)
         .settings(this.trsLifetime, this.trsExtraSpeed, this.trsDisableRedEffect)
         .visible(this::isTargetRenderSyncMode);
      GroupSetting soundGroup = new GroupSetting("group.killeffect.sound.name", "group.killeffect.sound.desc", false)
         .settings(this.enableSound, this.soundType, this.soundVolume, this.soundPitch);
      this.setup(new Setting[]{effectGroup, colorGroup, appearanceGroup, behaviorGroup, this.linkDistance, trsGroup, soundGroup});
   }

   @EventHandler
   public void onAttack(EventAttack event) {
      if (mc.player != null && mc.world != null) {
         if (!event.isPre()) {
            Entity target = event.getTarget();
            if (target instanceof LivingEntity living) {
               int entityId = target.getId();
               if (!this.firedKills.containsKey(entityId)) {
                  this.recentlyAttacked
                     .computeIfAbsent(
                        entityId,
                        id -> {
                           LoggerUtil.info(
                              "[KillEffect] onAttack: recorded entity {} ({}), hp={}, pos={}",
                              entityId,
                              target.getName().getString(),
                              living.getHealth(),
                              living.getPos()
                           );
                           return new KillEffect.AttackRecord(System.currentTimeMillis(), living.getPos(), living.getHealth());
                        }
                     );
               }
            }
         }
      }
   }

   @EventHandler
   public void onServerDamageConfirm(EntityDamageEvent event) {
      if (mc.player != null && mc.world != null) {
         int playerId = mc.player.getId();
         int entityId = event.entityId();
         KillEffect.AttackRecord ourRecord = this.recentlyAttacked.get(entityId);
         if (ourRecord != null && !ourRecord.damageConfirmed) {
            ourRecord.damageConfirmed = true;
            LoggerUtil.info("[KillEffect] onServerDamage: CONFIRMED (by correlation) entity {}", entityId);
         }

         KillEffect.AttackRecord friendRecord = this.recentlyAttackedByFriends.get(entityId);
         if (friendRecord != null && !friendRecord.damageConfirmed) {
            friendRecord.damageConfirmed = true;
         }

         if ((event.sourceCauseId() == playerId || event.sourceDirectId() == playerId) && ourRecord == null && !this.firedKills.containsKey(entityId)) {
            Entity entity = mc.world.getEntityById(entityId);
            if (entity instanceof LivingEntity living && entity != mc.player) {
               KillEffect.AttackRecord newRecord = new KillEffect.AttackRecord(System.currentTimeMillis(), living.getPos(), living.getHealth());
               newRecord.damageConfirmed = true;
               this.recentlyAttacked.put(entityId, newRecord);
            }
         }

         if (event.sourceCauseId() > 0 && friendRecord == null && !this.firedKills.containsKey(entityId)) {
            Entity causeEntity = mc.world.getEntityById(event.sourceCauseId());
            if (causeEntity instanceof PlayerEntity && FriendUtils.isFriend(causeEntity)) {
               Entity entity = mc.world.getEntityById(entityId);
               if (entity instanceof LivingEntity living && entity != mc.player) {
                  KillEffect.AttackRecord newRecord = new KillEffect.AttackRecord(System.currentTimeMillis(), living.getPos(), living.getHealth());
                  newRecord.damageConfirmed = true;
                  this.recentlyAttackedByFriends.put(entityId, newRecord);
               }
            }
         }
      }
   }

   @EventHandler
   public void onEntityDeath(EntityDeathEvent event) {
      if (mc.player != null && mc.world != null) {
         LivingEntity entity = event.entity();
         if (entity == mc.player) {
            this.recentlyAttacked.clear();
            this.recentlyAttackedByFriends.clear();
            this.pendingKills.clear();
         } else {
            int entityId = entity.getId();
            if (this.recentlyAttacked.containsKey(entityId)) {
               this.fireKillEffect(entity, false);
            } else if (this.recentlyAttackedByFriends.containsKey(entityId)) {
               this.fireKillEffect(entity, true);
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         if (!mc.player.isAlive() || mc.player.isDead() || mc.player.getHealth() <= 0.0F) {
            if (!this.recentlyAttacked.isEmpty() || !this.recentlyAttackedByFriends.isEmpty() || !this.pendingKills.isEmpty()) {
               this.recentlyAttacked.clear();
               this.recentlyAttackedByFriends.clear();
               this.pendingKills.clear();
            }

            this.playerLastTickPos = null;
         }

         Vec3d playerPos = mc.player.getPos();
         if (this.playerLastTickPos != null) {
            double playerDistSq = playerPos.squaredDistanceTo(this.playerLastTickPos);
            if (playerDistSq > 64.0) {
               LoggerUtil.info("[KillEffect] Player teleported (distSq={}), clearing all records", playerDistSq);
               this.recentlyAttacked.clear();
               this.recentlyAttackedByFriends.clear();
               this.pendingKills.clear();
            }
         }

         this.playerLastTickPos = playerPos;
         long currentTime = System.currentTimeMillis();
         CrystalExplosionEffect.tick();
         ParticleUpdateExecutor.updateParticlesInPlace(this.particles, currentTime, p -> true, null, particle -> {
            if ("Network".equals(particle.getParticleMode())) {
               if (this.spatialGrid != null) {
                  this.spatialGrid.remove(particle, particle.getX(), particle.getY(), particle.getZ());
               }

               this.networkParticles.remove(particle);
            }
         });
         if (this.spatialGrid != null && !this.networkParticles.isEmpty()) {
            this.networkParticles
               .forEach(
                  particle -> this.spatialGrid
                     .update(particle, particle.getPx(), particle.getPy(), particle.getPz(), particle.getX(), particle.getY(), particle.getZ())
               );
         }

         this.recentlyAttacked.entrySet().removeIf(entryx -> currentTime - ((KillEffect.AttackRecord)entryx.getValue()).timestamp > 5000L);
         this.recentlyAttackedByFriends.entrySet().removeIf(entryx -> currentTime - ((KillEffect.AttackRecord)entryx.getValue()).timestamp > 5000L);
         this.firedKills.entrySet().removeIf(entryx -> currentTime - (Long)entryx.getValue() > 2000L);

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && entity != mc.player) {
               int entityId = entity.getId();
               KillEffect.AttackRecord ourRecord = this.recentlyAttacked.get(entityId);
               KillEffect.AttackRecord friendRecord = this.recentlyAttackedByFriends.get(entityId);
               if (!entity.isAlive() || living.getHealth() <= 0.0F) {
                  if (ourRecord != null) {
                     this.fireKillEffect(living, false);
                     continue;
                  }

                  if (friendRecord != null) {
                     this.fireKillEffect(living, true);
                     continue;
                  }
               }

               if (ourRecord != null && ourRecord.damageConfirmed && ourRecord.lastKnownPos != null && !this.firedKills.containsKey(entityId)) {
                  double distSq = living.getPos().squaredDistanceTo(ourRecord.lastKnownPos);
                  if (distSq > 64.0 && living.getHealth() > ourRecord.lastKnownHealth) {
                     LoggerUtil.info("[KillEffect] T4 FIRED entity {}: distSq={}, hp {} -> {}", entityId, distSq, ourRecord.lastKnownHealth, living.getHealth());
                     this.firedKills.put(entityId, System.currentTimeMillis());
                     this.recentlyAttacked.remove(entityId);
                     this.pendingKills.remove(entityId);
                     this.fireKillEffectAtPosition(ourRecord.lastKnownPos.add(0.0, living.getHeight() / 2.0, 0.0), false);
                     continue;
                  }
               }

               if (friendRecord != null && friendRecord.damageConfirmed && friendRecord.lastKnownPos != null && !this.firedKills.containsKey(entityId)) {
                  double distSq = living.getPos().squaredDistanceTo(friendRecord.lastKnownPos);
                  if (distSq > 64.0 && living.getHealth() > friendRecord.lastKnownHealth) {
                     LoggerUtil.info(
                        "[KillEffect] T4 FIRED (friend) entity {}: distSq={}, hp {} -> {}", entityId, distSq, friendRecord.lastKnownHealth, living.getHealth()
                     );
                     this.firedKills.put(entityId, System.currentTimeMillis());
                     this.recentlyAttackedByFriends.remove(entityId);
                     this.pendingKills.remove(entityId);
                     this.fireKillEffectAtPosition(friendRecord.lastKnownPos.add(0.0, living.getHeight() / 2.0, 0.0), true);
                     continue;
                  }
               }

               if (living.hurtTime > 0) {
                  if (ourRecord != null && !ourRecord.damageConfirmed) {
                     ourRecord.damageConfirmed = true;
                  }

                  if (friendRecord != null && !friendRecord.damageConfirmed) {
                     friendRecord.damageConfirmed = true;
                  }
               }

               if (ourRecord != null) {
                  ourRecord.lastKnownHealth = living.getHealth();
                  ourRecord.lastKnownPos = living.getPos();
               }

               if (friendRecord != null) {
                  friendRecord.lastKnownHealth = living.getHealth();
                  friendRecord.lastKnownPos = living.getPos();
               }

               this.pendingKills.remove(entityId);
            }
         }

         this.addVanishedToPending(this.recentlyAttacked, currentTime, false);
         this.addVanishedToPending(this.recentlyAttackedByFriends, currentTime, true);
         Iterator<Entry<Integer, KillEffect.PendingKill>> pendingIt = this.pendingKills.entrySet().iterator();

         while (pendingIt.hasNext()) {
            Entry<Integer, KillEffect.PendingKill> entry = pendingIt.next();
            int entityIdx = entry.getKey();
            KillEffect.PendingKill pending = entry.getValue();
            long elapsedMs = currentTime - pending.removedTime;
            if (elapsedMs >= 100L) {
               if (!this.isEntityInWorld(entityIdx)) {
                  this.fireKillEffectAtPosition(pending.lastPosition, pending.killedByFriend);
                  this.firedKills.put(entityIdx, currentTime);
                  this.recentlyAttacked.remove(entityIdx);
                  this.recentlyAttackedByFriends.remove(entityIdx);
               }

               pendingIt.remove();
            }
         }
      }
   }

   private void addVanishedToPending(Map<Integer, KillEffect.AttackRecord> attackMap, long currentTime, boolean byFriend) {
      for (Entry<Integer, KillEffect.AttackRecord> entry : attackMap.entrySet()) {
         int entityId = entry.getKey();
         KillEffect.AttackRecord record = entry.getValue();
         boolean inWorld = this.isEntityInWorld(entityId);
         boolean hasPending = this.pendingKills.containsKey(entityId);
         boolean hasFired = this.firedKills.containsKey(entityId);
         if (!record.damageConfirmed) {
            if (!inWorld) {
               LoggerUtil.info("[KillEffect] T3 skip entity {}: damageConfirmed=false, inWorld={}", entityId, inWorld);
            }
         } else if (!inWorld && !hasPending && !hasFired && record.lastKnownPos != null) {
            LoggerUtil.info("[KillEffect] T3 pending added: entity {}, pos={}, byFriend={}", entityId, record.lastKnownPos, byFriend);
            this.pendingKills.put(entityId, new KillEffect.PendingKill(record.lastKnownPos, currentTime, byFriend));
         }
      }
   }

   private boolean isEntityInWorld(int entityId) {
      return mc.world == null ? false : mc.world.getEntityById(entityId) != null;
   }

   private void fireKillEffect(LivingEntity entity, boolean killedByFriend) {
      int entityId = entity.getId();
      if (!this.firedKills.containsKey(entityId)) {
         this.firedKills.put(entityId, System.currentTimeMillis());
         this.recentlyAttacked.remove(entityId);
         this.recentlyAttackedByFriends.remove(entityId);
         this.pendingKills.remove(entityId);
         boolean isPlayer = entity instanceof PlayerEntity;
         String targetTypeSelected = this.targetType.getSelected();
         if (!targetTypeSelected.equals("Player") || isPlayer) {
            if (!targetTypeSelected.equals("Mobs") || !isPlayer) {
               if (!killedByFriend || this.showForFriends.isValue()) {
                  String selectedEffectType = killedByFriend ? this.friendEffectType.getSelected() : this.effectType.getSelected();
                  Vec3d position = entity.getPos().add(0.0, entity.getHeight() / 2.0, 0.0);
                  switch (selectedEffectType) {
                     case "Particles":
                        this.spawnParticlesEffect(position);
                        break;
                     case "Thunder":
                        this.spawnThunderEffect(position);
                        break;
                     case "TR Sync":
                        this.spawnTargetRenderSyncEffect(entity);
                  }

                  if (this.enableSound.isValue()) {
                     this.playKillSound();
                  }
               }
            }
         }
      }
   }

   private void fireKillEffectAtPosition(Vec3d position, boolean killedByFriend) {
      if (!killedByFriend || this.showForFriends.isValue()) {
         String selectedEffectType = killedByFriend ? this.friendEffectType.getSelected() : this.effectType.getSelected();
         switch (selectedEffectType) {
            case "Particles":
               this.spawnParticlesEffect(position);
               break;
            case "Thunder":
               this.spawnThunderEffect(position);
               break;
            case "TR Sync":
               this.spawnTargetRenderSyncEffectAtPosition(position);
         }

         if (this.enableSound.isValue()) {
            this.playKillSound();
         }
      }
   }

   private void playKillSound() {
      String var2 = this.soundType.getSelected();

      SoundEvent sound = switch (var2) {
         case "Abmiss" -> SoundManager.KILL_ABMISS;
         case "Critow" -> SoundManager.KILL_CRITOW;
         case "Final Blink" -> SoundManager.KILL_FINAL_BLINK;
         case "Final Pok" -> SoundManager.KILL_FINAL_POK;
         case "Neptune" -> SoundManager.KILL_NEPTUNE;
         case "Rust Headshot" -> SoundManager.KILL_RUST_HEADSHOT;
         case "Whii" -> SoundManager.KILL_WHII;
         default -> SoundManager.KILL_NEPTUNE;
      };
      SoundManager.playSound(sound, this.soundVolume.getValue(), this.soundPitch.getValue());
   }

   private void spawnParticlesEffect(Vec3d position) {
      int colorInt;
      if (this.colorMode.isSelected("Sync")) {
         colorInt = ColorUtil.getClientColor();
      } else {
         int[] colors = this.getCustomColors();
         colorInt = colors != null && colors.length > 0 ? colors[0] : -1;
      }

      for (int i = 0; i < this.amount.getValue(); i++) {
         String selectedMode = this.getRandomMode();
         String selectedPhysics = selectedMode.equals("Network") ? "Fly" : this.getRandomPhysics();
         float x = (float)position.x;
         float y = (float)position.y;
         float z = (float)position.z;
         ThreadLocalRandom random = ThreadLocalRandom.current();
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

   private void spawnTargetRenderSyncEffect(LivingEntity entity) {
      TargetRender targetRender = TargetRender.getInstance();
      if (targetRender != null && targetRender.isEnabled()) {
         String renderType = targetRender.getTargetRenderType().getSelected();
         float red = 0.0F;
         if (!this.trsDisableRedEffect.isValue() && !targetRender.isRedEffectDisabled()) {
            float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
            red = MathHelper.clamp((entity.hurtTime - tickDelta) / 10.0F, 0.0F, 1.0F);
         }

         switch (renderType) {
            case "Circle":
               Vec3d circlePos = TargetRenderer.getLastCircleEntityPos();
               if (circlePos == null) {
                  circlePos = entity.getPos();
               }

               CircleExplosionEffect.spawn(
                  circlePos,
                  TargetRenderer.getLastCircleWidth() > 0.0F ? TargetRenderer.getLastCircleWidth() : entity.getWidth(),
                  TargetRenderer.getLastCircleHeight() > 0.0F ? TargetRenderer.getLastCircleHeight() : entity.getHeight(),
                  TargetRenderer.getLastCircleCircleStep(),
                  red,
                  targetRender.getCustomColors(),
                  (long)(this.trsLifetime.getValue() * 1000.0F),
                  this.trsExtraSpeed.getValue()
               );
               targetRender.removeRenderSlot(entity.getId());
               break;
            case "Crystals":
               TargetESPCrystals.instance
                  .startExplosion(
                     targetRender.getSpeedModifier(),
                     red,
                     targetRender.isCrystalsHorizontal(),
                     targetRender.getCustomColors(),
                     targetRender.getCrystalsSize(),
                     targetRender.isCrystalsGlow(),
                     targetRender.getCrystalsGlowSize(),
                     (long)(this.trsLifetime.getValue() * 1000.0F),
                     this.trsExtraSpeed.getValue()
                  );
               targetRender.removeRenderSlot(entity.getId());
               break;
            case "Ghosts":
               GhostExplosionEffect.spawn(
                  entity,
                  targetRender.getSpeedModifier(),
                  targetRender.getGhostsLength(),
                  targetRender.getGhostsRadiusModifier(),
                  targetRender.getGhostsHeadSize(),
                  targetRender.getGhostsTailSize(),
                  targetRender.getGhostsSubdivision(),
                  (int)targetRender.getGhostsCount().getValue(),
                  targetRender.getGhostsTrajectory().getSelected(),
                  targetRender.getGhostTexture(),
                  targetRender.getGhostsBlend().getSelected(),
                  targetRender.getCustomColors(),
                  red,
                  targetRender.getSpeedModifier(),
                  (long)(this.trsLifetime.getValue() * 1000.0F),
                  this.trsExtraSpeed.getValue()
               );
               targetRender.removeRenderSlot(entity.getId());
               break;
            case null:
            default:
               this.spawnParticlesEffect(entity.getPos().add(0.0, entity.getHeight() / 2.0, 0.0));
         }
      } else {
         this.spawnParticlesEffect(entity.getPos().add(0.0, entity.getHeight() / 2.0, 0.0));
      }
   }

   private void spawnTargetRenderSyncEffectAtPosition(Vec3d position) {
      TargetRender targetRender = TargetRender.getInstance();
      if (targetRender != null && targetRender.isEnabled()) {
         String renderType = targetRender.getTargetRenderType().getSelected();
         if ("Circle".equals(renderType)) {
            float red = 0.0F;
            if (!this.trsDisableRedEffect.isValue() && !targetRender.isRedEffectDisabled()) {
               red = 0.0F;
            }

            CircleExplosionEffect.spawn(
               position.add(0.0, -0.9, 0.0),
               0.6F,
               1.8F,
               TargetRenderer.getLastCircleCircleStep(),
               red,
               targetRender.getCustomColors(),
               (long)(this.trsLifetime.getValue() * 1000.0F),
               this.trsExtraSpeed.getValue()
            );
         } else if ("Crystals".equals(renderType)) {
            int[] customColors = targetRender.getCustomColors();
            int baseColor;
            if (customColors != null && customColors.length > 0) {
               baseColor = customColors[0];
            } else {
               baseColor = ColorUtil.getClientColor();
            }

            CrystalExplosionEffect.spawnComputed(
               position.add(0.0, -0.9, 0.0),
               1.8F,
               0.6F,
               targetRender.getCrystalsDistance(),
               targetRender.getCrystalsSize(),
               targetRender.isCrystalsGlow(),
               targetRender.getCrystalsGlowSize(),
               baseColor,
               customColors,
               targetRender.getSpeedModifier(),
               (long)(this.trsLifetime.getValue() * 1000.0F),
               this.trsExtraSpeed.getValue()
            );
         } else if ("Ghosts".equals(renderType)) {
            GhostExplosionEffect.spawn(
               position,
               targetRender.getSpeedModifier(),
               targetRender.getGhostsLength(),
               targetRender.getGhostsRadiusModifier(),
               targetRender.getGhostsHeadSize(),
               targetRender.getGhostsTailSize(),
               targetRender.getGhostsSubdivision(),
               (int)targetRender.getGhostsCount().getValue(),
               targetRender.getGhostsTrajectory().getSelected(),
               targetRender.getGhostTexture(),
               targetRender.getGhostsBlend().getSelected(),
               targetRender.getCustomColors(),
               0.0F,
               targetRender.getSpeedModifier(),
               (long)(this.trsLifetime.getValue() * 1000.0F),
               this.trsExtraSpeed.getValue()
            );
         } else {
            this.spawnParticlesEffect(position);
         }
      } else {
         this.spawnParticlesEffect(position);
      }
   }

   private void spawnThunderEffect(Vec3d position) {
      if (mc.world != null) {
         LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
         lightning.refreshPositionAfterTeleport(position);
         lightning.setCosmetic(true);
         mc.world.addEntity(lightning);
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

         if (this.mode.getSelected().contains("Network") && this.spatialGrid != null && !this.networkParticles.isEmpty()) {
            NetworkRenderer.renderNetworkLinks(stack, this.networkParticles, this.spatialGrid, currentTime, this.linkDistance.getValue(), this);
         }

         if (CrystalExplosionEffect.hasActiveExplosions()) {
            CrystalExplosionEffect.render(stack);
         }

         if (CircleExplosionEffect.hasActiveShockwaves()) {
            CircleExplosionEffect.render(stack);
         }
      }
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.particles.clear();
      this.networkParticles.clear();
      this.recentlyAttacked.clear();
      this.recentlyAttackedByFriends.clear();
      this.pendingKills.clear();
      this.firedKills.clear();
      this.playerLastTickPos = null;
      CrystalExplosionEffect.clear();
      GhostExplosionEffect.clear();
      CircleExplosionEffect.clear();
      if (this.spatialGrid != null) {
         this.spatialGrid = null;
      }
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
      return selected.isEmpty() ? "Stars" : selected.get(ThreadLocalRandom.current().nextInt(selected.size()));
   }

   private String getRandomPhysics() {
      List<String> selected = this.physics.getSelected();
      return selected.isEmpty() ? "Emerge" : selected.get(ThreadLocalRandom.current().nextInt(selected.size()));
   }

   public SpatialGrid3D<WorldParticle> getSpatialGrid() {
      return this.spatialGrid;
   }

   public SelectSetting getEffectType() {
      return this.effectType;
   }

   public SelectSetting getFriendEffectType() {
      return this.friendEffectType;
   }

   public BooleanSetting getShowForFriends() {
      return this.showForFriends;
   }

   public SelectSetting getTargetType() {
      return this.targetType;
   }

   public ValueSetting getLinkDistance() {
      return this.linkDistance;
   }

   public BooleanSetting getEnableSound() {
      return this.enableSound;
   }

   public SelectSetting getSoundType() {
      return this.soundType;
   }

   public ValueSetting getSoundVolume() {
      return this.soundVolume;
   }

   public ValueSetting getSoundPitch() {
      return this.soundPitch;
   }

   private static class AttackRecord {
      final long timestamp;
      Vec3d lastKnownPos;
      float lastKnownHealth;
      boolean damageConfirmed;

      AttackRecord(long timestamp, Vec3d pos, float health) {
         this.timestamp = timestamp;
         this.lastKnownPos = pos;
         this.lastKnownHealth = health;
         this.damageConfirmed = false;
      }
   }

   private static class PendingKill {
      final Vec3d lastPosition;
      final long removedTime;
      final boolean killedByFriend;

      PendingKill(Vec3d lastPosition, long removedTime, boolean killedByFriend) {
         this.lastPosition = lastPosition;
         this.removedTime = removedTime;
         this.killedByFriend = killedByFriend;
      }
   }
}
