package padej.soup.api.feature.module;

import java.util.ArrayList;
import java.util.List;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.implement.features.modules.client.FakePlayer;
import padej.soup.implement.features.modules.client.KeyBind;
import padej.soup.implement.features.modules.client.Language;
import padej.soup.implement.features.modules.client.Theme;
import padej.soup.implement.features.modules.hud.Armor;
import padej.soup.implement.features.modules.hud.BossBars;
import padej.soup.implement.features.modules.hud.Consumable;
import padej.soup.implement.features.modules.hud.CoolDowns;
import padej.soup.implement.features.modules.hud.CrossHair;
import padej.soup.implement.features.modules.hud.HotBar;
import padej.soup.implement.features.modules.hud.HotKeys;
import padej.soup.implement.features.modules.hud.Icons;
import padej.soup.implement.features.modules.hud.Inventory;
import padej.soup.implement.features.modules.hud.ItemOverlay;
import padej.soup.implement.features.modules.hud.ModulesList;
import padej.soup.implement.features.modules.hud.NoFire;
import padej.soup.implement.features.modules.hud.Notifications;
import padej.soup.implement.features.modules.hud.PlayerInfo;
import padej.soup.implement.features.modules.hud.Potions;
import padej.soup.implement.features.modules.hud.ScoreBoard;
import padej.soup.implement.features.modules.hud.TabList;
import padej.soup.implement.features.modules.hud.TargetHud;
import padej.soup.implement.features.modules.hud.TotemPop;
import padej.soup.implement.features.modules.hud.Watermark;
import padej.soup.implement.features.modules.mctiers.Balancer;
import padej.soup.implement.features.modules.other.AspectRatio;
import padej.soup.implement.features.modules.other.EndCrystal;
import padej.soup.implement.features.modules.other.HandTweaks;
import padej.soup.implement.features.modules.other.HitSounds;
import padej.soup.implement.features.modules.other.KillEffect;
import padej.soup.implement.features.modules.other.NameProtect;
import padej.soup.implement.features.modules.other.Trinket;
import padej.soup.implement.features.modules.particles.AmbientParticles;
import padej.soup.implement.features.modules.particles.HitBubbles;
import padej.soup.implement.features.modules.particles.HitParticles;
import padej.soup.implement.features.modules.particles.TotemParticles;
import padej.soup.implement.features.modules.visuals.BetterGlow;
import padej.soup.implement.features.modules.visuals.BlockHighLight;
import padej.soup.implement.features.modules.visuals.ChinaHat;
import padej.soup.implement.features.modules.visuals.FriendsTargetRender;
import padej.soup.implement.features.modules.visuals.HandsShader;
import padej.soup.implement.features.modules.visuals.Hitboxes;
import padej.soup.implement.features.modules.visuals.JumpCircles;
import padej.soup.implement.features.modules.visuals.TargetRender;
import padej.soup.implement.features.modules.visuals.Trails;
import padej.soup.implement.features.modules.world.Bright;
import padej.soup.implement.features.modules.world.Fog;
import padej.soup.implement.features.modules.world.Gps;
import padej.soup.implement.features.modules.world.Light;
import padej.soup.implement.features.modules.world.Time;

public class ModuleRepository {
   private final List<Module> modules = new ArrayList<>();

   public void setup() {
      this.register(
         new Language(),
         new FakePlayer(),
         new KeyBind(),
         new Theme(),
         new Armor(),
         new BossBars(),
         new Consumable(),
         new CoolDowns(),
         new ModulesList(),
         new CrossHair(),
         new HotBar(),
         new HotKeys(),
         new Icons(),
         new Inventory(),
         new ItemOverlay(),
         new NoFire(),
         new Notifications(),
         new PlayerInfo(),
         new Potions(),
         new ScoreBoard(),
         new TabList(),
         new TargetHud(),
         new TotemPop(),
         new Watermark(),
         new Balancer(),
         new AspectRatio(),
         new EndCrystal(),
         new HandTweaks(),
         new HitSounds(),
         new KillEffect(),
         new NameProtect(),
         new Trinket(),
         new AmbientParticles(),
         new HitBubbles(),
         new HitParticles(),
         new TotemParticles(),
         new BetterGlow(),
         new BlockHighLight(),
         new ChinaHat(),
         new FriendsTargetRender(),
         new HandsShader(),
         new Hitboxes(),
         new JumpCircles(),
         new TargetRender(),
         new Trails(),
         new Bright(),
         new Fog(),
         new Gps(),
         new Light(),
         new Time()
      );
   }

   public void register(Module... module) {
      for (Module m : module) {
         if (!m.isSetupCalled()) {
            LoggerUtil.warn(
               "[SoupAPI] Module '"
                  + m.getIdentifier()
                  + "' was registered without calling setup(). Settings will not be saved or shown in the menu. Call setup(setting1, setting2, ...) at the end of the constructor."
            );
         }
      }

      this.modules.addAll(List.of(module));
   }

   public List<Module> modules() {
      return this.modules;
   }
}
