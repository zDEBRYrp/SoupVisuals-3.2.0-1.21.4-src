package padej.soup.implement.features.modules.other;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.item.HandAnimationEvent;
import padej.soup.api.event.events.item.HandOffsetEvent;
import padej.soup.api.event.events.item.HandScaleEvent;
import padej.soup.api.event.events.item.SwingDurationEvent;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.compat.ModCompatibility;
import padej.soup.base.util.other.Instance;

public class HandTweaks extends Module {
   private final BooleanSetting enableSwing = new BooleanSetting("setting.handtweaks.enableswing.name", "setting.handtweaks.enableswing.desc")
      .setValue(false)
      .visible(() -> !ModCompatibility.isHoldMyItemsLoaded());
   private final SelectSetting swingType = new SelectSetting("setting.handtweaks.swingtype.name", "setting.handtweaks.swingtype.desc")
      .value("Vanilla", "Swipe", "Down", "Smooth", "Power", "Feast", "Custom")
      .selected("Vanilla")
      .visible(() -> this.enableSwing.isValue() && !ModCompatibility.isHoldMyItemsLoaded());
   private final ValueSetting swingSpeed = new ValueSetting("setting.handtweaks.swingspeed.name", "setting.handtweaks.swingspeed.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .visible(() -> this.enableSwing.isValue() || ModCompatibility.isHoldMyItemsLoaded());
   private final ValueSetting customTranslateX = new ValueSetting("setting.handtweaks.customtranslatex.name", "setting.handtweaks.customtranslatex.desc")
      .setValue(0.56F)
      .range(-2.0F, 2.0F)
      .visible(() -> this.enableSwing.isValue() && this.swingType.getSelected().equals("Custom"));
   private final ValueSetting customTranslateY = new ValueSetting("setting.handtweaks.customtranslatey.name", "setting.handtweaks.customtranslatey.desc")
      .setValue(-0.32F)
      .range(-2.0F, 2.0F)
      .visible(() -> this.enableSwing.isValue() && this.swingType.getSelected().equals("Custom"));
   private final ValueSetting customTranslateZ = new ValueSetting("setting.handtweaks.customtranslatez.name", "setting.handtweaks.customtranslatez.desc")
      .setValue(-0.72F)
      .range(-2.0F, 2.0F)
      .visible(() -> this.enableSwing.isValue() && this.swingType.getSelected().equals("Custom"));
   private final ValueSetting customRotateX = new ValueSetting("setting.handtweaks.customrotatex.name", "setting.handtweaks.customrotatex.desc")
      .setValue(0.0F)
      .range(-180.0F, 180.0F)
      .visible(() -> this.enableSwing.isValue() && this.swingType.getSelected().equals("Custom"));
   private final ValueSetting customRotateY = new ValueSetting("setting.handtweaks.customrotatey.name", "setting.handtweaks.customrotatey.desc")
      .setValue(0.0F)
      .range(-180.0F, 180.0F)
      .visible(() -> this.enableSwing.isValue() && this.swingType.getSelected().equals("Custom"));
   private final ValueSetting customRotateZ = new ValueSetting("setting.handtweaks.customrotatez.name", "setting.handtweaks.customrotatez.desc")
      .setValue(0.0F)
      .range(-180.0F, 180.0F)
      .visible(() -> this.enableSwing.isValue() && this.swingType.getSelected().equals("Custom"));
   private final ValueSetting customSwingRotateX = new ValueSetting("setting.handtweaks.customswingrotatex.name", "setting.handtweaks.customswingrotatex.desc")
      .setValue(-60.0F)
      .range(-180.0F, 180.0F)
      .visible(() -> this.enableSwing.isValue() && this.swingType.getSelected().equals("Custom"));
   private final ValueSetting customSwingRotateY = new ValueSetting("setting.handtweaks.customswingrotatey.name", "setting.handtweaks.customswingrotatey.desc")
      .setValue(0.0F)
      .range(-180.0F, 180.0F)
      .visible(() -> this.enableSwing.isValue() && this.swingType.getSelected().equals("Custom"));
   private final ValueSetting customSwingRotateZ = new ValueSetting("setting.handtweaks.customswingrotatez.name", "setting.handtweaks.customswingrotatez.desc")
      .setValue(0.0F)
      .range(-180.0F, 180.0F)
      .visible(() -> this.enableSwing.isValue() && this.swingType.getSelected().equals("Custom"));
   private final ValueSetting mainHandX = new ValueSetting("setting.handtweaks.mainhandx.name", "setting.handtweaks.mainhandx.desc")
      .setValue(0.0F)
      .range(-1.0F, 1.0F);
   private final ValueSetting mainHandY = new ValueSetting("setting.handtweaks.mainhandy.name", "setting.handtweaks.mainhandy.desc")
      .setValue(0.0F)
      .range(-1.0F, 1.0F);
   private final ValueSetting mainHandZ = new ValueSetting("setting.handtweaks.mainhandz.name", "setting.handtweaks.mainhandz.desc")
      .setValue(0.0F)
      .range(-2.5F, 2.5F);
   private final ValueSetting mainHandPitch = new ValueSetting("setting.handtweaks.mainhandpitch.name", "setting.handtweaks.mainhandpitch.desc")
      .setValue(0.0F)
      .range(-180.0F, 180.0F);
   private final ValueSetting mainHandYaw = new ValueSetting("setting.handtweaks.mainhandyaw.name", "setting.handtweaks.mainhandyaw.desc")
      .setValue(0.0F)
      .range(-180.0F, 180.0F);
   private final ValueSetting mainHandRoll = new ValueSetting("setting.handtweaks.mainhandroll.name", "setting.handtweaks.mainhandroll.desc")
      .setValue(0.0F)
      .range(-180.0F, 180.0F);
   private final ValueSetting mainHandScale = new ValueSetting("setting.handtweaks.mainhandscale.name", "setting.handtweaks.mainhandscale.desc")
      .setValue(1.0F)
      .range(0.1F, 2.0F);
   private final ValueSetting offHandX = new ValueSetting("setting.handtweaks.offhandx.name", "setting.handtweaks.offhandx.desc")
      .setValue(0.0F)
      .range(-1.0F, 1.0F);
   private final ValueSetting offHandY = new ValueSetting("setting.handtweaks.offhandy.name", "setting.handtweaks.offhandy.desc")
      .setValue(0.0F)
      .range(-1.0F, 1.0F);
   private final ValueSetting offHandZ = new ValueSetting("setting.handtweaks.offhandz.name", "setting.handtweaks.offhandz.desc")
      .setValue(0.0F)
      .range(-2.5F, 2.5F);
   private final ValueSetting offHandPitch = new ValueSetting("setting.handtweaks.offhandpitch.name", "setting.handtweaks.offhandpitch.desc")
      .setValue(0.0F)
      .range(-180.0F, 180.0F);
   private final ValueSetting offHandYaw = new ValueSetting("setting.handtweaks.offhandyaw.name", "setting.handtweaks.offhandyaw.desc")
      .setValue(0.0F)
      .range(-180.0F, 180.0F);
   private final ValueSetting offHandRoll = new ValueSetting("setting.handtweaks.offhandroll.name", "setting.handtweaks.offhandroll.desc")
      .setValue(0.0F)
      .range(-180.0F, 180.0F);
   private final ValueSetting offHandScale = new ValueSetting("setting.handtweaks.offhandscale.name", "setting.handtweaks.offhandscale.desc")
      .setValue(1.0F)
      .range(0.1F, 2.0F);

   public static HandTweaks getInstance() {
      return Instance.get(HandTweaks.class);
   }

   public HandTweaks() {
      super("module.handtweaks.name", ModuleCategory.OTHER);
      GroupSetting swingGroup = new GroupSetting("group.handtweaks.swing.name", "group.handtweaks.swing.desc", false)
         .settings(this.enableSwing, this.swingType, this.swingSpeed);
      GroupSetting customTranslateGroup = new GroupSetting("group.handtweaks.customtranslate.name", "group.handtweaks.customtranslate.desc", false)
         .settings(this.customTranslateX, this.customTranslateY, this.customTranslateZ)
         .visible(() -> this.enableSwing.isValue() && this.swingType.getSelected().equals("Custom"));
      GroupSetting customRotateGroup = new GroupSetting("group.handtweaks.customrotate.name", "group.handtweaks.customrotate.desc", false)
         .settings(this.customRotateX, this.customRotateY, this.customRotateZ)
         .visible(() -> this.enableSwing.isValue() && this.swingType.getSelected().equals("Custom"));
      GroupSetting customSwingRotateGroup = new GroupSetting("group.handtweaks.customswingrotat.name", "group.handtweaks.customswingrotat.desc", false)
         .settings(this.customSwingRotateX, this.customSwingRotateY, this.customSwingRotateZ)
         .visible(() -> this.enableSwing.isValue() && this.swingType.getSelected().equals("Custom"));
      GroupSetting mainPositionGroup = new GroupSetting("group.handtweaks.mainposition.name", "group.handtweaks.mainposition.desc", false)
         .settings(this.mainHandX, this.mainHandY, this.mainHandZ);
      GroupSetting mainRotationGroup = new GroupSetting("group.handtweaks.mainrotation.name", "group.handtweaks.mainrotation.desc", false)
         .settings(this.mainHandPitch, this.mainHandYaw, this.mainHandRoll);
      GroupSetting offPositionGroup = new GroupSetting("group.handtweaks.offposition.name", "group.handtweaks.offposition.desc", false)
         .settings(this.offHandX, this.offHandY, this.offHandZ);
      GroupSetting offRotationGroup = new GroupSetting("group.handtweaks.offrotation.name", "group.handtweaks.offrotation.desc", false)
         .settings(this.offHandPitch, this.offHandYaw, this.offHandRoll);
      if (ModCompatibility.isHoldMyItemsLoaded()) {
         this.setup(new Setting[]{this.swingSpeed});
      } else {
         this.setup(
            new Setting[]{
               swingGroup,
               customTranslateGroup,
               customRotateGroup,
               customSwingRotateGroup,
               mainPositionGroup,
               mainRotationGroup,
               this.mainHandScale,
               offPositionGroup,
               offRotationGroup,
               this.offHandScale
            }
         );
      }
   }

   @EventHandler
   public void onSwingDuration(SwingDurationEvent event) {
      if (this.isEnabled() && this.enableSwing.isValue()) {
         event.setAnimation(this.swingSpeed.getValue());
         event.cancel();
      }
   }

   @EventHandler
   public void onHandScale(HandScaleEvent event) {
      if (this.isEnabled()) {
         Hand hand = event.getHand();
         if (hand.equals(Hand.MAIN_HAND)) {
            if (this.mainHandScale.getValue() != 1.0F) {
               event.setScale(this.mainHandScale.getValue());
            }
         } else if (this.offHandScale.getValue() != 1.0F) {
            event.setScale(this.offHandScale.getValue());
         }
      }
   }

   @EventHandler
   public void onHandAnimation(HandAnimationEvent event) {
      if (this.isEnabled() && this.enableSwing.isValue()) {
         if (event.getHand().equals(Hand.MAIN_HAND) && !this.swingType.getSelected().equals("Vanilla")) {
            MatrixStack matrix = event.getMatrices();
            float swingProgress = event.getSwingProgress();
            int i = mc.player.getMainArm().equals(Arm.RIGHT) ? 1 : -1;
            float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
            float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
            float sinSmooth = (float)(Math.sin(swingProgress * Math.PI) * 0.5);
            String var8 = this.swingType.getSelected();
            switch (var8) {
               case "Swipe":
                  matrix.translate(0.56F * i, -0.32F, -0.72F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(60 * i));
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60 * i));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * sin1 * -5.0F));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * sin1 * -120.0F));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
                  break;
               case "Down":
                  matrix.translate(i * 0.56F, -0.32F, -0.72F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0F));
                  matrix.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0F));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0F));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
                  break;
               case "Smooth":
                  matrix.translate(i * 0.56F, -0.42F, -0.72F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + sin1 * -20.0F)));
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * sin2 * -20.0F));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
                  matrix.translate(0.0, -0.1, 0.0);
                  break;
               case "Power":
                  matrix.translate(i * 0.56F, -0.32F, -0.72F);
                  matrix.translate(-sinSmooth * sinSmooth * sin1 * i, 0.0F, 0.0F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61 * i));
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * sin1 * -5.0F));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * sin1 * -30.0F));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60.0F));
                  break;
               case "Feast":
                  matrix.translate(i * 0.56F, -0.32F, -0.72F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75.0F * i));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45.0F));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35 * i));
                  break;
               case "Custom":
                  matrix.translate(i * this.customTranslateX.getValue(), this.customTranslateY.getValue(), this.customTranslateZ.getValue());
                  if (this.customRotateX.getValue() != 0.0F) {
                     matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.customRotateX.getValue()));
                  }

                  if (this.customRotateY.getValue() != 0.0F) {
                     matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.customRotateY.getValue() * i));
                  }

                  if (this.customRotateZ.getValue() != 0.0F) {
                     matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.customRotateZ.getValue() * i));
                  }

                  if (this.customSwingRotateX.getValue() != 0.0F) {
                     matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * this.customSwingRotateX.getValue()));
                  }

                  if (this.customSwingRotateY.getValue() != 0.0F) {
                     matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * this.customSwingRotateY.getValue() * i));
                  }

                  if (this.customSwingRotateZ.getValue() != 0.0F) {
                     matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * this.customSwingRotateZ.getValue() * i));
                  }
            }

            event.cancel();
         }
      }
   }

   @EventHandler
   public void onHandOffset(HandOffsetEvent event) {
      if (this.isEnabled()) {
         Hand hand = event.getHand();
         if (!hand.equals(Hand.MAIN_HAND) || !(event.getStack().getItem() instanceof CrossbowItem)) {
            MatrixStack matrix = event.getMatrices();
            if (hand.equals(Hand.MAIN_HAND)) {
               matrix.translate(this.mainHandX.getValue(), this.mainHandY.getValue(), this.mainHandZ.getValue());
               if (this.mainHandPitch.getValue() != 0.0F) {
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.mainHandPitch.getValue()));
               }

               if (this.mainHandYaw.getValue() != 0.0F) {
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.mainHandYaw.getValue()));
               }

               if (this.mainHandRoll.getValue() != 0.0F) {
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.mainHandRoll.getValue()));
               }
            } else {
               matrix.translate(this.offHandX.getValue(), this.offHandY.getValue(), this.offHandZ.getValue());
               if (this.offHandPitch.getValue() != 0.0F) {
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.offHandPitch.getValue()));
               }

               if (this.offHandYaw.getValue() != 0.0F) {
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.offHandYaw.getValue()));
               }

               if (this.offHandRoll.getValue() != 0.0F) {
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.offHandRoll.getValue()));
               }
            }
         }
      }
   }

   public BooleanSetting getEnableSwing() {
      return this.enableSwing;
   }

   public SelectSetting getSwingType() {
      return this.swingType;
   }

   public ValueSetting getSwingSpeed() {
      return this.swingSpeed;
   }

   public ValueSetting getCustomTranslateX() {
      return this.customTranslateX;
   }

   public ValueSetting getCustomTranslateY() {
      return this.customTranslateY;
   }

   public ValueSetting getCustomTranslateZ() {
      return this.customTranslateZ;
   }

   public ValueSetting getCustomRotateX() {
      return this.customRotateX;
   }

   public ValueSetting getCustomRotateY() {
      return this.customRotateY;
   }

   public ValueSetting getCustomRotateZ() {
      return this.customRotateZ;
   }

   public ValueSetting getCustomSwingRotateX() {
      return this.customSwingRotateX;
   }

   public ValueSetting getCustomSwingRotateY() {
      return this.customSwingRotateY;
   }

   public ValueSetting getCustomSwingRotateZ() {
      return this.customSwingRotateZ;
   }

   public ValueSetting getMainHandX() {
      return this.mainHandX;
   }

   public ValueSetting getMainHandY() {
      return this.mainHandY;
   }

   public ValueSetting getMainHandZ() {
      return this.mainHandZ;
   }

   public ValueSetting getMainHandPitch() {
      return this.mainHandPitch;
   }

   public ValueSetting getMainHandYaw() {
      return this.mainHandYaw;
   }

   public ValueSetting getMainHandRoll() {
      return this.mainHandRoll;
   }

   public ValueSetting getMainHandScale() {
      return this.mainHandScale;
   }

   public ValueSetting getOffHandX() {
      return this.offHandX;
   }

   public ValueSetting getOffHandY() {
      return this.offHandY;
   }

   public ValueSetting getOffHandZ() {
      return this.offHandZ;
   }

   public ValueSetting getOffHandPitch() {
      return this.offHandPitch;
   }

   public ValueSetting getOffHandYaw() {
      return this.offHandYaw;
   }

   public ValueSetting getOffHandRoll() {
      return this.offHandRoll;
   }

   public ValueSetting getOffHandScale() {
      return this.offHandScale;
   }
}
