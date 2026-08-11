package padej.soup.implement.features.modules.other;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.player.EventAttack;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.other.Instance;

public class HitSounds extends Module {
   private final BooleanSetting enableCustomSound = new BooleanSetting("setting.hitsounds.enablecustomsound.name", "setting.hitsounds.enablecustomsound.desc")
      .setValue(false);
   private final SelectSetting soundType = new SelectSetting("setting.hitsounds.soundtype.name", "setting.hitsounds.soundtype.desc")
      .value("Hit", "8Bit", "Glass", "Drum Bass", "AimBooster", "Plastic", "Blip", "Pop", "Bell", "Bonk", "Bubble", "Get", "Magic Pok", "Off", "On", "Squish")
      .selected("Hit")
      .visible(this.enableCustomSound::isValue);
   private final ValueSetting volume = new ValueSetting("setting.hitsounds.volume.name", "setting.hitsounds.volume.desc")
      .setValue(1.0F)
      .range(0.1F, 2.0F)
      .visible(this.enableCustomSound::isValue);
   private final ValueSetting pitch = new ValueSetting("setting.hitsounds.pitch.name", "setting.hitsounds.pitch.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .visible(this.enableCustomSound::isValue);
   private final BooleanSetting onlyCrits = new BooleanSetting("setting.hitsounds.onlycrits.name", "setting.hitsounds.onlycrits.desc")
      .setValue(false)
      .visible(this.enableCustomSound::isValue);
   public final ValueSetting sweepVolume = new ValueSetting("setting.hitsounds.sweepvolume.name", "setting.hitsounds.sweepvolume.desc")
      .setValue(1.0F)
      .range(0.0F, 2.0F);
   public final ValueSetting critVolume = new ValueSetting("setting.hitsounds.critvolume.name", "setting.hitsounds.critvolume.desc")
      .setValue(1.0F)
      .range(0.0F, 2.0F);
   public final ValueSetting knockbackVolume = new ValueSetting("setting.hitsounds.knockbackvolume.name", "setting.hitsounds.knockbackvolume.desc")
      .setValue(1.0F)
      .range(0.0F, 2.0F);
   private boolean wasLastAttackCrit = false;

   public static HitSounds getInstance() {
      return Instance.get(HitSounds.class);
   }

   public HitSounds() {
      super("module.hitsounds.name", ModuleCategory.OTHER);
      GroupSetting customSoundGroup = new GroupSetting("group.hitsounds.customsound.name", "group.hitsounds.customsound.desc", false)
         .settings(this.enableCustomSound, this.soundType, this.volume, this.pitch, this.onlyCrits);
      GroupSetting vanillaGroup = new GroupSetting("group.hitsounds.vanilla.name", "group.hitsounds.vanilla.desc", false)
         .settings(this.sweepVolume, this.critVolume, this.knockbackVolume);
      this.setup(new Setting[]{customSoundGroup, vanillaGroup});
   }

   private boolean isCrit() {
      PlayerEntity player = mc.player;
      if (player == null) {
         return false;
      } else {
         boolean hasEnoughCooldown = player.getAttackCooldownProgress(0.5F) >= 0.848F;
         return hasEnoughCooldown
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
   public void onAttack(EventAttack event) {
      if (mc.player != null && mc.world != null) {
         if (event.isPre()) {
            this.wasLastAttackCrit = this.isCrit();
         } else if (this.enableCustomSound.isValue()) {
            if (event.getTarget() != null) {
               if (!this.onlyCrits.isValue() || this.wasLastAttackCrit) {
                  String var3 = this.soundType.getSelected();

                  SoundEvent sound = switch (var3) {
                     case "8Bit" -> SoundManager.TRINKET_8BIT;
                     case "Glass" -> SoundManager.TRINKET_GLASS;
                     case "Drum Bass" -> SoundManager.TRINKET_DRUM_BASS;
                     case "AimBooster" -> SoundManager.TRINKET_AIMBOOSTER;
                     case "Plastic" -> SoundManager.TRINKET_PLASTIC;
                     case "Blip" -> SoundManager.TRINKET_BLIP;
                     case "Pop" -> SoundManager.TRINKET_POP;
                     case "Bell" -> SoundManager.TRINKET_BELL;
                     case "Bonk" -> SoundManager.TRINKET_BONK;
                     case "Bubble" -> SoundManager.TRINKET_BUBBLE;
                     case "Get" -> SoundManager.TRINKET_GET;
                     case "Magic Pok" -> SoundManager.TRINKET_MAGIC_POK;
                     case "Off" -> SoundManager.TRINKET_OFF;
                     case "On" -> SoundManager.TRINKET_ON;
                     case "Squish" -> SoundManager.TRINKET_SQUISH;
                     default -> SoundManager.TRINKET_HIT;
                  };
                  SoundManager.playSound(sound, this.volume.getValue(), this.pitch.getValue());
               }
            }
         }
      }
   }

   public BooleanSetting getEnableCustomSound() {
      return this.enableCustomSound;
   }

   public SelectSetting getSoundType() {
      return this.soundType;
   }

   public ValueSetting getVolume() {
      return this.volume;
   }

   public ValueSetting getPitch() {
      return this.pitch;
   }

   public BooleanSetting getOnlyCrits() {
      return this.onlyCrits;
   }

   public ValueSetting getSweepVolume() {
      return this.sweepVolume;
   }

   public ValueSetting getCritVolume() {
      return this.critVolume;
   }

   public ValueSetting getKnockbackVolume() {
      return this.knockbackVolume;
   }
}
