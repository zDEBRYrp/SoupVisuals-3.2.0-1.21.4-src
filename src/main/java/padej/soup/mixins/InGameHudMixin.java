package padej.soup.mixins;

import java.util.ConcurrentModificationException;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.render.DrawEvent;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.pipeline.HudRenderPipeline;
import padej.soup.base.QuickImports;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.render.HotBarStatusRenderer;
import padej.soup.base.util.render.Render2DUtil;
import padej.soup.core.Main;
import padej.soup.core.perftest.HudProfiler;
import padej.soup.implement.features.draggables.HotBar;
import padej.soup.implement.features.draggables.TargetHud;
import padej.soup.implement.features.modules.hud.CrossHair;
import padej.soup.implement.features.modules.hud.Potions;
import padej.soup.implement.features.modules.hud.ScoreBoard;

@ProtIgnore
@Mixin({InGameHud.class})
public abstract class InGameHudMixin implements QuickImports {
   @Final
   @Shadow
   private MinecraftClient field_2035;
   @Unique
   private float displayedHealth = 20.0F;
   @Unique
   private float displayedArmor = 0.0F;
   @Unique
   private float displayedFood = 20.0F;
   @Unique
   private float displayedAir = 300.0F;
   @Unique
   private float displayedAbsorption = 0.0F;
   @Unique
   private TargetHud cachedTargetHudDraggable;
   @Unique
   private HotBar cachedHotBarDraggable;

   @Shadow
   protected abstract void method_1760(DrawContext var1);

   @Shadow
   protected abstract void method_1741(DrawContext var1);

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   public void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      EventManager.callEvent(DrawEvent.INSTANCE.set(context, drawEngine, tickCounter.getTickDelta(false)));
      Render2DUtil.onRender(context);
      if (this.field_2035.options.hudHidden) {
         blur.resetRegion();
         blur.setup();
      } else {
         HudProfiler.frameStart();
         HudProfiler.begin(HudProfiler.Section.FRAME_TOTAL);
         List<AbstractDraggable> draggables = Main.getInstance().getDraggableRepository().draggable();
         HudProfiler.begin(HudProfiler.Section.PHASE1_COLLECT_REGIONS);
         blur.resetRegion();

         for (AbstractDraggable draggable : draggables) {
            if (draggable.canDraw(draggable)) {
               draggable.startAnimation();
            } else {
               draggable.stopAnimation();
            }

            float scale = draggable.getScaleAnimation().getOutput().floatValue();
            if (!(scale <= 0.01F)) {
               draggable.validPosition();
               blur.expandRegion(draggable.getX(), draggable.getY(), draggable.getWidth(), draggable.getHeight());
            }
         }

         HudProfiler.end(HudProfiler.Section.PHASE1_COLLECT_REGIONS);
         HudProfiler.begin(HudProfiler.Section.PHASE2_BLUR_SETUP);
         blur.setup();
         HudProfiler.end(HudProfiler.Section.PHASE2_BLUR_SETUP);
         HudProfiler.begin(HudProfiler.Section.PHASE3B_DRAGGABLES);
         HudRenderPipeline pipeline = HudRenderPipeline.getInstance();
         pipeline.beginFrame(Fonts.getSize(16));

         for (AbstractDraggable draggable : draggables) {
            float scale = draggable.getScaleAnimation().getOutput().floatValue();
            if (!(scale <= 0.01F)) {
               long perDraggableStart = HudProfiler.nano();
               MathUtil.setAlpha(scale, () -> {
                  try {
                     float draggableScale = draggable.getDraggableScale();
                     if (Math.abs(draggableScale - 1.0F) <= 0.001F) {
                        draggable.drawDraggable(context);
                        return;
                     }

                     context.getMatrices().push();

                     try {
                        float centerX = draggable.getX() + draggable.getWidth() / 2.0F;
                        float centerY = draggable.getY() + draggable.getHeight() / 2.0F;
                        context.getMatrices().translate(centerX, centerY, 0.0F);
                        context.getMatrices().scale(draggableScale, draggableScale, 1.0F);
                        context.getMatrices().translate(-centerX, -centerY, 0.0F);
                        draggable.drawDraggable(context);
                     } finally {
                        context.getMatrices().pop();
                     }
                  } catch (ConcurrentModificationException var9x) {
                     LoggerUtil.error("Failed to render draggable: " + var9x.getMessage());
                  }
               });
               HudProfiler.recordDraggable(draggable.getName(), perDraggableStart);
            }
         }

         pipeline.endFrame();
         HudProfiler.end(HudProfiler.Section.PHASE3B_DRAGGABLES);
         HudProfiler.end(HudProfiler.Section.FRAME_TOTAL);
         HudProfiler.frameEnd();

         try {
            if (!padej.soup.implement.features.modules.hud.TargetHud.getInstance().isEnabled()) {
               return;
            }

            TargetHud targetHud = this.getTargetHudDraggable(draggables);
            if (targetHud != null) {
               targetHud.renderParticlesAlways(context);
            }
         } catch (Exception var11) {
            LoggerUtil.error("Failed onRender: " + var11.getMessage());
         }
      }
   }

   @Unique
   private float getHotBarDraggableScale() {
      if (this.cachedHotBarDraggable != null) {
         return this.cachedHotBarDraggable.getDraggableScale();
      } else {
         for (AbstractDraggable draggable : Main.getInstance().getDraggableRepository().draggable()) {
            if (draggable instanceof HotBar hotBar) {
               this.cachedHotBarDraggable = hotBar;
               return hotBar.getDraggableScale();
            }
         }

         return 1.0F;
      }
   }

   @Unique
   private TargetHud getTargetHudDraggable(List<AbstractDraggable> draggables) {
      if (this.cachedTargetHudDraggable != null) {
         return this.cachedTargetHudDraggable;
      } else {
         for (AbstractDraggable draggable : draggables) {
            if (draggable instanceof TargetHud targetHud) {
               this.cachedTargetHudDraggable = targetHud;
               return targetHud;
            }
         }

         return null;
      }
   }

   @Inject(
      method = {"renderCrosshair"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/gui/hud/InGameHud;CROSSHAIR_TEXTURE:Lnet/minecraft/util/Identifier;"
      )},
      cancellable = true
   )
   public void renderCrosshairHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CrossHair crossHair = CrossHair.getInstance();
      if (crossHair.isState()) {
         crossHair.onRenderCrossHair();
         ci.cancel();
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"renderStatusEffectOverlay"},
      cancellable = true
   )
   public void renderStatusEffectOverlayHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (Potions.getInstance().isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderScoreboardSidebarHook(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
      if (ScoreBoard.getInstance().isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderOverlayMessage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (padej.soup.implement.features.modules.hud.HotBar.getInstance().isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderExperienceLevel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (padej.soup.implement.features.modules.hud.HotBar.getInstance().isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderMainHud"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (padej.soup.implement.features.modules.hud.HotBar.getInstance().isEnabled()) {
         context.drawGuiTexture(RenderLayer::getGuiTextured, InGameHud.HOTBAR_ATTACK_INDICATOR_BACKGROUND_TEXTURE, 0, 0, 1, 1);
         String statusBarsMode = padej.soup.implement.features.modules.hud.HotBar.getInstance().getStatusBarsMode().getSelected();
         if (this.field_2035.interactionManager.hasStatusBars()) {
            if (statusBarsMode.equals("Bars")) {
               this.renderCustomStatusBars(context);
            } else {
               this.method_1760(context);
            }
         }

         this.method_1741(context);
         ci.cancel();
      }
   }

   @Unique
   private void renderCustomStatusBars(DrawContext context) {
      if (this.field_2035.player != null) {
         int screenWidth = context.getScaledWindowWidth();
         int screenHeight = context.getScaledWindowHeight();
         float scale = this.getHotBarDraggableScale();
         float pivotX = screenWidth / 2.0F;
         float pivotY = screenHeight - 16.0F;
         context.getMatrices().push();
         context.getMatrices().translate(pivotX, pivotY, 0.0F);
         context.getMatrices().scale(scale, scale, 1.0F);
         context.getMatrices().translate(-pivotX, -pivotY, 0.0F);
         HotBarStatusRenderer.setScale(scale);
         int offsetT = 12;
         float maxHealth = this.field_2035.player.getMaxHealth();
         float health = this.field_2035.player.getHealth();
         float absorption = this.field_2035.player.getAbsorptionAmount();
         float armor = this.field_2035.player.getArmor();
         float food = this.field_2035.player.getHungerManager().getFoodLevel();
         float saturation = this.field_2035.player.getHungerManager().getSaturationLevel();
         int maxAir = this.field_2035.player.getMaxAir();
         int air = this.field_2035.player.getAir();
         this.displayedHealth = this.displayedHealth + (health - this.displayedHealth) * 0.2F;
         this.displayedAbsorption = this.displayedAbsorption + (absorption - this.displayedAbsorption) * 0.2F;
         this.displayedArmor = this.displayedArmor + (armor - this.displayedArmor) * 0.2F;
         this.displayedFood = this.displayedFood + (food - this.displayedFood) * 0.2F;
         this.displayedAir = this.displayedAir + (air - this.displayedAir) * 0.2F;
         int barWidth = 81;
         int barHeight = 9;
         int cornerRadius = 2;
         int centerX = screenWidth / 2;
         int offset = 91;
         int healthX = centerX - offset;
         int healthY = screenHeight - 55;
         HotBarStatusRenderer.renderHealthBar(
            context, healthX, healthY + offsetT, barWidth, barHeight, cornerRadius, this.displayedHealth, maxHealth, this.displayedAbsorption
         );
         if (armor > 0.0F) {
            int armorY = healthY - 15;
            HotBarStatusRenderer.renderArmorBar(context, healthX, armorY + offsetT, barWidth, barHeight, cornerRadius, this.displayedArmor);
         }

         int hungerX = centerX + offset - barWidth;
         int hungerY = screenHeight - 55;
         HotBarStatusRenderer.renderHungerBar(context, hungerX, hungerY + offsetT, barWidth, barHeight, cornerRadius, this.displayedFood, saturation);
         if (air < maxAir) {
            int airY = hungerY - 15;
            HotBarStatusRenderer.renderAirBar(context, hungerX, airY + offsetT, barWidth, barHeight, cornerRadius, this.displayedAir, maxAir);
         }

         if (padej.soup.implement.features.modules.hud.HotBar.getInstance().getExperienceBar().isValue()) {
            float experienceProgress = this.field_2035.player.experienceProgress;
            int experienceLevel = this.field_2035.player.experienceLevel;
            int expBarWidth = 186;
            int expBarHeight = 4;
            int expBarX = centerX - expBarWidth / 2 + 2;
            int expBarY = screenHeight - 33;
            HotBarStatusRenderer.renderExperienceBar(context, expBarX, expBarY, expBarWidth, expBarHeight, cornerRadius, experienceProgress, experienceLevel);
         }

         HotBarStatusRenderer.setScale(1.0F);
         context.getMatrices().pop();
      }
   }
}
