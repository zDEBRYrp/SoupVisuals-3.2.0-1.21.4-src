package padej.soup.implement.features.modules.hud;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.keyboard.KeyEvent;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BindSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;
import padej.soup.implement.features.modules.particles.TotemParticles;
import padej.soup.implement.menu.MenuScreen;

public class TotemPop extends Module {
   public final ValueSetting totemSize = new ValueSetting("setting.totempop.size.name", "setting.totempop.size.desc").setValue(0.4F).range(0.1F, 1.0F);
   public final ValueSetting totemAlpha = new ValueSetting("setting.totempop.alpha.name", "setting.totempop.alpha.desc").setValue(0.9F).range(0.1F, 1.0F);
   public final ValueSetting totemVolume = new ValueSetting("setting.totempop.volume.name", "setting.totempop.volume.desc").setValue(1.0F).range(0.0F, 1.0F);
   private final BindSetting fakeTotemKey = new BindSetting("setting.totempop.fakekey.name", "setting.totempop.fakekey.desc").setKey(80);

   public static TotemPop getInstance() {
      return Instance.get(TotemPop.class);
   }

   public TotemPop() {
      super("module.totempop.name", ModuleCategory.HUD);
      this.setup(new Setting[]{this.totemSize, this.totemAlpha, this.totemVolume, this.fakeTotemKey});
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if ((mc.currentScreen instanceof MenuScreen || mc.currentScreen == null)
         && event.key() == this.fakeTotemKey.getKey()
         && event.action() == 1
         && mc.currentScreen == null
         && mc.player != null
         && mc.world != null) {
         this.playFakeTotemEffect(mc.player);
      }
   }

   private void playFakeTotemEffect(LivingEntity entity) {
      TotemParticles totemModule = TotemParticles.getInstance();
      if (totemModule != null && totemModule.isEnabled()) {
         totemModule.onTotemPop(entity);
      } else {
         mc.particleManager.addEmitter(entity, ParticleTypes.TOTEM_OF_UNDYING, 30);
      }

      mc.world
         .playSound(
            entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ITEM_TOTEM_USE, entity.getSoundCategory(), this.totemVolume.getValue(), 1.0F, false
         );
      if (entity == mc.player) {
         mc.gameRenderer.showFloatingItem(new ItemStack(Items.TOTEM_OF_UNDYING));
      }
   }

   public ValueSetting getTotemSize() {
      return this.totemSize;
   }

   public ValueSetting getTotemAlpha() {
      return this.totemAlpha;
   }

   public ValueSetting getTotemVolume() {
      return this.totemVolume;
   }

   public BindSetting getFakeTotemKey() {
      return this.fakeTotemKey;
   }
}
