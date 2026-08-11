package padej.soup.implement.features.modules.world;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.player.TickEvent;
import padej.soup.api.event.events.render.DrawEvent;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.api.system.shape.implement.Image;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.ProjectionUtil;
import padej.soup.base.util.other.Instance;

public class Gps extends Module {
   private final Animation radiusAnim = new DecelerateAnimation().setMs(150).setValue(12.0);
   private BlockPos targetPos = null;
   private boolean ignoreY = false;
   private final SelectSetting anchorType = new SelectSetting("setting.gps.anchor.name", "setting.gps.anchor.desc")
      .value("Point", "Crosshair")
      .selected("Point");
   private final SelectSetting arrowType = new SelectSetting("setting.gps.arrowtype.name", "setting.gps.arrowtype.desc")
      .value("Fill", "Outline")
      .selected("Fill");
   private final ValueSetting arrowSize = new ValueSetting("setting.gps.arrowsize.name", "setting.gps.arrowsize.desc").setValue(16.0F).range(8, 32);
   private final ValueSetting radiusSetting = new ValueSetting("setting.gps.arrowradius.name", "setting.gps.arrowradius.desc")
      .setValue(50.0F)
      .range(30, 100)
      .visible(() -> this.anchorType.isSelected("Crosshair"));
   private final ValueSetting markerSize = new ValueSetting("setting.gps.markersize.name", "setting.gps.markersize.desc").setValue(16.0F).range(8, 32);
   private final BooleanSetting showDistance = new BooleanSetting("setting.gps.showdistance.name", "setting.gps.showdistance.desc").setValue(false);
   private final SelectSetting distancePosition = new SelectSetting("setting.gps.distanceposition.name", "setting.gps.distanceposition.desc")
      .value("Arrow", "Location Icon")
      .selected("Arrow");
   private final SelectSetting distanceTextPosition = new SelectSetting("setting.gps.distancetextposition.name", "setting.gps.distancetextposition.desc")
      .value("Above", "Follow", "Bottom")
      .selected("Bottom")
      .visible(() -> this.showDistance.isValue() && this.distancePosition.isSelected("Arrow") && this.anchorType.isSelected("Point"));
   private final ColorSetting distanceColor = new ColorSetting("setting.gps.distancecolor.name", "setting.gps.distancecolor.desc").value(-1);
   private final BooleanSetting showBeam = new BooleanSetting("setting.gps.showbeam.name", "setting.gps.showbeam.desc").setValue(true);
   private final BooleanSetting autoDelete = new BooleanSetting("setting.gps.autodelete.name", "setting.gps.autodelete.desc").setValue(false);
   private final ValueSetting autoDeleteDistance = new ValueSetting("setting.gps.autodeletedistance.name", "setting.gps.autodeletedistance.desc")
      .setValue(5.0F)
      .range(1, 10)
      .visible(this.autoDelete::isValue);

   public static Gps getInstance() {
      return Instance.get(Gps.class);
   }

   public Gps() {
      super("module.gps.name", ModuleCategory.WORLD);
      GroupSetting arrowGroup = new GroupSetting("group.gps.arrow.name", "group.gps.arrow.desc", false)
         .settings(this.arrowType, this.arrowSize, this.radiusSetting);
      GroupSetting markerGroup = new GroupSetting("group.gps.marker.name", "group.gps.marker.desc", false).settings(this.showBeam, this.markerSize);
      GroupSetting distanceGroup = new GroupSetting("group.gps.distance.name", "group.gps.distance.desc", false)
         .settings(this.showDistance, this.distancePosition, this.distanceTextPosition, this.distanceColor);
      GroupSetting autoDeleteGroup = new GroupSetting("group.gps.autodelete.name", "group.gps.autodelete.desc", false)
         .settings(this.autoDelete, this.autoDeleteDistance);
      this.setup(new Setting[]{this.anchorType, arrowGroup, markerGroup, distanceGroup, autoDeleteGroup});
      this.registerCommands();
   }

   private void registerCommands() {
      ClientCommandRegistrationCallback.EVENT
         .register(
            (ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> dispatcher.register(
               (LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommandManager.literal("gps")
                     .then(ClientCommandManager.literal("clear").executes(context -> {
                        this.targetPos = null;
                        this.ignoreY = false;
                        String message = LocalizationManager.getInstance().get("message.gps.cleared");
                        ((FabricClientCommandSource)context.getSource()).sendFeedback(Text.of("§a" + message));
                        return 1;
                     })))
                  .then(
                     ClientCommandManager.argument("x", IntegerArgumentType.integer())
                        .then(
                           ((RequiredArgumentBuilder)ClientCommandManager.argument("y", IntegerArgumentType.integer())
                                 .then(ClientCommandManager.argument("z", IntegerArgumentType.integer()).executes(context -> {
                                    int x = IntegerArgumentType.getInteger(context, "x");
                                    int y = IntegerArgumentType.getInteger(context, "y");
                                    int z = IntegerArgumentType.getInteger(context, "z");
                                    this.targetPos = new BlockPos(x, y, z);
                                    this.ignoreY = false;
                                    String message = LocalizationManager.getInstance().getFormatted("message.gps.set", x, y, z);
                                    ((FabricClientCommandSource)context.getSource()).sendFeedback(Text.of("§a" + message));
                                    return 1;
                                 })))
                              .executes(context -> {
                                 int x = IntegerArgumentType.getInteger(context, "x");
                                 int z = IntegerArgumentType.getInteger(context, "y");
                                 this.targetPos = new BlockPos(x, 0, z);
                                 this.ignoreY = true;
                                 String message = LocalizationManager.getInstance().getFormatted("message.gps.setnoheight", x, z);
                                 ((FabricClientCommandSource)context.getSource()).sendFeedback(Text.of("§a" + message));
                                 return 1;
                              })
                        )
                  )
            )
         );
   }

   @EventHandler
   public void onTick(TickEvent e) {
      this.radiusAnim.setDirection(mc.player.isSprinting() ? Direction.FORWARDS : Direction.BACKWARDS);
      if (this.autoDelete.isValue() && this.targetPos != null) {
         float distance = this.getDistanceToTarget();
         if (distance <= this.autoDeleteDistance.getValue()) {
            this.targetPos = null;
            this.ignoreY = false;
            if (mc.player != null) {
               String message = LocalizationManager.getInstance().get("message.gps.targetreached");
               mc.player.sendMessage(Text.of("§a" + message), false);
            }
         }
      }
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (this.targetPos != null) {
         MatrixStack matrix = e.getDrawContext().getMatrices();
         float middleW = mc.getWindow().getScaledWidth() / 2.0F;
         float middleH = mc.getWindow().getScaledHeight() / 2.0F;
         float size = this.arrowSize.getValue();
         float anchorX;
         float anchorY;
         float posY;
         if (this.anchorType.isSelected("Point")) {
            anchorX = middleW;
            anchorY = middleH / 2.0F;
            posY = anchorY;
         } else {
            anchorX = middleW;
            anchorY = middleH;
            float radius = this.radiusSetting.getValue() + this.radiusAnim.getOutput().floatValue();
            posY = middleH - radius;
         }

         if (!mc.options.hudHidden && mc.options.getPerspective().equals(Perspective.FIRST_PERSON)) {
            float yaw = this.getRotationToTarget() - this.getInterpolatedYaw();
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShaderTexture(0, this.getArrowTexture());
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            int color = ColorUtil.getClientColor();
            matrix.push();
            matrix.translate(anchorX, anchorY, 0.0F);
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
            matrix.translate(-anchorX, -anchorY, 0.0F);
            Matrix4f matrix4f = matrix.peek().getPositionMatrix();
            buffer.vertex(matrix4f, anchorX - size / 2.0F, posY + size, 0.0F)
               .texture(0.0F, 1.0F)
               .color(ColorUtil.multAlpha(ColorUtil.multDark(color, 0.4F), 0.5F));
            buffer.vertex(matrix4f, anchorX + size / 2.0F, posY + size, 0.0F)
               .texture(1.0F, 1.0F)
               .color(ColorUtil.multAlpha(ColorUtil.multDark(color, 0.4F), 0.5F));
            buffer.vertex(matrix4f, anchorX + size / 2.0F, posY, 0.0F).texture(1.0F, 0.0F).color(color);
            buffer.vertex(matrix4f, anchorX - size / 2.0F, posY, 0.0F).texture(0.0F, 0.0F).color(color);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            if (this.showDistance.isValue() && this.distancePosition.isSelected("Arrow")) {
               this.renderDistanceOnArrow(matrix, anchorX, anchorY, posY, size, yaw);
            }

            matrix.pop();
         }

         if (this.showBeam.isValue()) {
            this.renderLocationIcon(matrix);
         }
      }
   }

   private void renderDistanceOnArrow(MatrixStack matrix, float anchorX, float anchorY, float posY, float size, float yaw) {
      float distance = this.getDistanceToTarget();
      String distanceText = String.format("%.1fm", distance);
      float textWidth = mc.textRenderer.getWidth(distanceText);
      if (this.anchorType.isSelected("Point")) {
         float textX = anchorX - textWidth / 2.0F;
         String var12 = this.distanceTextPosition.getSelected();
         switch (var12) {
            case "Above": {
               float textY = posY - 10.0F;
               Fonts.getSize(15, Fonts.Type.INTER_BOLD).drawString(matrix, distanceText, textX, textY, this.distanceColor.getColor());
               break;
            }
            case "Follow": {
               float textY = posY + size + 4.0F;
               Fonts.getSize(15, Fonts.Type.INTER_BOLD).drawString(matrix, distanceText, textX, textY, this.distanceColor.getColor());
               break;
            }
            case "Bottom": {
               matrix.pop();
               float textY = posY + size + 4.0F;
               Fonts.getSize(15, Fonts.Type.INTER_BOLD).drawString(matrix, distanceText, textX, textY, this.distanceColor.getColor());
               matrix.push();
               matrix.translate(anchorX, anchorY, 0.0F);
               matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
               matrix.translate(-anchorX, -anchorY, 0.0F);
            }
         }
      } else {
         float textX = anchorX - textWidth / 2.0F;
         float textY = posY + size + 4.0F;
         Fonts.getSize(15, Fonts.Type.INTER_BOLD).drawString(matrix, distanceText, textX, textY, this.distanceColor.getColor());
      }
   }

   private void renderLocationIcon(MatrixStack matrix) {
      float yOffset = mc.player.getHeight() / 2.0F;
      double targetY;
      if (this.ignoreY) {
         targetY = MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(false), mc.player.prevY + yOffset, mc.player.getY() + yOffset);
      } else {
         targetY = this.targetPos.getY() + 0.5;
      }

      Vec3d targetWorldPos = new Vec3d(this.targetPos.getX() + 0.5, targetY, this.targetPos.getZ() + 0.5);
      Vec3d screenPos = ProjectionUtil.worldSpaceToScreenSpace(targetWorldPos);
      if (screenPos.z > 0.0) {
         float screenWidth = mc.getWindow().getScaledWidth();
         float screenHeight = mc.getWindow().getScaledHeight();
         float margin = 100.0F;
         if (screenPos.x >= -margin && screenPos.x <= screenWidth + margin && screenPos.y >= -margin && screenPos.y <= screenHeight + margin) {
            float size = this.markerSize.getValue();
            float x = (float)screenPos.x;
            float y = (float)screenPos.y;
            RenderSystem.disableDepthTest();
            matrix.push();
            int color = ColorUtil.getClientColor();
            new Image().setIcon(61451).render(ShapeProperties.create(matrix, x - size / 2.0F, y, size, size).color(color).build());
            RenderSystem.enableDepthTest();
            if (this.showDistance.isValue() && this.distancePosition.isSelected("Location Icon")) {
               float distance = this.getDistanceToTarget();
               String distanceText = String.format("%.1fm", distance);
               float textWidth = mc.textRenderer.getWidth(distanceText);
               float textX = x - textWidth / 2.0F;
               float textY = y + size + 4.0F;
               Fonts.getSize(15, Fonts.Type.INTER_BOLD).drawString(matrix, distanceText, textX, textY, this.distanceColor.getColor());
            }

            matrix.pop();
         }
      }
   }

   private float getRotationToTarget() {
      float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
      double playerX = MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX());
      double playerZ = MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ());
      double x = this.targetPos.getX() - playerX;
      double z = this.targetPos.getZ() - playerZ;
      return (float)(-(Math.atan2(x, z) * (180.0 / Math.PI)));
   }

   private float getInterpolatedYaw() {
      float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
      return MathHelper.lerp(tickDelta, mc.player.prevYaw, mc.player.getYaw());
   }

   private float getDistanceToTarget() {
      float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
      double playerX = MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX());
      double playerZ = MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ());
      double dx = this.targetPos.getX() - playerX;
      double dz = this.targetPos.getZ() - playerZ;
      return (float)Math.sqrt(dx * dx + dz * dz);
   }

   public Identifier getArrowTexture() {
      String var1 = this.arrowType.getSelected();

      return switch (var1) {
         case "Fill" -> Identifier.of("textures/arrows/arrow_fill.png");
         case "Outline" -> Identifier.of("textures/arrows/arrow_outline.png");
         default -> null;
      };
   }

   public Animation getRadiusAnim() {
      return this.radiusAnim;
   }

   public BlockPos getTargetPos() {
      return this.targetPos;
   }

   public boolean isIgnoreY() {
      return this.ignoreY;
   }

   public SelectSetting getAnchorType() {
      return this.anchorType;
   }

   public SelectSetting getArrowType() {
      return this.arrowType;
   }

   public ValueSetting getArrowSize() {
      return this.arrowSize;
   }

   public ValueSetting getRadiusSetting() {
      return this.radiusSetting;
   }

   public ValueSetting getMarkerSize() {
      return this.markerSize;
   }

   public BooleanSetting getShowDistance() {
      return this.showDistance;
   }

   public SelectSetting getDistancePosition() {
      return this.distancePosition;
   }

   public SelectSetting getDistanceTextPosition() {
      return this.distanceTextPosition;
   }

   public ColorSetting getDistanceColor() {
      return this.distanceColor;
   }

   public BooleanSetting getShowBeam() {
      return this.showBeam;
   }

   public BooleanSetting getAutoDelete() {
      return this.autoDelete;
   }

   public ValueSetting getAutoDeleteDistance() {
      return this.autoDeleteDistance;
   }
}
