package padej.soup.base;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.Window;
import padej.soup.api.system.draw.DrawEngine;
import padej.soup.api.system.draw.DrawEngineImpl;
import padej.soup.api.system.shape.implement.Arc;
import padej.soup.api.system.shape.implement.Blur;
import padej.soup.api.system.shape.implement.Image;
import padej.soup.api.system.shape.implement.ModulesOutline;
import padej.soup.api.system.shape.implement.Rectangle;
import padej.soup.core.Main;
import padej.soup.implement.menu.components.implement.window.WindowManager;

public interface QuickImports extends QuickLogger {
   MinecraftClient mc = Main.mc;
   RenderTickCounter tickCounter = mc.getRenderTickCounter();
   Window window = mc.getWindow();
   Tessellator tessellator = Tessellator.getInstance();
   DrawEngine drawEngine = new DrawEngineImpl();
   Rectangle rectangle = new Rectangle();
   Blur blur = new Blur();
   Arc arc = new Arc();
   Image image = new Image();
   ModulesOutline modulesOutline = new ModulesOutline();
   WindowManager windowManager = WindowManager.INSTANCE;

   static ThreadLocalRandom random() {
      return ThreadLocalRandom.current();
   }
}
