package padej.soup.implement.features.modules.client;

import com.mojang.authlib.GameProfile;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import padej.soup.base.QuickImports;
import padej.soup.core.Main;
import padej.soup.implement.features.modules.particles.TotemParticles;

public class FakePlayerEntity extends OtherClientPlayerEntity implements QuickImports {
   private boolean isSneaking = false;
   private Box defaultBoundingBox;

   public FakePlayerEntity(ClientWorld clientWorld, GameProfile gameProfile) {
      super(clientWorld, new GameProfile(gameProfile.getId(), "[Fake]"));
      this.copyDataFromLocalPlayer();
   }

   public void copyDataFromLocalPlayer() {
      ClientPlayerEntity localPlayer = mc.player;
      if (localPlayer != null) {
         this.updateDataFromClientPlayer(localPlayer);
      }
   }

   public void updateDataFromClientPlayer(ClientPlayerEntity localPlayer) {
      this.copyPositionAndRotation(localPlayer);
      this.setFireTicks(localPlayer.getFireTicks());
      this.setHealth(localPlayer.getHealth());
      this.setSneaking(localPlayer.isSneaking());
      this.setSprinting(false);
      this.noClip = false;
      this.limbAnimator.updateLimbs(0.0F, 0.0F, 20.0F);
      this.defaultBoundingBox = localPlayer.getBoundingBox();
      this.bodyYaw = localPlayer.bodyYaw;
      this.headYaw = localPlayer.headYaw;
      this.capeX = localPlayer.capeX;
      this.capeY = localPlayer.capeY;
      this.capeZ = localPlayer.capeZ;
      this.fallDistance = localPlayer.fallDistance;
      this.isSneaking = localPlayer.isSneaking();
      this.applyArmor();
   }

   public void applyArmor() {
      FakePlayer fakePlayerModule = FakePlayer.getInstance();
      if (fakePlayerModule != null) {
         if (fakePlayerModule.armor.isSelected("Helmet")) {
            this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
         } else {
            this.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
         }

         if (fakePlayerModule.armor.isSelected("Chestplate")) {
            this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
         } else {
            this.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
         }

         if (fakePlayerModule.armor.isSelected("Leggings")) {
            this.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
         } else {
            this.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
         }

         if (fakePlayerModule.armor.isSelected("Boots")) {
            this.equipStack(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
         } else {
            this.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);
         }
      }
   }

   public void tick() {
      super.tick();
      ClientPlayerEntity localPlayer = mc.player;
      if (localPlayer != null) {
         if (this.isSneaking) {
            Vec3d pos = this.getPos();
            this.setBoundingBox(new Box(pos.x - 0.3, pos.y, pos.z - 0.3, pos.x + 0.3, pos.y + 1.5, pos.z + 0.3));
         } else if (this.defaultBoundingBox != null) {
            this.setBoundingBox(this.defaultBoundingBox);
         }
      }
   }

   public boolean isInSneakingPose() {
      return this.isSneaking;
   }

   public boolean canBeHitByProjectile() {
      return false;
   }

   public void pushAwayFrom(Entity entity) {
   }

   public boolean isPushable() {
      return false;
   }

   protected void pushAway(Entity entity) {
   }

   public boolean isPartVisible(PlayerModelPart modelPart) {
      ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
      return localPlayer != null ? localPlayer.isPartVisible(modelPart) : super.isPartVisible(modelPart);
   }

   public void handleAttack() {
      MinecraftClient mc = Main.mc;
      if (mc.world != null && mc.player != null) {
         mc.world.playSound(mc.player, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0F, 1.0F);
         this.onDamaged(mc.world.getDamageSources().generic());
         float damage = this.calculateDamage();
         boolean isCrit = mc.player.fallDistance > 0.0F
            && !mc.player.isOnGround()
            && !mc.player.isClimbing()
            && !mc.player.isTouchingWater()
            && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
            && mc.player.getVelocity().y < 0.0
            && !mc.player.hasVehicle();
         if (isCrit) {
            damage *= 1.5F;
            mc.world.playSound(mc.player, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 1.0F);

            for (int i = 0; i < 5; i++) {
               double offsetX = this.getRandom().nextGaussian() * 0.02;
               double offsetY = this.getRandom().nextGaussian() * 0.02;
               double offsetZ = this.getRandom().nextGaussian() * 0.02;
               mc.world
                  .addParticle(
                     ParticleTypes.CRIT,
                     this.getX() + offsetX,
                     this.getY() + this.getHeight() * 0.5 + offsetY,
                     this.getZ() + offsetZ,
                     offsetX,
                     offsetY,
                     offsetZ
                  );
            }
         }

         if (FakePlayer.getInstance().autoTotem.isValue()
            && this.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING
            && this.getHealth() + this.getAbsorptionAmount() <= damage) {
            this.setHealth(1.0F);
            this.setAbsorptionAmount(8.0F);
            this.getOffHandStack().decrement(1);
            mc.world.playSound(mc.player, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
            TotemParticles totemModule = TotemParticles.getInstance();
            if (totemModule != null && totemModule.isEnabled()) {
               totemModule.onTotemPop(this);
            } else {
               mc.particleManager.addEmitter(this, ParticleTypes.TOTEM_OF_UNDYING, 30);
            }
         } else {
            if (this.getAbsorptionAmount() > 0.0F) {
               float absorptionDamage = Math.min(damage, this.getAbsorptionAmount());
               this.setAbsorptionAmount(this.getAbsorptionAmount() - absorptionDamage);
               damage -= absorptionDamage;
            }

            this.setHealth(Math.max(0.0F, this.getHealth() - damage));
         }
      }
   }

   private float calculateDamage() {
      MinecraftClient mc = Main.mc;
      ItemStack weapon = mc.player.getMainHandStack();
      if (weapon.isEmpty()) {
         return 1.0F;
      } else {
         float baseDamage = this.getDamageForItem(weapon.getItem());
         if (mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            int strengthLevel = Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.STRENGTH)).getAmplifier() + 1;
            baseDamage += 3.0F * strengthLevel;
         }

         if (mc.player.hasStatusEffect(StatusEffects.WEAKNESS)) {
            int weaknessLevel = Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.WEAKNESS)).getAmplifier() + 1;
            baseDamage -= 4.0F * weaknessLevel;
         }

         return Math.max(0.0F, baseDamage);
      }
   }

   private float getDamageForItem(Item item) {
      if (item == Items.NETHERITE_SWORD) {
         return 8.0F;
      } else if (item == Items.DIAMOND_SWORD) {
         return 7.0F;
      } else if (item == Items.IRON_SWORD) {
         return 6.0F;
      } else if (item == Items.STONE_SWORD) {
         return 5.0F;
      } else if (item == Items.WOODEN_SWORD || item == Items.GOLDEN_SWORD) {
         return 4.0F;
      } else if (item == Items.NETHERITE_AXE) {
         return 10.0F;
      } else if (item == Items.DIAMOND_AXE) {
         return 9.0F;
      } else if (item == Items.IRON_AXE) {
         return 9.0F;
      } else if (item == Items.STONE_AXE) {
         return 9.0F;
      } else if (item == Items.WOODEN_AXE || item == Items.GOLDEN_AXE) {
         return 7.0F;
      } else if (item == Items.TRIDENT) {
         return 9.0F;
      } else if (item == Items.NETHERITE_PICKAXE || item == Items.DIAMOND_PICKAXE) {
         return 6.0F;
      } else if (item == Items.IRON_PICKAXE) {
         return 5.0F;
      } else if (item == Items.STONE_PICKAXE) {
         return 4.0F;
      } else if (item == Items.WOODEN_PICKAXE || item == Items.GOLDEN_PICKAXE) {
         return 3.0F;
      } else if (item == Items.NETHERITE_SHOVEL || item == Items.DIAMOND_SHOVEL) {
         return 5.5F;
      } else if (item == Items.IRON_SHOVEL) {
         return 4.5F;
      } else if (item == Items.STONE_SHOVEL) {
         return 3.5F;
      } else {
         return item != Items.WOODEN_SHOVEL && item != Items.GOLDEN_SHOVEL ? 1.0F : 2.5F;
      }
   }
}
