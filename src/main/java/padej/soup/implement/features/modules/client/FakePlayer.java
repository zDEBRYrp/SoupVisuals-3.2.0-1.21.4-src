package padej.soup.implement.features.modules.client;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.packet.PacketEvent;
import padej.soup.api.event.events.player.TickEvent;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.core.Main;

public class FakePlayer extends Module {
   public static FakePlayerEntity fakePlayer;
   private boolean isInitialized = false;
   private int deathTime;
   private int regenTicks = 0;
   private boolean wasAttackKeyPressed = false;
   public final BooleanSetting autoTotem = new BooleanSetting("setting.fakeplayer.autototem.name", "setting.fakeplayer.autototem.desc").setValue(false);
   private final BooleanSetting regen = new BooleanSetting("setting.fakeplayer.regen.name", "setting.fakeplayer.regen.desc").setValue(true);
   private final BooleanSetting invisibility = new BooleanSetting("setting.fakeplayer.invisibility.name", "setting.fakeplayer.invisibility.desc")
      .setValue(false);
   public final MultiSelectSetting armor = new MultiSelectSetting("setting.fakeplayer.armor.name", "setting.fakeplayer.armor.desc")
      .value("Helmet", "Chestplate", "Leggings", "Boots")
      .selected();

   public static FakePlayer getInstance() {
      return Instance.get(FakePlayer.class);
   }

   public FakePlayer() {
      super("module.fakeplayer.name", ModuleCategory.CLIENT);
      this.setup(new Setting[]{this.autoTotem, this.regen, this.invisibility, this.armor});
   }

   @Override
   public void activate() {
      super.activate();
      MinecraftClient client = Main.mc;
      if (client.world != null && client.player != null) {
         this.summonFakePlayer(true);
      }
   }

   private void summonFakePlayer(boolean firstTime) {
      this.destroyFakePlayer();
      GameProfile cloneProfile = mc.player.getGameProfile();
      fakePlayer = new FakePlayerEntity(mc.world, cloneProfile);
      fakePlayer.setPosition(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      mc.world.addEntity(fakePlayer);
      this.isInitialized = true;
      if (firstTime) {
         this.summonFakePlayer(false);
      }
   }

   private void destroyFakePlayer() {
      if (fakePlayer != null) {
         try {
            if (mc.world != null) {
               fakePlayer.remove(RemovalReason.DISCARDED);
            }
         } catch (Exception var2) {
            LoggerUtil.error("Failed to destroy fake player: " + var2.getMessage());
         }

         fakePlayer = null;
         this.isInitialized = false;
      }
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.destroyFakePlayer();
      this.deathTime = 0;
      this.regenTicks = 0;
      this.wasAttackKeyPressed = false;
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (fakePlayer == null && !this.isInitialized && mc.world != null && mc.player != null) {
         this.summonFakePlayer(true);
      }

      if (fakePlayer != null && this.isInitialized && mc.world != null && mc.player != null) {
         if (this.autoTotem.isValue() && fakePlayer.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            fakePlayer.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
         } else if (!this.autoTotem.isValue() && fakePlayer.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            fakePlayer.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
         }

         if (this.regen.isValue()) {
            this.regenTicks++;
            if (this.regenTicks >= 10) {
               this.regenTicks = 0;
               float currentHealth = fakePlayer.getHealth();
               float newHealth = Math.min(currentHealth + 10.0F, 20.0F);
               if (fakePlayer.getHealth() >= 0.0F && fakePlayer.isAlive()) {
                  fakePlayer.setHealth(newHealth);
               }
            }
         }

         fakePlayer.setInvisible(this.invisibility.isValue());
         fakePlayer.applyArmor();
         boolean isAttackKeyPressed = mc.options.attackKey.isPressed();
         if (mc.crosshairTarget instanceof EntityHitResult entityHit
            && entityHit.getEntity() == fakePlayer
            && fakePlayer.hurtTime == 0
            && isAttackKeyPressed
            && !this.wasAttackKeyPressed) {
            fakePlayer.handleAttack();
         }

         this.wasAttackKeyPressed = isAttackKeyPressed;
         if (fakePlayer.isDead()) {
            this.deathTime++;
            if (this.deathTime > 10) {
               this.setState(false);
            }
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (event.isSend() && event.getPacket() instanceof PlayerInteractEntityC2SPacket packet && fakePlayer != null && mc.world != null && mc.player != null) {
         try {
            Field entityIdField = PlayerInteractEntityC2SPacket.class.getDeclaredField("entityId");
            entityIdField.setAccessible(true);
            int entityId = (Integer)entityIdField.get(packet);
            Entity target = mc.world.getEntityById(entityId);
            if (target == fakePlayer) {
               event.cancel();
               if (fakePlayer.hurtTime == 0) {
                  Field typeField = PlayerInteractEntityC2SPacket.class.getDeclaredField("type");
                  typeField.setAccessible(true);
                  Object type = typeField.get(packet);
                  if (type != null && type.getClass().getSimpleName().equals("Attack")) {
                     fakePlayer.handleAttack();
                  }
               }
            }
         } catch (Exception var8) {
            if (mc.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() == fakePlayer) {
               event.cancel();
               if (fakePlayer.hurtTime == 0 && mc.options.attackKey.isPressed()) {
                  fakePlayer.handleAttack();
               }
            }
         }
      }
   }

   public boolean isInitialized() {
      return this.isInitialized;
   }

   public int getDeathTime() {
      return this.deathTime;
   }

   public int getRegenTicks() {
      return this.regenTicks;
   }

   public boolean isWasAttackKeyPressed() {
      return this.wasAttackKeyPressed;
   }

   public BooleanSetting getAutoTotem() {
      return this.autoTotem;
   }

   public BooleanSetting getRegen() {
      return this.regen;
   }

   public BooleanSetting getInvisibility() {
      return this.invisibility;
   }

   public MultiSelectSetting getArmor() {
      return this.armor;
   }
}
