package padej.soup.implement.features.modules.mctiers;

import padej.soup.api.feature.module.McTiersModule;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.other.Instance;

public class Balancer extends McTiersModule {
   public final SelectSetting fixType = new SelectSetting("setting.balancer.fixtype.name", "setting.balancer.fixtype.desc")
      .value("Vanilla", "UHC", "Pot", "NethOP", "SMP", "Sword", "Axe", "Mace")
      .selected("Vanilla");

   public static Balancer getInstance() {
      return Instance.get(Balancer.class);
   }

   public Balancer() {
      super("module.balancer.name", ModuleCategory.MCTIERS);
      this.setup(new Setting[]{this.fixType});
   }

   @Override
   protected void logActivationWithSettings() {
      LoggerUtil.protectedInfo("Balancer activated with fixType: " + this.fixType.getSelected());
   }

   public String rpcIcon() {
      return this.isEnabled() ? this.fixType.getSelected().toLowerCase() : "";
   }

   public boolean isVanilla() {
      return this.fixType.isSelected("Vanilla");
   }

   public boolean isUHC() {
      return this.fixType.isSelected("UHC");
   }

   public boolean isPot() {
      return this.fixType.isSelected("Pot");
   }

   public boolean isNethOP() {
      return this.fixType.isSelected("NethOP");
   }

   public boolean isSMP() {
      return this.fixType.isSelected("SMP");
   }

   public boolean isSword() {
      return this.fixType.isSelected("Sword");
   }

   public boolean isAxe() {
      return this.fixType.isSelected("Axe");
   }

   public boolean isMace() {
      return this.fixType.isSelected("Mace");
   }
}
