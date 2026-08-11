package padej.soup.implement.features.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.joml.Vector2f;
import org.joml.Vector3f;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.render.shader.Shader;
import padej.soup.base.util.render.shader.ShaderHelper;

public class HandsShader extends Module {
   private final BooleanSetting dualShaderMode = new BooleanSetting("setting.handsshader.dualshadermode.name", "setting.handsshader.dualshadermode.desc")
      .setValue(true);
   private final SelectSetting mainShader = new SelectSetting("setting.handsshader.mainshader.name", "setting.handsshader.mainshader.desc")
      .value("None", "Solid", "Chroma", "Balatro", "Smoke", "Stripes", "Glow", "Glass", "Invert", "Snow")
      .selected("Glass");
   private final SelectSetting shader1 = new SelectSetting("setting.handsshader.shader1.name", "setting.handsshader.shader1.desc")
      .value("None", "Solid", "Chroma", "Balatro", "Smoke", "Stripes", "Glow", "Glass", "Invert", "Snow")
      .selected("Glow")
      .visible(this.dualShaderMode::isValue);
   private final SelectSetting shader2 = new SelectSetting("setting.handsshader.shader2.name", "setting.handsshader.shader2.desc")
      .value("None", "Solid", "Chroma", "Balatro", "Smoke", "Stripes", "Glow", "Glass", "Invert", "Snow")
      .selected("Smoke")
      .visible(this.dualShaderMode::isValue);
   private final SelectSetting blendMode = new SelectSetting("setting.handsshader.blendmode.name", "setting.handsshader.blendmode.desc")
      .value("Mix", "Add", "Multiply", "Screen", "Overlay", "Difference")
      .selected("Add")
      .visible(this.dualShaderMode::isValue);
   private final ValueSetting blendIntensity = new ValueSetting("setting.handsshader.blendintensity.name", "setting.handsshader.blendintensity.desc")
      .setValue(1.0F)
      .range(0.0F, 1.0F)
      .visible(this.dualShaderMode::isValue);
   private final ValueSetting shader1Strength = new ValueSetting("setting.handsshader.shader1strength.name", "setting.handsshader.shader1strength.desc")
      .setValue(1.0F)
      .range(0.0F, 1.0F)
      .visible(this.dualShaderMode::isValue);
   private final ValueSetting shader2Strength = new ValueSetting("setting.handsshader.shader2strength.name", "setting.handsshader.shader2strength.desc")
      .setValue(1.0F)
      .range(0.0F, 1.0F)
      .visible(this.dualShaderMode::isValue);
   private final ColorSetting solidColor1 = new ColorSetting("setting.handsshader.solidcolor1.name", "setting.handsshader.solidcolor1.desc")
      .value(-1499549)
      .visible(
         () -> this.mainShader.isSelected("Solid") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Solid") || this.shader2.isSelected("Solid"))
      );
   private final ColorSetting solidColor2 = new ColorSetting("setting.handsshader.solidcolor2.name", "setting.handsshader.solidcolor2.desc")
      .value(-6596170)
      .visible(
         () -> this.mainShader.isSelected("Solid") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Solid") || this.shader2.isSelected("Solid"))
      );
   private final ValueSetting gradientSpeed = new ValueSetting("setting.handsshader.gradientspeed.name", "setting.handsshader.gradientspeed.desc")
      .setValue(0.5F)
      .range(0.1F, 1.0F)
      .visible(
         () -> this.mainShader.isSelected("Solid") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Solid") || this.shader2.isSelected("Solid"))
      );
   private final ValueSetting chromaSpeed = new ValueSetting("setting.handsshader.chromaspeed.name", "setting.handsshader.chromaspeed.desc")
      .setValue(0.3F)
      .range(0.05F, 1.0F)
      .visible(
         () -> this.mainShader.isSelected("Chroma")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Chroma") || this.shader2.isSelected("Chroma"))
      );
   private final ValueSetting chromaSaturation = new ValueSetting("setting.handsshader.chromasaturation.name", "setting.handsshader.chromasaturation.desc")
      .setValue(0.5F)
      .range(0.1F, 1.0F)
      .visible(
         () -> this.mainShader.isSelected("Chroma")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Chroma") || this.shader2.isSelected("Chroma"))
      );
   private final ValueSetting chromaBrightness = new ValueSetting("setting.handsshader.chromabrightness.name", "setting.handsshader.chromabrightness.desc")
      .setValue(1.0F)
      .range(0.5F, 1.5F)
      .visible(
         () -> this.mainShader.isSelected("Chroma")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Chroma") || this.shader2.isSelected("Chroma"))
      );
   private final ValueSetting balatrospeed = new ValueSetting("setting.handsshader.balatrospeed.name", "setting.handsshader.balatrospeed.desc")
      .setValue(0.5F)
      .range(0.1F, 2.0F)
      .visible(
         () -> this.mainShader.isSelected("Balatro")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Balatro") || this.shader2.isSelected("Balatro"))
      );
   private final ColorSetting balatroMainColor = new ColorSetting("setting.handsshader.balatromaincolor.name", "setting.handsshader.balatromaincolor.desc")
      .value(-2210757)
      .visible(
         () -> this.mainShader.isSelected("Balatro")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Balatro") || this.shader2.isSelected("Balatro"))
      );
   private final ColorSetting balatroColor2 = new ColorSetting("setting.handsshader.balatrocolor2.name", "setting.handsshader.balatrocolor2.desc")
      .value(-16749644)
      .visible(
         () -> this.mainShader.isSelected("Balatro")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Balatro") || this.shader2.isSelected("Balatro"))
      );
   private final ColorSetting balatroColor3 = new ColorSetting("setting.handsshader.balatrocolor3.name", "setting.handsshader.balatrocolor3.desc")
      .value(-15326427)
      .visible(
         () -> this.mainShader.isSelected("Balatro")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Balatro") || this.shader2.isSelected("Balatro"))
      );
   private final ColorSetting balatroColor4 = new ColorSetting("setting.handsshader.balatrocolor4.name", "setting.handsshader.balatrocolor4.desc")
      .value(-10918400)
      .visible(
         () -> this.mainShader.isSelected("Balatro")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Balatro") || this.shader2.isSelected("Balatro"))
      );
   private final ColorSetting smokeColor = new ColorSetting("setting.handsshader.smokecolor.name", "setting.handsshader.smokecolor.desc")
      .value(-6969946)
      .visible(
         () -> this.mainShader.isSelected("Smoke") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Smoke") || this.shader2.isSelected("Smoke"))
      );
   private final ValueSetting smokeIntensity = new ValueSetting("setting.handsshader.smokeintensity.name", "setting.handsshader.smokeintensity.desc")
      .setValue(1.0F)
      .range(0.1F, 3.0F)
      .visible(
         () -> this.mainShader.isSelected("Smoke") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Smoke") || this.shader2.isSelected("Smoke"))
      );
   private final ValueSetting smokeSpeed = new ValueSetting("setting.handsshader.smokespeed.name", "setting.handsshader.smokespeed.desc")
      .setValue(0.5F)
      .range(0.1F, 2.0F)
      .visible(
         () -> this.mainShader.isSelected("Smoke") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Smoke") || this.shader2.isSelected("Smoke"))
      );
   private final ColorSetting stripesColor1 = new ColorSetting("setting.handsshader.stripescolor1.name", "setting.handsshader.stripescolor1.desc")
      .value(-8943479)
      .visible(
         () -> this.mainShader.isSelected("Stripes")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Stripes") || this.shader2.isSelected("Stripes"))
      );
   private final ColorSetting stripesColor2 = new ColorSetting("setting.handsshader.stripescolor2.name", "setting.handsshader.stripescolor2.desc")
      .value(-10853018)
      .visible(
         () -> this.mainShader.isSelected("Stripes")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Stripes") || this.shader2.isSelected("Stripes"))
      );
   private final ValueSetting stripesWidth = new ValueSetting("setting.handsshader.stripeswidth.name", "setting.handsshader.stripeswidth.desc")
      .setValue(0.3F)
      .range(0.05F, 0.5F)
      .visible(
         () -> this.mainShader.isSelected("Stripes")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Stripes") || this.shader2.isSelected("Stripes"))
      );
   private final ValueSetting stripesSpeed = new ValueSetting("setting.handsshader.stripesspeed.name", "setting.handsshader.stripesspeed.desc")
      .setValue(0.3F)
      .range(0.1F, 2.0F)
      .visible(
         () -> this.mainShader.isSelected("Stripes")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Stripes") || this.shader2.isSelected("Stripes"))
      );
   private final ColorSetting snowColor = new ColorSetting("setting.handsshader.snowcolor.name", "setting.handsshader.snowcolor.desc")
      .value(-1)
      .visible(
         () -> this.mainShader.isSelected("Snow") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Snow") || this.shader2.isSelected("Snow"))
      );
   private final ValueSetting snowIntensity = new ValueSetting("setting.handsshader.snowintensity.name", "setting.handsshader.snowintensity.desc")
      .setValue(1.0F)
      .range(0.1F, 3.0F)
      .visible(
         () -> this.mainShader.isSelected("Snow") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Snow") || this.shader2.isSelected("Snow"))
      );
   private final ValueSetting snowSpeed = new ValueSetting("setting.handsshader.snowspeed.name", "setting.handsshader.snowspeed.desc")
      .setValue(1.0F)
      .range(0.1F, 2.0F)
      .visible(
         () -> this.mainShader.isSelected("Snow") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Snow") || this.shader2.isSelected("Snow"))
      );
   private final BooleanSetting fillGlow = new BooleanSetting("setting.handsshader.fillglow.name", "setting.handsshader.fillglow.desc")
      .setValue(true)
      .visible(
         () -> this.mainShader.isSelected("Glow") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glow") || this.shader2.isSelected("Glow"))
      );
   private final ValueSetting glowRadius = new ValueSetting("setting.handsshader.glowradius.name", "setting.handsshader.glowradius.desc")
      .setValue(0.04F)
      .range(0.01F, 0.2F)
      .visible(
         () -> this.mainShader.isSelected("Glow") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glow") || this.shader2.isSelected("Glow"))
      );
   private final ValueSetting glowPower = new ValueSetting("setting.handsshader.glowpower.name", "setting.handsshader.glowpower.desc")
      .setValue(0.75F)
      .range(0.1F, 2.0F)
      .visible(
         () -> this.mainShader.isSelected("Glow") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glow") || this.shader2.isSelected("Glow"))
      );
   private final ColorSetting glowColor = new ColorSetting("setting.handsshader.glowcolor.name", "setting.handsshader.glowcolor.desc")
      .value(-13330213)
      .visible(
         () -> this.mainShader.isSelected("Glow") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glow") || this.shader2.isSelected("Glow"))
      );
   private final ValueSetting glowDispersion = new ValueSetting("setting.handsshader.glowdispersion.name", "setting.handsshader.glowdispersion.desc")
      .setValue(5.0F)
      .range(1.0F, 10.0F)
      .visible(
         () -> this.mainShader.isSelected("Glow") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glow") || this.shader2.isSelected("Glow"))
      );
   private final ValueSetting glassBlurSize = new ValueSetting("setting.handsshader.glassblursize.name", "setting.handsshader.glassblursize.desc")
      .setValue(50.0F)
      .range(5.0F, 200.0F)
      .visible(
         () -> this.mainShader.isSelected("Glass") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glass") || this.shader2.isSelected("Glass"))
      );
   private final ValueSetting glassQuality = new ValueSetting("setting.handsshader.glassquality.name", "setting.handsshader.glassquality.desc")
      .setValue(10.0F)
      .range(3.0F, 20.0F)
      .visible(
         () -> this.mainShader.isSelected("Glass") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glass") || this.shader2.isSelected("Glass"))
      );
   private final ValueSetting glassDirection = new ValueSetting("setting.handsshader.glassdirection.name", "setting.handsshader.glassdirection.desc")
      .setValue(10.0F)
      .range(4.0F, 16.0F)
      .visible(
         () -> this.mainShader.isSelected("Glass") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glass") || this.shader2.isSelected("Glass"))
      );
   private final ValueSetting glassRefraction = new ValueSetting("setting.handsshader.glassrefraction.name", "setting.handsshader.glassrefraction.desc")
      .setValue(1.0F)
      .range(0.0F, 3.0F)
      .visible(
         () -> this.mainShader.isSelected("Glass") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glass") || this.shader2.isSelected("Glass"))
      );
   private final ValueSetting glassBrightness = new ValueSetting("setting.handsshader.glassbrightness.name", "setting.handsshader.glassbrightness.desc")
      .setValue(1.1F)
      .range(0.5F, 2.0F)
      .visible(
         () -> this.mainShader.isSelected("Glass") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glass") || this.shader2.isSelected("Glass"))
      );
   private final BooleanSetting glassDistortion = new BooleanSetting("setting.handsshader.glassdistortion.name", "setting.handsshader.glassdistortion.desc")
      .setValue(false)
      .visible(
         () -> this.mainShader.isSelected("Glass") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glass") || this.shader2.isSelected("Glass"))
      );
   private final BooleanSetting glassHideHand = new BooleanSetting("setting.handsshader.glasshidehand.name", "setting.handsshader.glasshidehand.desc")
      .setValue(true)
      .visible(
         () -> this.mainShader.isSelected("Glass") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glass") || this.shader2.isSelected("Glass"))
      );
   private final BooleanSetting glassChromatic = new BooleanSetting("setting.handsshader.glasschromatic.name", "setting.handsshader.glasschromatic.desc")
      .setValue(true)
      .visible(
         () -> !this.glassHideHand.isValue()
            && (this.mainShader.isSelected("Glass") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glass") || this.shader2.isSelected("Glass")))
      );
   private final ValueSetting glassBackgroundBlur = new ValueSetting(
         "setting.handsshader.glassbackgroundblur.name", "setting.handsshader.glassbackgroundblur.desc"
      )
      .setValue(70.0F)
      .range(10, 100)
      .visible(
         () -> this.mainShader.isSelected("Glass") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glass") || this.shader2.isSelected("Glass"))
      );
   private final BooleanSetting invertHideHand = new BooleanSetting("setting.handsshader.inverthidehand.name", "setting.handsshader.inverthidehand.desc")
      .setValue(true)
      .visible(
         () -> this.mainShader.isSelected("Invert")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Invert") || this.shader2.isSelected("Invert"))
      );
   private final ValueSetting invertIntensity = new ValueSetting("setting.handsshader.invertintensity.name", "setting.handsshader.invertintensity.desc")
      .setValue(1.0F)
      .range(0.0F, 1.0F)
      .visible(
         () -> this.mainShader.isSelected("Invert")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Invert") || this.shader2.isSelected("Invert"))
      );
   private final BooleanSetting invertHue = new BooleanSetting("setting.handsshader.inverthue.name", "setting.handsshader.inverthue.desc")
      .setValue(false)
      .visible(
         () -> this.mainShader.isSelected("Invert")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Invert") || this.shader2.isSelected("Invert"))
      );
   private final BooleanSetting invertSaturation = new BooleanSetting("setting.handsshader.invertsaturation.name", "setting.handsshader.invertsaturation.desc")
      .setValue(false)
      .visible(
         () -> this.mainShader.isSelected("Invert")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Invert") || this.shader2.isSelected("Invert"))
      );
   private final BooleanSetting invertBrightness = new BooleanSetting("setting.handsshader.invertbrightness.name", "setting.handsshader.invertbrightness.desc")
      .setValue(false)
      .visible(
         () -> this.mainShader.isSelected("Invert")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Invert") || this.shader2.isSelected("Invert"))
      );
   private final ValueSetting invertEdgeGlow = new ValueSetting("setting.handsshader.invertedgeglow.name", "setting.handsshader.invertedgeglow.desc")
      .setValue(0.5F)
      .range(0.0F, 2.0F)
      .visible(
         () -> this.mainShader.isSelected("Invert")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Invert") || this.shader2.isSelected("Invert"))
      );
   private final ColorSetting invertGlowColor = new ColorSetting("setting.handsshader.invertglowcolor.name", "setting.handsshader.invertglowcolor.desc")
      .value(-16711681)
      .visible(
         () -> this.mainShader.isSelected("Invert")
            || this.dualShaderMode.isValue() && (this.shader1.isSelected("Invert") || this.shader2.isSelected("Invert"))
      );
   private static Vector2f cachedResolution = new Vector2f();
   private static int lastWidth = -1;
   private static int lastHeight = -1;

   public static HandsShader getInstance() {
      return Instance.get(HandsShader.class);
   }

   public HandsShader() {
      super("module.handsshader.name", ModuleCategory.VISUALS);
      GroupSetting modeGroup = new GroupSetting("group.handsshader.mode.name", "group.handsshader.mode.desc", false)
         .settings(this.mainShader, this.dualShaderMode);
      GroupSetting dualModeGroup = new GroupSetting("group.handsshader.dualmode.name", "group.handsshader.dualmode.desc", false)
         .settings(this.shader1, this.shader2, this.blendMode, this.blendIntensity, this.shader1Strength, this.shader2Strength)
         .visible(this.dualShaderMode::isValue);
      GroupSetting solidGroup = new GroupSetting("group.handsshader.solid.name", "group.handsshader.solid.desc", false)
         .settings(this.solidColor1, this.solidColor2, this.gradientSpeed)
         .visible(
            () -> this.mainShader.isSelected("Solid")
               || this.dualShaderMode.isValue() && (this.shader1.isSelected("Solid") || this.shader2.isSelected("Solid"))
         );
      GroupSetting chromaGroup = new GroupSetting("group.handsshader.chroma.name", "group.handsshader.chroma.desc", false)
         .settings(this.chromaSpeed, this.chromaSaturation, this.chromaBrightness)
         .visible(
            () -> this.mainShader.isSelected("Chroma")
               || this.dualShaderMode.isValue() && (this.shader1.isSelected("Chroma") || this.shader2.isSelected("Chroma"))
         );
      GroupSetting balatroGroup = new GroupSetting("group.handsshader.balatro.name", "group.handsshader.balatro.desc", false)
         .settings(this.balatrospeed, this.balatroMainColor, this.balatroColor2, this.balatroColor3, this.balatroColor4)
         .visible(
            () -> this.mainShader.isSelected("Balatro")
               || this.dualShaderMode.isValue() && (this.shader1.isSelected("Balatro") || this.shader2.isSelected("Balatro"))
         );
      GroupSetting smokeGroup = new GroupSetting("group.handsshader.smoke.name", "group.handsshader.smoke.desc", false)
         .settings(this.smokeColor, this.smokeIntensity, this.smokeSpeed)
         .visible(
            () -> this.mainShader.isSelected("Smoke")
               || this.dualShaderMode.isValue() && (this.shader1.isSelected("Smoke") || this.shader2.isSelected("Smoke"))
         );
      GroupSetting stripesGroup = new GroupSetting("group.handsshader.stripes.name", "group.handsshader.stripes.desc", false)
         .settings(this.stripesColor1, this.stripesColor2, this.stripesWidth, this.stripesSpeed)
         .visible(
            () -> this.mainShader.isSelected("Stripes")
               || this.dualShaderMode.isValue() && (this.shader1.isSelected("Stripes") || this.shader2.isSelected("Stripes"))
         );
      GroupSetting snowGroup = new GroupSetting("group.handsshader.snow.name", "group.handsshader.snow.desc", false)
         .settings(this.snowColor, this.snowIntensity, this.snowSpeed)
         .visible(
            () -> this.mainShader.isSelected("Snow") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Snow") || this.shader2.isSelected("Snow"))
         );
      GroupSetting glowGroup = new GroupSetting("group.handsshader.glow.name", "group.handsshader.glow.desc", false)
         .settings(this.fillGlow, this.glowRadius, this.glowPower, this.glowColor, this.glowDispersion)
         .visible(
            () -> this.mainShader.isSelected("Glow") || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glow") || this.shader2.isSelected("Glow"))
         );
      GroupSetting glassHandEffectGroup = new GroupSetting("group.handsshader.glass.handeffect.name", "group.handsshader.glass.handeffect.desc", false)
         .settings(this.glassBlurSize, this.glassQuality, this.glassDirection, this.glassBrightness)
         .visible(
            () -> this.mainShader.isSelected("Glass")
               || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glass") || this.shader2.isSelected("Glass"))
         );
      GroupSetting glassBackgroundEffectGroup = new GroupSetting(
            "group.handsshader.glass.backgroundeffect.name", "group.handsshader.glass.backgroundeffect.desc", false
         )
         .settings(this.glassBackgroundBlur)
         .visible(
            () -> this.mainShader.isSelected("Glass")
               || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glass") || this.shader2.isSelected("Glass"))
         );
      GroupSetting glassAdvancedGroup = new GroupSetting("group.handsshader.glass.advanced.name", "group.handsshader.glass.advanced.desc", false)
         .settings(this.glassRefraction, this.glassDistortion, this.glassChromatic)
         .visible(
            () -> this.mainShader.isSelected("Glass")
               || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glass") || this.shader2.isSelected("Glass"))
         );
      GroupSetting glassGroup = new GroupSetting("group.handsshader.glass.name", "group.handsshader.glass.desc", false)
         .settings(this.glassHideHand, glassHandEffectGroup, glassBackgroundEffectGroup, glassAdvancedGroup)
         .visible(
            () -> this.mainShader.isSelected("Glass")
               || this.dualShaderMode.isValue() && (this.shader1.isSelected("Glass") || this.shader2.isSelected("Glass"))
         );
      GroupSetting invertGroup = new GroupSetting("group.handsshader.invert.name", "group.handsshader.invert.desc", false)
         .settings(
            this.invertHideHand, this.invertIntensity, this.invertHue, this.invertSaturation, this.invertBrightness, this.invertEdgeGlow, this.invertGlowColor
         )
         .visible(
            () -> this.mainShader.isSelected("Invert")
               || this.dualShaderMode.isValue() && (this.shader1.isSelected("Invert") || this.shader2.isSelected("Invert"))
         );
      this.setup(
         new Setting[]{
            modeGroup, dualModeGroup, solidGroup, chromaGroup, balatroGroup, smokeGroup, stripesGroup, snowGroup, glowGroup, glassGroup, invertGroup
         }
      );
   }

   public void render() {
      if (ShaderHelper.isInitialized()) {
         try {
            if (!this.dualShaderMode.isValue() && this.mainShader.isSelected("None")) {
               return;
            }

            if (this.dualShaderMode.isValue()) {
               this.renderDualShader();
            } else {
               String e = this.mainShader.getSelected();
               switch (e) {
                  case "Solid":
                     this.renderSolid();
                     break;
                  case "Chroma":
                     this.renderChroma();
                     break;
                  case "Balatro":
                     this.renderBalatro();
                     break;
                  case "Smoke":
                     this.renderSmoke();
                     break;
                  case "Stripes":
                     this.renderStripes();
                     break;
                  case "Snow":
                     this.renderSnow();
                     break;
                  case "Glow":
                     this.renderGlow();
                     break;
                  case "Glass":
                     this.renderGlass();
                     break;
                  case "Invert":
                     this.renderInvert();
               }
            }
         } catch (Exception var3) {
            LoggerUtil.error("Error rendering hand shader: " + var3.getMessage());
         }
      }
   }

   private void prepareShaderRender(Framebuffer mainFbo, SimpleFramebuffer copyFbo) {
      copyFbo.clear();
      copyFbo.beginWrite(true);
      RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
      RenderSystem.clear(16640);
      mainFbo.draw(copyFbo.textureWidth, copyFbo.textureHeight);
      copyFbo.copyDepthFrom(mainFbo);
      mainFbo.beginWrite(true);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
   }

   private void bindShaderTextures(SimpleFramebuffer copyFbo) {
      RenderSystem.activeTexture(33984);
      RenderSystem.bindTexture(copyFbo.getColorAttachment());
      RenderSystem.activeTexture(33985);
      RenderSystem.bindTexture(copyFbo.getDepthAttachment());
   }

   private void cleanupShaderRender() {
      RenderSystem.activeTexture(33984);
      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
      if (mc.getFramebuffer() != null) {
         mc.getFramebuffer().beginWrite(true);
      }
   }

   private void renderChroma() {
      Framebuffer mainFbo = mc.getFramebuffer();
      Shader chromaShader = ShaderHelper.getChromaShader();
      SimpleFramebuffer copyFbo = ShaderHelper.getCopyFbo();
      if (mainFbo != null && chromaShader != null && copyFbo != null) {
         this.prepareShaderRender(mainFbo, copyFbo);
         this.bindShaderTextures(copyFbo);
         chromaShader.bind();
         chromaShader.setUniform1i("ColorTexture", 0);
         chromaShader.setUniform1i("DepthTexture", 1);
         float gameTime = (float)System.nanoTime() / 1.0E9F;
         chromaShader.setUniform1f("time", gameTime);
         chromaShader.setUniform1f("chromaSpeed", this.chromaSpeed.getValue());
         chromaShader.setUniform1f("chromaSaturation", this.chromaSaturation.getValue());
         chromaShader.setUniform1f("chromaBrightness", this.chromaBrightness.getValue());
         ShaderHelper.drawFullScreenQuad();
         chromaShader.unbind();
         this.cleanupShaderRender();
      }
   }

   private void renderSolid() {
      Framebuffer mainFbo = mc.getFramebuffer();
      Shader solidShader = ShaderHelper.getSolidShader();
      SimpleFramebuffer copyFbo = ShaderHelper.getCopyFbo();
      if (mainFbo != null && solidShader != null && copyFbo != null) {
         copyFbo.clear();
         copyFbo.beginWrite(true);
         RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         RenderSystem.clear(16640);
         mainFbo.draw(copyFbo.textureWidth, copyFbo.textureHeight);
         copyFbo.copyDepthFrom(mainFbo);
         mainFbo.beginWrite(true);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         RenderSystem.activeTexture(33984);
         RenderSystem.bindTexture(copyFbo.getColorAttachment());
         RenderSystem.activeTexture(33985);
         RenderSystem.bindTexture(copyFbo.getDepthAttachment());
         solidShader.bind();
         solidShader.setUniform1i("ColorTexture", 0);
         solidShader.setUniform1i("DepthTexture", 1);
         float gameTime = (float)System.nanoTime() / 1.0E9F * this.gradientSpeed.getValue();
         solidShader.setUniform1f("time", gameTime);
         Color color1 = new Color(this.solidColor1.getColor());
         Vector3f customColor1 = new Vector3f(color1.getRed() / 255.0F, color1.getGreen() / 255.0F, color1.getBlue() / 255.0F);
         solidShader.setUniform3f("customColor1", customColor1);
         Color color2 = new Color(this.solidColor2.getColor());
         Vector3f customColor2 = new Vector3f(color2.getRed() / 255.0F, color2.getGreen() / 255.0F, color2.getBlue() / 255.0F);
         solidShader.setUniform3f("customColor2", customColor2);
         float maxAlpha = Math.max(this.solidColor1.getAlpha(), this.solidColor2.getAlpha());
         solidShader.setUniform1f("effectAlpha", maxAlpha);
         ShaderHelper.drawFullScreenQuad();
         solidShader.unbind();
         RenderSystem.activeTexture(33984);
         RenderSystem.disableBlend();
         RenderSystem.enableDepthTest();
         if (mc.getFramebuffer() != null) {
            mc.getFramebuffer().beginWrite(true);
         }
      }
   }

   private void renderBalatro() {
      Framebuffer mainFbo = mc.getFramebuffer();
      Shader balatroShader = ShaderHelper.getBalatroShader();
      SimpleFramebuffer copyFbo = ShaderHelper.getCopyFbo();
      if (mainFbo != null && balatroShader != null && copyFbo != null) {
         copyFbo.clear();
         copyFbo.beginWrite(true);
         RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         RenderSystem.clear(16640);
         mainFbo.draw(copyFbo.textureWidth, copyFbo.textureHeight);
         copyFbo.copyDepthFrom(mainFbo);
         mainFbo.beginWrite(true);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         RenderSystem.activeTexture(33984);
         RenderSystem.bindTexture(copyFbo.getColorAttachment());
         RenderSystem.activeTexture(33985);
         RenderSystem.bindTexture(copyFbo.getDepthAttachment());
         balatroShader.bind();
         balatroShader.setUniform1i("ColorTexture", 0);
         balatroShader.setUniform1i("DepthTexture", 1);
         float gameTime = (float)System.nanoTime() / 1.0E9F * this.balatrospeed.getValue();
         balatroShader.setUniform1f("time", gameTime);
         Vector2f resolution = new Vector2f(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
         balatroShader.setUniform2f("resolution", resolution);
         Color color1 = new Color(this.balatroMainColor.getColor());
         balatroShader.setUniform3f("color1", new Vector3f(color1.getRed() / 255.0F, color1.getGreen() / 255.0F, color1.getBlue() / 255.0F));
         Color color2 = new Color(this.balatroColor2.getColor());
         balatroShader.setUniform3f("color2", new Vector3f(color2.getRed() / 255.0F, color2.getGreen() / 255.0F, color2.getBlue() / 255.0F));
         Color color3 = new Color(this.balatroColor3.getColor());
         balatroShader.setUniform3f("color3", new Vector3f(color3.getRed() / 255.0F, color3.getGreen() / 255.0F, color3.getBlue() / 255.0F));
         Color color4 = new Color(this.balatroColor4.getColor());
         balatroShader.setUniform3f("color4", new Vector3f(color4.getRed() / 255.0F, color4.getGreen() / 255.0F, color4.getBlue() / 255.0F));
         ShaderHelper.drawFullScreenQuad();
         balatroShader.unbind();
         RenderSystem.activeTexture(33984);
         RenderSystem.disableBlend();
         RenderSystem.enableDepthTest();
         if (mc.getFramebuffer() != null) {
            mc.getFramebuffer().beginWrite(true);
         }
      }
   }

   private void renderSmoke() {
      Framebuffer mainFbo = mc.getFramebuffer();
      Shader smokeShader = ShaderHelper.getSmokeShader();
      SimpleFramebuffer copyFbo = ShaderHelper.getCopyFbo();
      if (mainFbo != null && smokeShader != null && copyFbo != null) {
         copyFbo.clear();
         copyFbo.beginWrite(true);
         RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         RenderSystem.clear(16640);
         mainFbo.draw(copyFbo.textureWidth, copyFbo.textureHeight);
         copyFbo.copyDepthFrom(mainFbo);
         mainFbo.beginWrite(true);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         RenderSystem.activeTexture(33984);
         RenderSystem.bindTexture(copyFbo.getColorAttachment());
         RenderSystem.activeTexture(33985);
         RenderSystem.bindTexture(copyFbo.getDepthAttachment());
         smokeShader.bind();
         smokeShader.setUniform1i("ColorTexture", 0);
         smokeShader.setUniform1i("DepthTexture", 1);
         float gameTime = (float)System.nanoTime() / 1.0E9F * this.smokeSpeed.getValue();
         smokeShader.setUniform1f("time", gameTime);
         Vector2f resolution = new Vector2f(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
         smokeShader.setUniform2f("resolution", resolution);
         Color color = new Color(this.smokeColor.getColor());
         smokeShader.setUniform3f("smokeColor", new Vector3f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F));
         smokeShader.setUniform1f("smokeIntensity", this.smokeIntensity.getValue());
         ShaderHelper.drawFullScreenQuad();
         smokeShader.unbind();
         RenderSystem.activeTexture(33984);
         RenderSystem.disableBlend();
         RenderSystem.enableDepthTest();
         if (mc.getFramebuffer() != null) {
            mc.getFramebuffer().beginWrite(true);
         }
      }
   }

   private void renderStripes() {
      Framebuffer mainFbo = mc.getFramebuffer();
      Shader stripesShader = ShaderHelper.getStripesShader();
      SimpleFramebuffer copyFbo = ShaderHelper.getCopyFbo();
      if (mainFbo != null && stripesShader != null && copyFbo != null) {
         copyFbo.clear();
         copyFbo.beginWrite(true);
         RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         RenderSystem.clear(16640);
         mainFbo.draw(copyFbo.textureWidth, copyFbo.textureHeight);
         copyFbo.copyDepthFrom(mainFbo);
         mainFbo.beginWrite(true);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         RenderSystem.activeTexture(33984);
         RenderSystem.bindTexture(copyFbo.getColorAttachment());
         RenderSystem.activeTexture(33985);
         RenderSystem.bindTexture(copyFbo.getDepthAttachment());
         stripesShader.bind();
         stripesShader.setUniform1i("ColorTexture", 0);
         stripesShader.setUniform1i("DepthTexture", 1);
         float gameTime = (float)System.nanoTime() / 1.0E9F;
         stripesShader.setUniform1f("time", gameTime);
         Vector2f resolution = new Vector2f(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
         stripesShader.setUniform2f("resolution", resolution);
         Color color1 = new Color(this.stripesColor1.getColor());
         stripesShader.setUniform3f("stripesColor1", new Vector3f(color1.getRed() / 255.0F, color1.getGreen() / 255.0F, color1.getBlue() / 255.0F));
         Color color2 = new Color(this.stripesColor2.getColor());
         stripesShader.setUniform3f("stripesColor2", new Vector3f(color2.getRed() / 255.0F, color2.getGreen() / 255.0F, color2.getBlue() / 255.0F));
         stripesShader.setUniform1f("stripesWidth", this.stripesWidth.getValue());
         stripesShader.setUniform1f("stripesSpeed", this.stripesSpeed.getValue());
         ShaderHelper.drawFullScreenQuad();
         stripesShader.unbind();
         RenderSystem.activeTexture(33984);
         RenderSystem.disableBlend();
         RenderSystem.enableDepthTest();
         if (mc.getFramebuffer() != null) {
            mc.getFramebuffer().beginWrite(true);
         }
      }
   }

   private void renderSnow() {
      Framebuffer mainFbo = mc.getFramebuffer();
      Shader snowShader = ShaderHelper.getSnowShader();
      SimpleFramebuffer copyFbo = ShaderHelper.getCopyFbo();
      if (mainFbo != null && snowShader != null && copyFbo != null) {
         copyFbo.clear();
         copyFbo.beginWrite(true);
         RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         RenderSystem.clear(16640);
         mainFbo.draw(copyFbo.textureWidth, copyFbo.textureHeight);
         copyFbo.copyDepthFrom(mainFbo);
         mainFbo.beginWrite(true);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         RenderSystem.activeTexture(33984);
         RenderSystem.bindTexture(copyFbo.getColorAttachment());
         RenderSystem.activeTexture(33985);
         RenderSystem.bindTexture(copyFbo.getDepthAttachment());
         snowShader.bind();
         snowShader.setUniform1i("ColorTexture", 0);
         snowShader.setUniform1i("DepthTexture", 1);
         float gameTime = (float)System.nanoTime() / 1.0E9F;
         snowShader.setUniform1f("time", gameTime);
         Vector2f resolution = new Vector2f(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
         snowShader.setUniform2f("resolution", resolution);
         Color color = new Color(this.snowColor.getColor());
         snowShader.setUniform3f("snowColor", new Vector3f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F));
         snowShader.setUniform1f("snowIntensity", this.snowIntensity.getValue());
         snowShader.setUniform1f("snowSpeed", this.snowSpeed.getValue());
         ShaderHelper.drawFullScreenQuad();
         snowShader.unbind();
         RenderSystem.activeTexture(33984);
         RenderSystem.disableBlend();
         RenderSystem.enableDepthTest();
         if (mc.getFramebuffer() != null) {
            mc.getFramebuffer().beginWrite(true);
         }
      }
   }

   private void renderGlow() {
      Framebuffer mainFbo = mc.getFramebuffer();
      Shader glowShader = ShaderHelper.getGlowShader();
      SimpleFramebuffer copyFbo = ShaderHelper.getCopyFbo();
      if (mainFbo != null && glowShader != null && copyFbo != null) {
         copyFbo.clear();
         copyFbo.beginWrite(true);
         RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         RenderSystem.clear(16640);
         mainFbo.draw(copyFbo.textureWidth, copyFbo.textureHeight);
         copyFbo.copyDepthFrom(mainFbo);
         mainFbo.beginWrite(true);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         RenderSystem.activeTexture(33984);
         RenderSystem.bindTexture(copyFbo.getColorAttachment());
         RenderSystem.activeTexture(33985);
         RenderSystem.bindTexture(copyFbo.getDepthAttachment());
         glowShader.bind();
         glowShader.setUniform1i("ColorTexture", 0);
         glowShader.setUniform1i("DepthTexture", 1);
         float gameTime = (float)System.nanoTime() / 1.0E9F;
         glowShader.setUniform1f("time", gameTime);
         Vector2f resolution = new Vector2f(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
         glowShader.setUniform2f("resolution", resolution);
         glowShader.setUniformBool("fillGlow", this.fillGlow.isValue());
         glowShader.setUniform1f("glowRadius", this.glowRadius.getValue());
         glowShader.setUniform1f("glowPower", this.glowPower.getValue());
         glowShader.setUniform1f("glowDispersion", this.glowDispersion.getValue());
         Color glowColorRGB = new Color(this.glowColor.getColor());
         glowShader.setUniform3f("glowColor", new Vector3f(glowColorRGB.getRed() / 255.0F, glowColorRGB.getGreen() / 255.0F, glowColorRGB.getBlue() / 255.0F));
         ShaderHelper.drawFullScreenQuad();
         glowShader.unbind();
         RenderSystem.activeTexture(33984);
         RenderSystem.disableBlend();
         RenderSystem.enableDepthTest();
         if (mc.getFramebuffer() != null) {
            mc.getFramebuffer().beginWrite(true);
         }
      }
   }

   private void renderGlass() {
      Framebuffer mainFbo = mc.getFramebuffer();
      Shader glassShader = ShaderHelper.getGlassShader();
      SimpleFramebuffer copyFbo = ShaderHelper.getCopyFbo();
      if (mainFbo != null && glassShader != null && copyFbo != null) {
         copyFbo.clear();
         copyFbo.beginWrite(true);
         RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         RenderSystem.clear(16640);
         mainFbo.draw(copyFbo.textureWidth, copyFbo.textureHeight);
         copyFbo.copyDepthFrom(mainFbo);
         mainFbo.beginWrite(true);
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(1, 0, 1, 0);
         RenderSystem.disableDepthTest();
         RenderSystem.activeTexture(33984);
         RenderSystem.bindTexture(copyFbo.getColorAttachment());
         RenderSystem.activeTexture(33985);
         RenderSystem.bindTexture(copyFbo.getDepthAttachment());
         glassShader.bind();
         glassShader.setUniform1i("ColorTexture", 0);
         glassShader.setUniform1i("DepthTexture", 1);
         float gameTime = (float)System.nanoTime() / 1.0E9F;
         glassShader.setUniform1f("time", gameTime);
         Vector2f resolution = new Vector2f(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
         glassShader.setUniform2f("resolution", resolution);
         glassShader.setUniform1f("blurSize", this.glassBlurSize.getValue());
         glassShader.setUniform1f("quality", this.glassQuality.getValue());
         glassShader.setUniform1f("direction", this.glassDirection.getValue());
         glassShader.setUniform1f("refraction", this.glassRefraction.getValue());
         glassShader.setUniform1f("brightness", this.glassBrightness.getValue());
         glassShader.setUniformBool("enableChromatic", this.glassChromatic.isValue());
         glassShader.setUniformBool("enableDistortion", this.glassDistortion.isValue());
         glassShader.setUniformBool("hideHand", this.glassHideHand.isValue());
         glassShader.setUniform1f("backgroundBlur", this.glassBackgroundBlur.getValue());
         ShaderHelper.drawFullScreenQuad();
         glassShader.unbind();
         RenderSystem.activeTexture(33984);
         RenderSystem.disableBlend();
         RenderSystem.enableDepthTest();
         if (mc.getFramebuffer() != null) {
            mc.getFramebuffer().beginWrite(true);
         }
      }
   }

   private void renderInvert() {
      Framebuffer mainFbo = mc.getFramebuffer();
      Shader invertShader = ShaderHelper.getInvertShader();
      SimpleFramebuffer copyFbo = ShaderHelper.getCopyFbo();
      if (mainFbo != null && invertShader != null && copyFbo != null) {
         this.prepareShaderRender(mainFbo, copyFbo);
         this.bindShaderTextures(copyFbo);
         invertShader.bind();
         invertShader.setUniform1i("ColorTexture", 0);
         invertShader.setUniform1i("DepthTexture", 1);
         float gameTime = (float)System.nanoTime() / 1.0E9F;
         invertShader.setUniform1f("time", gameTime);
         Vector2f resolution = this.getResolution();
         invertShader.setUniform2f("resolution", resolution);
         invertShader.setUniformBool("hideHand", this.invertHideHand.isValue());
         invertShader.setUniform1f("invertIntensity", this.invertIntensity.getValue());
         invertShader.setUniformBool("invertHue", this.invertHue.isValue());
         invertShader.setUniformBool("invertSaturation", this.invertSaturation.isValue());
         invertShader.setUniformBool("invertBrightness", this.invertBrightness.isValue());
         invertShader.setUniform1f("edgeGlow", this.invertEdgeGlow.getValue());
         Color glowCol = new Color(this.invertGlowColor.getColor());
         invertShader.setUniform3f("glowColor", new Vector3f(glowCol.getRed() / 255.0F, glowCol.getGreen() / 255.0F, glowCol.getBlue() / 255.0F));
         ShaderHelper.drawFullScreenQuad();
         invertShader.unbind();
         this.cleanupShaderRender();
      }
   }

   private void renderDualShader() {
      if (!this.shader1.isSelected("None") || !this.shader2.isSelected("None")) {
         Framebuffer mainFbo = mc.getFramebuffer();
         SimpleFramebuffer copyFbo = ShaderHelper.getCopyFbo();
         SimpleFramebuffer shader1Fbo = ShaderHelper.getShader1Fbo();
         SimpleFramebuffer shader2Fbo = ShaderHelper.getShader2Fbo();
         Shader blendShader = ShaderHelper.getBlendShader();
         if (mainFbo != null && copyFbo != null && shader1Fbo != null && shader2Fbo != null && blendShader != null) {
            copyFbo.clear();
            copyFbo.beginWrite(true);
            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            RenderSystem.clear(16640);
            mainFbo.draw(copyFbo.textureWidth, copyFbo.textureHeight);
            copyFbo.copyDepthFrom(mainFbo);
            this.renderShaderToFramebuffer(this.shader1.getSelected(), shader1Fbo, copyFbo);
            this.renderShaderToFramebuffer(this.shader2.getSelected(), shader2Fbo, copyFbo);
            mainFbo.beginWrite(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.activeTexture(33984);
            RenderSystem.bindTexture(shader1Fbo.getColorAttachment());
            RenderSystem.activeTexture(33985);
            RenderSystem.bindTexture(shader2Fbo.getColorAttachment());
            RenderSystem.activeTexture(33986);
            RenderSystem.bindTexture(copyFbo.getDepthAttachment());
            blendShader.bind();
            blendShader.setUniform1i("Shader1Texture", 0);
            blendShader.setUniform1i("Shader2Texture", 1);
            blendShader.setUniform1i("DepthTexture", 2);
            int blendModeInt = 0;
            if (this.blendMode.isSelected("Add")) {
               blendModeInt = 1;
            } else if (this.blendMode.isSelected("Multiply")) {
               blendModeInt = 2;
            } else if (this.blendMode.isSelected("Screen")) {
               blendModeInt = 3;
            } else if (this.blendMode.isSelected("Overlay")) {
               blendModeInt = 4;
            } else if (this.blendMode.isSelected("Difference")) {
               blendModeInt = 5;
            }

            blendShader.setUniform1i("blendMode", blendModeInt);
            blendShader.setUniform1f("blendIntensity", this.blendIntensity.getValue());
            blendShader.setUniform1f("shader1Strength", this.shader1Strength.getValue());
            blendShader.setUniform1f("shader2Strength", this.shader2Strength.getValue());
            ShaderHelper.drawFullScreenQuad();
            blendShader.unbind();
            RenderSystem.activeTexture(33984);
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            if (mc.getFramebuffer() != null) {
               mc.getFramebuffer().beginWrite(true);
            }
         }
      }
   }

   private void renderShaderToFramebuffer(String shaderName, SimpleFramebuffer targetFbo, SimpleFramebuffer sourceFbo) {
      if (shaderName.equals("None")) {
         targetFbo.clear();
         targetFbo.beginWrite(true);
         RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         RenderSystem.clear(16640);
         sourceFbo.draw(targetFbo.textureWidth, targetFbo.textureHeight);
         targetFbo.copyDepthFrom(sourceFbo);
      } else {
         Shader shader = this.getShaderByName(shaderName);
         if (shader != null) {
            targetFbo.clear();
            targetFbo.beginWrite(true);
            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            RenderSystem.clear(16640);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.activeTexture(33984);
            RenderSystem.bindTexture(sourceFbo.getColorAttachment());
            RenderSystem.activeTexture(33985);
            RenderSystem.bindTexture(sourceFbo.getDepthAttachment());
            shader.bind();
            shader.setUniform1i("ColorTexture", 0);
            shader.setUniform1i("DepthTexture", 1);
            float gameTime = (float)System.nanoTime() / 1.0E9F;
            shader.setUniform1f("time", gameTime);
            this.setShaderUniforms(shader, shaderName, gameTime);
            ShaderHelper.drawFullScreenQuad();
            shader.unbind();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
         }
      }
   }

   private Shader getShaderByName(String name) {
      return switch (name) {
         case "Chroma" -> ShaderHelper.getChromaShader();
         case "Solid" -> ShaderHelper.getSolidShader();
         case "Balatro" -> ShaderHelper.getBalatroShader();
         case "Smoke" -> ShaderHelper.getSmokeShader();
         case "Stripes" -> ShaderHelper.getStripesShader();
         case "Snow" -> ShaderHelper.getSnowShader();
         case "Glow" -> ShaderHelper.getGlowShader();
         case "Glass" -> ShaderHelper.getGlassShader();
         case "Invert" -> ShaderHelper.getInvertShader();
         default -> null;
      };
   }

   private Vector2f getResolution() {
      int width = mc.getWindow().getFramebufferWidth();
      int height = mc.getWindow().getFramebufferHeight();
      if (width != lastWidth || height != lastHeight) {
         cachedResolution.set(width, height);
         lastWidth = width;
         lastHeight = height;
      }

      return cachedResolution;
   }

   private void setShaderUniforms(Shader shader, String shaderName, float gameTime) {
      Vector2f resolution = this.getResolution();
      switch (shaderName) {
         case "Chroma":
            shader.setUniform1f("chromaSpeed", this.chromaSpeed.getValue());
            shader.setUniform1f("chromaSaturation", this.chromaSaturation.getValue());
            shader.setUniform1f("chromaBrightness", this.chromaBrightness.getValue());
            break;
         case "Solid":
            shader.setUniform1f("time", gameTime * this.gradientSpeed.getValue());
            Color color1 = new Color(this.solidColor1.getColor());
            shader.setUniform3f("customColor1", new Vector3f(color1.getRed() / 255.0F, color1.getGreen() / 255.0F, color1.getBlue() / 255.0F));
            Color color2 = new Color(this.solidColor2.getColor());
            shader.setUniform3f("customColor2", new Vector3f(color2.getRed() / 255.0F, color2.getGreen() / 255.0F, color2.getBlue() / 255.0F));
            float maxAlpha = Math.max(this.solidColor1.getAlpha(), this.solidColor2.getAlpha());
            shader.setUniform1f("effectAlpha", maxAlpha);
            break;
         case "Balatro":
            shader.setUniform1f("time", gameTime * this.balatrospeed.getValue());
            shader.setUniform2f("resolution", resolution);
            Color bColor1 = new Color(this.balatroMainColor.getColor());
            shader.setUniform3f("color1", new Vector3f(bColor1.getRed() / 255.0F, bColor1.getGreen() / 255.0F, bColor1.getBlue() / 255.0F));
            Color bColor2 = new Color(this.balatroColor2.getColor());
            shader.setUniform3f("color2", new Vector3f(bColor2.getRed() / 255.0F, bColor2.getGreen() / 255.0F, bColor2.getBlue() / 255.0F));
            Color bColor3 = new Color(this.balatroColor3.getColor());
            shader.setUniform3f("color3", new Vector3f(bColor3.getRed() / 255.0F, bColor3.getGreen() / 255.0F, bColor3.getBlue() / 255.0F));
            Color bColor4 = new Color(this.balatroColor4.getColor());
            shader.setUniform3f("color4", new Vector3f(bColor4.getRed() / 255.0F, bColor4.getGreen() / 255.0F, bColor4.getBlue() / 255.0F));
            break;
         case "Smoke":
            shader.setUniform1f("time", gameTime * this.smokeSpeed.getValue());
            shader.setUniform2f("resolution", resolution);
            Color smokeCol = new Color(this.smokeColor.getColor());
            shader.setUniform3f("smokeColor", new Vector3f(smokeCol.getRed() / 255.0F, smokeCol.getGreen() / 255.0F, smokeCol.getBlue() / 255.0F));
            shader.setUniform1f("smokeIntensity", this.smokeIntensity.getValue());
            break;
         case "Stripes":
            shader.setUniform2f("resolution", resolution);
            Color stripesCol1 = new Color(this.stripesColor1.getColor());
            shader.setUniform3f("stripesColor1", new Vector3f(stripesCol1.getRed() / 255.0F, stripesCol1.getGreen() / 255.0F, stripesCol1.getBlue() / 255.0F));
            Color stripesCol2 = new Color(this.stripesColor2.getColor());
            shader.setUniform3f("stripesColor2", new Vector3f(stripesCol2.getRed() / 255.0F, stripesCol2.getGreen() / 255.0F, stripesCol2.getBlue() / 255.0F));
            shader.setUniform1f("stripesWidth", this.stripesWidth.getValue());
            shader.setUniform1f("stripesSpeed", this.stripesSpeed.getValue());
            break;
         case "Snow":
            shader.setUniform2f("resolution", resolution);
            Color snowCol = new Color(this.snowColor.getColor());
            shader.setUniform3f("snowColor", new Vector3f(snowCol.getRed() / 255.0F, snowCol.getGreen() / 255.0F, snowCol.getBlue() / 255.0F));
            shader.setUniform1f("snowIntensity", this.snowIntensity.getValue());
            shader.setUniform1f("snowSpeed", this.snowSpeed.getValue());
            break;
         case "Glow":
            shader.setUniform2f("resolution", resolution);
            shader.setUniformBool("fillGlow", this.fillGlow.isValue());
            shader.setUniform1f("glowRadius", this.glowRadius.getValue());
            shader.setUniform1f("glowPower", this.glowPower.getValue());
            shader.setUniform1f("glowDispersion", this.glowDispersion.getValue());
            Color glowCol = new Color(this.glowColor.getColor());
            shader.setUniform3f("glowColor", new Vector3f(glowCol.getRed() / 255.0F, glowCol.getGreen() / 255.0F, glowCol.getBlue() / 255.0F));
            break;
         case "Glass":
            shader.setUniform2f("resolution", resolution);
            shader.setUniform1f("blurSize", this.glassBlurSize.getValue());
            shader.setUniform1f("quality", this.glassQuality.getValue());
            shader.setUniform1f("direction", this.glassDirection.getValue());
            shader.setUniform1f("refraction", this.glassRefraction.getValue());
            shader.setUniform1f("brightness", this.glassBrightness.getValue());
            shader.setUniformBool("enableChromatic", this.glassChromatic.isValue());
            shader.setUniformBool("enableDistortion", this.glassDistortion.isValue());
            shader.setUniformBool("hideHand", this.glassHideHand.isValue());
            shader.setUniform1f("backgroundBlur", this.glassBackgroundBlur.getValue());
            break;
         case "Invert":
            shader.setUniform2f("resolution", resolution);
            shader.setUniformBool("hideHand", this.invertHideHand.isValue());
            shader.setUniform1f("invertIntensity", this.invertIntensity.getValue());
            shader.setUniformBool("invertHue", this.invertHue.isValue());
            shader.setUniformBool("invertSaturation", this.invertSaturation.isValue());
            shader.setUniformBool("invertBrightness", this.invertBrightness.isValue());
            shader.setUniform1f("edgeGlow", this.invertEdgeGlow.getValue());
            Color invertGlowCol = new Color(this.invertGlowColor.getColor());
            shader.setUniform3f("glowColor", new Vector3f(invertGlowCol.getRed() / 255.0F, invertGlowCol.getGreen() / 255.0F, invertGlowCol.getBlue() / 255.0F));
      }
   }

   public BooleanSetting getDualShaderMode() {
      return this.dualShaderMode;
   }

   public SelectSetting getMainShader() {
      return this.mainShader;
   }

   public SelectSetting getShader1() {
      return this.shader1;
   }

   public SelectSetting getShader2() {
      return this.shader2;
   }

   public SelectSetting getBlendMode() {
      return this.blendMode;
   }

   public ValueSetting getBlendIntensity() {
      return this.blendIntensity;
   }

   public ValueSetting getShader1Strength() {
      return this.shader1Strength;
   }

   public ValueSetting getShader2Strength() {
      return this.shader2Strength;
   }

   public ColorSetting getSolidColor1() {
      return this.solidColor1;
   }

   public ColorSetting getSolidColor2() {
      return this.solidColor2;
   }

   public ValueSetting getGradientSpeed() {
      return this.gradientSpeed;
   }

   public ValueSetting getChromaSpeed() {
      return this.chromaSpeed;
   }

   public ValueSetting getChromaSaturation() {
      return this.chromaSaturation;
   }

   public ValueSetting getChromaBrightness() {
      return this.chromaBrightness;
   }

   public ValueSetting getBalatrospeed() {
      return this.balatrospeed;
   }

   public ColorSetting getBalatroMainColor() {
      return this.balatroMainColor;
   }

   public ColorSetting getBalatroColor2() {
      return this.balatroColor2;
   }

   public ColorSetting getBalatroColor3() {
      return this.balatroColor3;
   }

   public ColorSetting getBalatroColor4() {
      return this.balatroColor4;
   }

   public ColorSetting getSmokeColor() {
      return this.smokeColor;
   }

   public ValueSetting getSmokeIntensity() {
      return this.smokeIntensity;
   }

   public ValueSetting getSmokeSpeed() {
      return this.smokeSpeed;
   }

   public ColorSetting getStripesColor1() {
      return this.stripesColor1;
   }

   public ColorSetting getStripesColor2() {
      return this.stripesColor2;
   }

   public ValueSetting getStripesWidth() {
      return this.stripesWidth;
   }

   public ValueSetting getStripesSpeed() {
      return this.stripesSpeed;
   }

   public ColorSetting getSnowColor() {
      return this.snowColor;
   }

   public ValueSetting getSnowIntensity() {
      return this.snowIntensity;
   }

   public ValueSetting getSnowSpeed() {
      return this.snowSpeed;
   }

   public BooleanSetting getFillGlow() {
      return this.fillGlow;
   }

   public ValueSetting getGlowRadius() {
      return this.glowRadius;
   }

   public ValueSetting getGlowPower() {
      return this.glowPower;
   }

   public ColorSetting getGlowColor() {
      return this.glowColor;
   }

   public ValueSetting getGlowDispersion() {
      return this.glowDispersion;
   }

   public ValueSetting getGlassBlurSize() {
      return this.glassBlurSize;
   }

   public ValueSetting getGlassQuality() {
      return this.glassQuality;
   }

   public ValueSetting getGlassDirection() {
      return this.glassDirection;
   }

   public ValueSetting getGlassRefraction() {
      return this.glassRefraction;
   }

   public ValueSetting getGlassBrightness() {
      return this.glassBrightness;
   }

   public BooleanSetting getGlassDistortion() {
      return this.glassDistortion;
   }

   public BooleanSetting getGlassHideHand() {
      return this.glassHideHand;
   }

   public BooleanSetting getGlassChromatic() {
      return this.glassChromatic;
   }

   public ValueSetting getGlassBackgroundBlur() {
      return this.glassBackgroundBlur;
   }

   public BooleanSetting getInvertHideHand() {
      return this.invertHideHand;
   }

   public ValueSetting getInvertIntensity() {
      return this.invertIntensity;
   }

   public BooleanSetting getInvertHue() {
      return this.invertHue;
   }

   public BooleanSetting getInvertSaturation() {
      return this.invertSaturation;
   }

   public BooleanSetting getInvertBrightness() {
      return this.invertBrightness;
   }

   public ValueSetting getInvertEdgeGlow() {
      return this.invertEdgeGlow;
   }

   public ColorSetting getInvertGlowColor() {
      return this.invertGlowColor;
   }
}
