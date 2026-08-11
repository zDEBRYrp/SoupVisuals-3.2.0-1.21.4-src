package padej.soup.api.system.sound;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import padej.soup.base.QuickImports;
import padej.soup.base.util.entity.PlayerIntersectionUtil;

public final class SoundManager implements QuickImports {
   public static SoundEvent OPEN_GUI = SoundEvent.of(Identifier.of("minecraft:gui_open"));
   public static SoundEvent CLOSE_GUI = SoundEvent.of(Identifier.of("minecraft:gui_close"));
   public static SoundEvent ENABLE_MODULE = SoundEvent.of(Identifier.of("minecraft:module_enable"));
   public static SoundEvent DISABLE_MODULE = SoundEvent.of(Identifier.of("minecraft:module_disable"));
   public static SoundEvent TURN_ON = SoundEvent.of(Identifier.of("minecraft:turn_on_click"));
   public static SoundEvent TURN_OFF = SoundEvent.of(Identifier.of("minecraft:turn_off_click"));
   public static SoundEvent CLICK = SoundEvent.of(Identifier.of("minecraft:click"));
   public static SoundEvent CHANGE_CATEGORY = SoundEvent.of(Identifier.of("minecraft:change_category"));
   public static SoundEvent SLIDER_STEP = SoundEvent.of(Identifier.of("minecraft:slider_step"));
   public static SoundEvent TRINKET_HIT = SoundEvent.of(Identifier.of("minecraft:trinket_hit"));
   public static SoundEvent TRINKET_8BIT = SoundEvent.of(Identifier.of("minecraft:8bit"));
   public static SoundEvent TRINKET_GLASS = SoundEvent.of(Identifier.of("minecraft:glass"));
   public static SoundEvent TRINKET_DRUM_BASS = SoundEvent.of(Identifier.of("minecraft:drum_bass"));
   public static SoundEvent TRINKET_AIMBOOSTER = SoundEvent.of(Identifier.of("minecraft:aimbooster_hit"));
   public static SoundEvent TRINKET_PLASTIC = SoundEvent.of(Identifier.of("minecraft:plastic_ball"));
   public static SoundEvent TRINKET_BLIP = SoundEvent.of(Identifier.of("minecraft:blip"));
   public static SoundEvent TRINKET_POP = SoundEvent.of(Identifier.of("minecraft:pop"));
   public static SoundEvent TRINKET_BELL = SoundEvent.of(Identifier.of("minecraft:bell"));
   public static SoundEvent TRINKET_BONK = SoundEvent.of(Identifier.of("minecraft:bonk"));
   public static SoundEvent TRINKET_BUBBLE = SoundEvent.of(Identifier.of("minecraft:bubble"));
   public static SoundEvent TRINKET_GET = SoundEvent.of(Identifier.of("minecraft:get"));
   public static SoundEvent TRINKET_MAGIC_POK = SoundEvent.of(Identifier.of("minecraft:magic_pok"));
   public static SoundEvent TRINKET_OFF = SoundEvent.of(Identifier.of("minecraft:off"));
   public static SoundEvent TRINKET_ON = SoundEvent.of(Identifier.of("minecraft:on"));
   public static SoundEvent TRINKET_SQUISH = SoundEvent.of(Identifier.of("minecraft:squish"));
   public static SoundEvent KILL_ABMISS = SoundEvent.of(Identifier.of("minecraft:kill_sounds/abmiss"));
   public static SoundEvent KILL_CRITOW = SoundEvent.of(Identifier.of("minecraft:kill_sounds/critow"));
   public static SoundEvent KILL_FINAL_BLINK = SoundEvent.of(Identifier.of("minecraft:kill_sounds/final_blink"));
   public static SoundEvent KILL_FINAL_POK = SoundEvent.of(Identifier.of("minecraft:kill_sounds/final_pok"));
   public static SoundEvent KILL_NEPTUNE = SoundEvent.of(Identifier.of("minecraft:kill_sounds/neptune"));
   public static SoundEvent KILL_RUST_HEADSHOT = SoundEvent.of(Identifier.of("minecraft:kill_sounds/rust_headshot"));
   public static SoundEvent KILL_WHII = SoundEvent.of(Identifier.of("minecraft:kill_sounds/whii"));

   public static void init() {
      Registry.register(Registries.SOUND_EVENT, OPEN_GUI.id(), OPEN_GUI);
      Registry.register(Registries.SOUND_EVENT, CLOSE_GUI.id(), CLOSE_GUI);
      Registry.register(Registries.SOUND_EVENT, ENABLE_MODULE.id(), ENABLE_MODULE);
      Registry.register(Registries.SOUND_EVENT, DISABLE_MODULE.id(), DISABLE_MODULE);
      Registry.register(Registries.SOUND_EVENT, TURN_ON.id(), TURN_ON);
      Registry.register(Registries.SOUND_EVENT, TURN_OFF.id(), TURN_OFF);
      Registry.register(Registries.SOUND_EVENT, CLICK.id(), CLICK);
      Registry.register(Registries.SOUND_EVENT, CHANGE_CATEGORY.id(), CHANGE_CATEGORY);
      Registry.register(Registries.SOUND_EVENT, SLIDER_STEP.id(), SLIDER_STEP);
      Registry.register(Registries.SOUND_EVENT, TRINKET_HIT.id(), TRINKET_HIT);
      Registry.register(Registries.SOUND_EVENT, TRINKET_8BIT.id(), TRINKET_8BIT);
      Registry.register(Registries.SOUND_EVENT, TRINKET_GLASS.id(), TRINKET_GLASS);
      Registry.register(Registries.SOUND_EVENT, TRINKET_DRUM_BASS.id(), TRINKET_DRUM_BASS);
      Registry.register(Registries.SOUND_EVENT, TRINKET_AIMBOOSTER.id(), TRINKET_AIMBOOSTER);
      Registry.register(Registries.SOUND_EVENT, TRINKET_PLASTIC.id(), TRINKET_PLASTIC);
      Registry.register(Registries.SOUND_EVENT, TRINKET_BLIP.id(), TRINKET_BLIP);
      Registry.register(Registries.SOUND_EVENT, TRINKET_POP.id(), TRINKET_POP);
      Registry.register(Registries.SOUND_EVENT, TRINKET_BELL.id(), TRINKET_BELL);
      Registry.register(Registries.SOUND_EVENT, TRINKET_BONK.id(), TRINKET_BONK);
      Registry.register(Registries.SOUND_EVENT, TRINKET_BUBBLE.id(), TRINKET_BUBBLE);
      Registry.register(Registries.SOUND_EVENT, TRINKET_GET.id(), TRINKET_GET);
      Registry.register(Registries.SOUND_EVENT, TRINKET_MAGIC_POK.id(), TRINKET_MAGIC_POK);
      Registry.register(Registries.SOUND_EVENT, TRINKET_OFF.id(), TRINKET_OFF);
      Registry.register(Registries.SOUND_EVENT, TRINKET_ON.id(), TRINKET_ON);
      Registry.register(Registries.SOUND_EVENT, TRINKET_SQUISH.id(), TRINKET_SQUISH);
      Registry.register(Registries.SOUND_EVENT, KILL_ABMISS.id(), KILL_ABMISS);
      Registry.register(Registries.SOUND_EVENT, KILL_CRITOW.id(), KILL_CRITOW);
      Registry.register(Registries.SOUND_EVENT, KILL_FINAL_BLINK.id(), KILL_FINAL_BLINK);
      Registry.register(Registries.SOUND_EVENT, KILL_FINAL_POK.id(), KILL_FINAL_POK);
      Registry.register(Registries.SOUND_EVENT, KILL_NEPTUNE.id(), KILL_NEPTUNE);
      Registry.register(Registries.SOUND_EVENT, KILL_RUST_HEADSHOT.id(), KILL_RUST_HEADSHOT);
      Registry.register(Registries.SOUND_EVENT, KILL_WHII.id(), KILL_WHII);
   }

   public static void playSound(SoundEvent sound) {
      playSound(sound, 1.0F, 1.0F);
   }

   public static void playSound(SoundEvent sound, float volume, float pitch) {
      if (!PlayerIntersectionUtil.nullCheck() && mc.world != null && mc.player != null) {
         mc.world.playSound(mc.player, mc.player.getBlockPos(), sound, SoundCategory.BLOCKS, volume, pitch);
      }
   }

   private SoundManager() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
