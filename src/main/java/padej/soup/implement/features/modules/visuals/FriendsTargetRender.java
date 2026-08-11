package padej.soup.implement.features.modules.visuals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.player.TickEvent;
import padej.soup.api.event.events.render.WorldRenderEvent;
import padej.soup.api.feature.module.ITargetRenderModule;
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
import padej.soup.api.system.animation.Direction;
import padej.soup.base.util.entity.VisibleUtils;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.render.RenderSlot;
import padej.soup.base.util.render.TargetRenderer;
import padej.soup.core.server.ServerLimitCfg;
import padej.soup.implement.features.modules.particles.render.ParticleBatchRenderer;

public class FriendsTargetRender extends Module implements ITargetRenderModule {
   private Map<UUID, FriendsTargetRender.FriendTargetData> friendTargets = new HashMap<>();
   private final SelectSetting colorMode = new SelectSetting("setting.friendstargetrender.colormode.name", "setting.friendstargetrender.colormode.desc")
      .value("Sync", "Custom")
      .selected("Custom");
   private final SelectSetting customColorsCount = new SelectSetting(
         "setting.friendstargetrender.colorcount.name", "setting.friendstargetrender.colorcount.desc"
      )
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final MultiColorSetting customColors = new MultiColorSetting(
         "setting.friendstargetrender.gradientcolors.name", "setting.friendstargetrender.gradientcolors.desc"
      )
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final SelectSetting targetEspType = new SelectSetting("setting.friendstargetrender.rendertype.name", "setting.friendstargetrender.rendertype.desc")
      .value("Legacy", "Circle", "Ghosts", "Crystals")
      .selected("Legacy");
   private final SelectSetting legacyTexture = new SelectSetting(
         "setting.friendstargetrender.legacytexture.name", "setting.friendstargetrender.legacytexture.desc"
      )
      .value("Amogus", "Bo", "Capture", "Jeka", "Marker", "Scifi", "Simple", "Skull", "Vegas")
      .selected("Capture");
   private final BooleanSetting optimalAim = new BooleanSetting("setting.friendstargetrender.optimalaim.name", "setting.friendstargetrender.optimalaim.desc")
      .setValue(false);
   private final ValueSetting legacySize = new ValueSetting("setting.friendstargetrender.legacysize.name", "setting.friendstargetrender.legacysize.desc")
      .setValue(1.0F)
      .range(0.5F, 1.5F);
   private final SelectSetting ghostsTexture = new SelectSetting(
         "setting.friendstargetrender.ghoststexture.name", "setting.friendstargetrender.ghoststexture.desc"
      )
      .value("Bloom", "Soft")
      .selected("Soft");
   private final SelectSetting ghostsBlend = new SelectSetting("setting.friendstargetrender.ghostsblend.name", "setting.friendstargetrender.ghostsblend.desc")
      .value("Smoke", "Plasma")
      .selected("Plasma");
   private final ValueSetting ghostsLength = new ValueSetting("setting.friendstargetrender.ghostslength.name", "setting.friendstargetrender.ghostslength.desc")
      .setValue(1.3F)
      .range(0.5F, 1.5F);
   private final SelectSetting ghostsTrajectory = new SelectSetting(
         "setting.friendstargetrender.ghoststrajectory.name", "setting.friendstargetrender.ghoststrajectory.desc"
      )
      .value("Standard", "Spiral", "Atomic")
      .selected("Standard");
   private final ValueSetting ghostsRadiusModifier = new ValueSetting(
         "setting.friendstargetrender.ghostsradiusmodifier.name", "setting.friendstargetrender.ghostsradiusmodifier.desc"
      )
      .setValue(1.6F)
      .range(0.5F, 2.0F);
   private final ValueSetting ghostsHeadSize = new ValueSetting(
         "setting.friendstargetrender.ghostsheadsize.name", "setting.friendstargetrender.ghostsheadsize.desc"
      )
      .setValue(0.7F)
      .range(0.1F, 1.0F);
   private final ValueSetting ghostsTailSize = new ValueSetting(
         "setting.friendstargetrender.ghoststailsize.name", "setting.friendstargetrender.ghoststailsize.desc"
      )
      .setValue(0.1F)
      .range(0.1F, 1.0F);
   private final ValueSetting ghostsCount = new ValueSetting("setting.friendstargetrender.ghostscount.name", "setting.friendstargetrender.ghostscount.desc")
      .setValue(4.0F)
      .range(1, 8);
   private final ValueSetting ghostsSubdivision = new ValueSetting(
         "setting.friendstargetrender.ghostssubdivision.name", "setting.friendstargetrender.ghostssubdivision.desc"
      )
      .setValue(5.0F)
      .range(1, 8);
   private final ValueSetting crystalsDistance = new ValueSetting(
         "setting.friendstargetrender.crystalsdistance.name", "setting.friendstargetrender.crystalsdistance.desc"
      )
      .setValue(0.5F)
      .range(0.5F, 1.0F);
   private final ValueSetting crystalsSize = new ValueSetting("setting.friendstargetrender.crystalssize.name", "setting.friendstargetrender.crystalssize.desc")
      .setValue(0.35F)
      .range(0.2F, 0.4F);
   private final BooleanSetting crystalsGlow = new BooleanSetting(
         "setting.friendstargetrender.crystalsglow.name", "setting.friendstargetrender.crystalsglow.desc"
      )
      .setValue(true);
   private final ValueSetting crystalsGlowSize = new ValueSetting(
         "setting.friendstargetrender.crystalsglowsize.name", "setting.friendstargetrender.crystalsglowsize.desc"
      )
      .setValue(1.5F)
      .range(1.5F, 3.0F);
   private final SelectSetting crystalsOrientation = new SelectSetting(
         "setting.friendstargetrender.crystalsorientation.name", "setting.friendstargetrender.crystalsorientation.desc"
      )
      .value("Center", "Horizontal")
      .selected("Center");
   private final SelectSetting renderPriority = new SelectSetting("setting.friendstargetrender.priority.name", "setting.friendstargetrender.priority.desc")
      .value("Self", "Friend", "Both")
      .selected("Friend");
   private final MultiSelectSetting showOn = new MultiSelectSetting("setting.friendstargetrender.showon.name", "setting.friendstargetrender.showon.desc")
      .value("Self", "Friends")
      .selected("Friends");
   private final ValueSetting followTime = new ValueSetting("setting.friendstargetrender.followtime.name", "setting.friendstargetrender.followtime.desc")
      .setValue(5.0F)
      .range(0, 10);
   private final ValueSetting speedMod = new ValueSetting("setting.friendstargetrender.speedmod.name", "setting.friendstargetrender.speedmod.desc")
      .setValue(0.7F)
      .range(0.5F, 2.0F);
   private final ValueSetting maxDistance = new ValueSetting("setting.friendstargetrender.maxdistance.name", "setting.friendstargetrender.maxdistance.desc")
      .setValue(128.0F)
      .range(16.0F, 128.0F);
   static final int ANIMATION_MS = 200;

   public static FriendsTargetRender getInstance() {
      return Instance.get(FriendsTargetRender.class);
   }

   public FriendsTargetRender() {
      super("module.friendstargetrender.name", ModuleCategory.VISUALS, true, true);
      GroupSetting colorGroup = new GroupSetting("group.friendstargetrender.colors.name", "group.friendstargetrender.colors.desc", false)
         .settings(this.colorMode, this.customColorsCount, this.customColors);
      GroupSetting legacyGroup = new GroupSetting("group.friendstargetrender.legacy.name", "group.friendstargetrender.legacy.desc", false)
         .settings(this.legacyTexture, this.optimalAim, this.legacySize)
         .visible(() -> this.targetEspType.isSelected("Legacy"));
      GroupSetting ghostsVisualGroup = new GroupSetting("group.friendstargetrender.ghosts.visual.name", "group.friendstargetrender.ghosts.visual.desc", false)
         .settings(this.ghostsTexture, this.ghostsBlend);
      GroupSetting ghostsAnimationGroup = new GroupSetting(
            "group.friendstargetrender.ghosts.animation.name", "group.friendstargetrender.ghosts.animation.desc", false
         )
         .settings(this.ghostsLength, this.ghostsTrajectory);
      GroupSetting ghostsSizeGroup = new GroupSetting("group.friendstargetrender.ghosts.size.name", "group.friendstargetrender.ghosts.size.desc", false)
         .settings(this.ghostsRadiusModifier, this.ghostsHeadSize, this.ghostsTailSize, this.ghostsCount, this.ghostsSubdivision);
      GroupSetting ghostsGroup = new GroupSetting("group.friendstargetrender.ghosts.name", "group.friendstargetrender.ghosts.desc", false)
         .settings(ghostsVisualGroup, ghostsAnimationGroup, ghostsSizeGroup)
         .visible(() -> this.targetEspType.isSelected("Ghosts"));
      GroupSetting crystalsGroup = new GroupSetting("group.friendstargetrender.crystals.name", "group.friendstargetrender.crystals.desc", false)
         .settings(this.crystalsDistance, this.crystalsSize, this.crystalsGlow, this.crystalsGlowSize, this.crystalsOrientation)
         .visible(() -> this.targetEspType.isSelected("Crystals"));
      GroupSetting behaviorGroup = new GroupSetting("group.friendstargetrender.behavior.name", "group.friendstargetrender.behavior.desc", false)
         .settings(this.showOn, this.renderPriority, this.followTime, this.speedMod, this.maxDistance);
      this.setup(new Setting[]{colorGroup, this.targetEspType, legacyGroup, ghostsGroup, crystalsGroup, behaviorGroup});
   }

   @Override
   public void deactivate() {
      this.friendTargets.clear();
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (ServerLimitCfg.showFriendsTargetRender()) {
         long currentTime = System.currentTimeMillis();
         long followTimeMs = (long)(this.followTime.getValue() * 1000.0F);
         LivingEntity playerTarget = this.getPlayerTarget();
         Map<Integer, FriendsTargetRender.TargetRenderData> uniqueTargets = new HashMap<>();
         this.friendTargets
            .forEach(
               (uuid, data) -> {
                  Iterator<Entry<Integer, RenderSlot>> it = data.renderSlots.entrySet().iterator();

                  while (it.hasNext()) {
                     Entry<Integer, RenderSlot> slotEntry = it.next();
                     RenderSlot slot = slotEntry.getValue();
                     LivingEntity entity = slot.getEntity();
                     if (entity == null || this.shouldShowTarget(entity)) {
                        boolean isCurrentTarget = data.targetEntity != null && data.targetEntity.getId() == slotEntry.getKey();
                        boolean shouldRender;
                        if (isCurrentTarget) {
                           shouldRender = this.canFriendSeeTarget(data.friend, entity);
                        } else if (data.targetEntity != null) {
                           shouldRender = false;
                        } else if (slotEntry.getKey() == data.lastActiveTargetId) {
                           shouldRender = currentTime - slot.getLastActiveTime() < followTimeMs
                              && this.canFriendSeeTarget(data.friend, entity)
                              && !entity.isDead()
                              && entity.isAlive();
                        } else {
                           shouldRender = false;
                        }

                        slot.setDirection(shouldRender ? Direction.FORWARDS : Direction.BACKWARDS);
                        float animationDelta = slot.getAnimationDelta();
                        if (slot.isFinishedBackwards()) {
                           it.remove();
                        } else if (!(animationDelta <= 0.0F)
                           && entity != null
                           && (playerTarget == null || playerTarget != entity || !"Self".equals(this.renderPriority.getSelected()))) {
                           FriendsTargetRender.TargetRenderData existingData = uniqueTargets.get(entity.getId());
                           if (existingData == null || animationDelta > existingData.animationDelta) {
                              uniqueTargets.put(entity.getId(), new FriendsTargetRender.TargetRenderData(entity, animationDelta));
                           }
                        }
                     }
                  }
               }
            );
         float tickDelta = tickCounter.getTickDelta(false);
         boolean needsGhostFlush = false;

         for (FriendsTargetRender.TargetRenderData renderData : uniqueTargets.values()) {
            LivingEntity target = renderData.entity;
            float red = MathHelper.clamp((target.hurtTime - tickDelta) / 10.0F, 0.0F, 1.0F);
            String var14 = this.targetEspType.getSelected();
            switch (var14) {
               case "Legacy":
                  TargetRenderer.drawLegacy(target, renderData.animationDelta, red, this);
                  break;
               case "Circle":
                  TargetRenderer.drawCircle(e.getStack(), target, renderData.animationDelta, red, this);
                  break;
               case "Ghosts":
                  TargetRenderer.drawGhosts(
                     target,
                     renderData.animationDelta,
                     red,
                     this.speedMod.getValue(),
                     this.ghostsLength.getValue(),
                     this.ghostsRadiusModifier.getValue(),
                     this.ghostsHeadSize.getValue(),
                     this.ghostsTailSize.getValue(),
                     this.ghostsSubdivision.getValue(),
                     this
                  );
                  needsGhostFlush = true;
                  break;
               case "Crystals":
                  TargetRenderer.drawCrystals(e, target, renderData.animationDelta, red, this);
            }
         }

         if (needsGhostFlush) {
            ParticleBatchRenderer.renderBatches();
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.world != null && mc.player != null) {
         long currentTime = System.currentTimeMillis();

         for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (FriendUtils.isFriend(player) && player != mc.player) {
               if (!this.isWithinMaxDistance(player)) {
                  this.friendTargets.remove(player.getUuid());
               } else {
                  FriendsTargetRender.FriendTargetData data = this.friendTargets
                     .computeIfAbsent(player.getUuid(), uuid -> new FriendsTargetRender.FriendTargetData(player));
                  data.friend = player;
                  LivingEntity previousTarget = data.targetEntity;
                  data.targetEntity = this.getFriendTarget(player);
                  if (data.targetEntity != null && (data.targetEntity.isDead() || !data.targetEntity.isAlive())) {
                     data.targetEntity = null;
                  }

                  if (data.targetEntity != null) {
                     data.lastActiveTargetId = data.targetEntity.getId();
                  }

                  if (data.targetEntity != previousTarget) {
                     int currentTargetId = data.targetEntity != null ? data.targetEntity.getId() : -1;

                     for (Entry<Integer, RenderSlot> entry : data.renderSlots.entrySet()) {
                        if (entry.getKey() != currentTargetId) {
                           RenderSlot slot = entry.getValue();
                           if (slot.getLastActiveTime() == 0L) {
                              slot.setLastActiveTime(currentTime);
                           }

                           slot.setDirection(Direction.BACKWARDS);
                        }
                     }

                     if (data.targetEntity != null) {
                        RenderSlot newSlot = data.renderSlots.computeIfAbsent(data.targetEntity.getId(), id -> new RenderSlot(data.targetEntity, 200));
                        newSlot.setEntity(data.targetEntity);
                        newSlot.setDirection(Direction.FORWARDS);
                        newSlot.setLastActiveTime(0L);
                     }
                  }

                  if (data.targetEntity != null) {
                     RenderSlot slot = data.renderSlots.get(data.targetEntity.getId());
                     if (slot != null) {
                        slot.setEntity(data.targetEntity);
                     }
                  }
               }
            }
         }

         this.friendTargets.keySet().removeIf(uuid -> mc.world.getPlayers().stream().noneMatch(p -> p.getUuid().equals(uuid)));
         TargetRenderer.updateAnimations(this.speedMod.getValue());
      }
   }

   private boolean canFriendSeeTarget(AbstractClientPlayerEntity friend, LivingEntity target) {
      if (friend == null || target == null || mc.player == null) {
         return false;
      } else if (this.isWithinMaxDistance(friend) && this.isWithinMaxDistance(target)) {
         double distanceSq = friend.getPos().squaredDistanceTo(target.getPos());
         double maxDistanceSq = this.maxDistance.getValue() * this.maxDistance.getValue();
         if (distanceSq > maxDistanceSq) {
            return false;
         } else {
            return friend.canSee(target) ? VisibleUtils.canBeTargeted(target) : false;
         }
      } else {
         return false;
      }
   }

   private boolean isWithinMaxDistance(LivingEntity entity) {
      if (entity != null && mc.player != null) {
         double maxDistanceSq = this.maxDistance.getValue() * this.maxDistance.getValue();
         return mc.player.getPos().squaredDistanceTo(entity.getPos()) <= maxDistanceSq;
      } else {
         return false;
      }
   }

   private boolean shouldShowTarget(LivingEntity target) {
      boolean showOnSelf = this.showOn.isSelected("Self");
      boolean showOnFriends = this.showOn.isSelected("Friends");
      if (target == mc.player) {
         return showOnSelf;
      } else {
         return FriendUtils.isFriend(target) ? showOnFriends : true;
      }
   }

   private LivingEntity getPlayerTarget() {
      TargetRender targetRender = TargetRender.getInstance();
      return targetRender != null && targetRender.isEnabled() ? targetRender.getTargetEntity() : null;
   }

   private LivingEntity getFriendTarget(AbstractClientPlayerEntity friend) {
      float raycastDistance = friend.isCreative() ? 5.3F : 3.3F;
      Vec3d start = friend.getCameraPosVec(tickCounter.getTickDelta(false));
      Vec3d direction = friend.getRotationVec(tickCounter.getTickDelta(false));
      Vec3d end = start.add(direction.multiply(raycastDistance));
      Box box = friend.getBoundingBox().stretch(direction.multiply(raycastDistance)).expand(1.0);
      EntityHitResult result = ProjectileUtil.raycast(
         friend, start, end, box, entity -> !entity.isSpectator() && entity.isAlive() && entity instanceof LivingEntity, raycastDistance * raycastDistance
      );
      return result != null && result.getEntity() instanceof LivingEntity target && VisibleUtils.canBeTargeted(target) ? target : null;
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
   public boolean isOptimalAim() {
      return this.optimalAim.isValue();
   }

   @Override
   public boolean isStaticMode() {
      return false;
   }

   @Override
   public float getLegacySize() {
      return this.legacySize.getValue();
   }

   @Override
   public Identifier getGhostTexture() {
      String var1 = this.ghostsTexture.getSelected();

      return switch (var1) {
         case "Bloom" -> Identifier.of("textures/particles/bloom/bloom.png");
         case "Soft" -> Identifier.of("textures/particles/bloom/bloom_soft.png");
         default -> null;
      };
   }

   @Override
   public Identifier getLegacyTexture() {
      String var1 = this.legacyTexture.getSelected();

      return switch (var1) {
         case "Amogus" -> Identifier.of("textures/legacy/amongus.png");
         case "Bo" -> Identifier.of("textures/legacy/bo.png");
         case "Capture" -> Identifier.of("textures/legacy/capture.png");
         case "Jeka" -> Identifier.of("textures/legacy/jeka.png");
         case "Marker" -> Identifier.of("textures/legacy/marker.png");
         case "Scifi" -> Identifier.of("textures/legacy/scifi.png");
         case "Simple" -> Identifier.of("textures/legacy/simple.png");
         case "Skull" -> Identifier.of("textures/legacy/skull.png");
         case "Vegas" -> Identifier.of("textures/legacy/vegas.png");
         default -> null;
      };
   }

   public boolean isFriendTargeting(LivingEntity entity) {
      if (entity == null) {
         return false;
      } else {
         for (FriendsTargetRender.FriendTargetData data : this.friendTargets.values()) {
            if (data.targetEntity == entity) {
               return true;
            }

            if (data.renderSlots.containsKey(entity.getId())) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public float getCrystalsDistance() {
      return this.crystalsDistance.getValue();
   }

   @Override
   public float getCrystalsSize() {
      return this.crystalsSize.getValue();
   }

   @Override
   public boolean isCrystalsGlow() {
      return this.crystalsGlow.isValue();
   }

   @Override
   public float getCrystalsGlowSize() {
      return this.crystalsGlowSize.getValue();
   }

   @Override
   public boolean isCrystalsHorizontal() {
      return this.crystalsOrientation.isSelected("Horizontal");
   }

   public Map<UUID, FriendsTargetRender.FriendTargetData> getFriendTargets() {
      return this.friendTargets;
   }

   public SelectSetting getColorMode() {
      return this.colorMode;
   }

   public SelectSetting getCustomColorsCount() {
      return this.customColorsCount;
   }

   public SelectSetting getTargetEspType() {
      return this.targetEspType;
   }

   public BooleanSetting getOptimalAim() {
      return this.optimalAim;
   }

   public SelectSetting getGhostsTexture() {
      return this.ghostsTexture;
   }

   @Override
   public SelectSetting getGhostsBlend() {
      return this.ghostsBlend;
   }

   public ValueSetting getGhostsLength() {
      return this.ghostsLength;
   }

   @Override
   public SelectSetting getGhostsTrajectory() {
      return this.ghostsTrajectory;
   }

   public ValueSetting getGhostsRadiusModifier() {
      return this.ghostsRadiusModifier;
   }

   public ValueSetting getGhostsHeadSize() {
      return this.ghostsHeadSize;
   }

   public ValueSetting getGhostsTailSize() {
      return this.ghostsTailSize;
   }

   @Override
   public ValueSetting getGhostsCount() {
      return this.ghostsCount;
   }

   public ValueSetting getGhostsSubdivision() {
      return this.ghostsSubdivision;
   }

   public BooleanSetting getCrystalsGlow() {
      return this.crystalsGlow;
   }

   public SelectSetting getCrystalsOrientation() {
      return this.crystalsOrientation;
   }

   public SelectSetting getRenderPriority() {
      return this.renderPriority;
   }

   public MultiSelectSetting getShowOn() {
      return this.showOn;
   }

   public ValueSetting getFollowTime() {
      return this.followTime;
   }

   public ValueSetting getSpeedMod() {
      return this.speedMod;
   }

   public ValueSetting getMaxDistance() {
      return this.maxDistance;
   }

   private static class FriendTargetData {
      private AbstractClientPlayerEntity friend;
      private LivingEntity targetEntity;
      private final Map<Integer, RenderSlot> renderSlots = new HashMap<>();
      private int lastActiveTargetId = -1;

      FriendTargetData(AbstractClientPlayerEntity friend) {
         this.friend = friend;
      }
   }

   private record TargetRenderData(LivingEntity entity, float animationDelta) {
   }
}
