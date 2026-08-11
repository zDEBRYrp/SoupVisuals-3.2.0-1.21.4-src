package padej.soup.api.system.logger.implement;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import padej.soup.api.system.logger.Logger;

public class ConsoleLogger implements Logger {
   private final org.apache.logging.log4j.Logger logger = LogManager.getLogger("SoupAPI");

   @Override
   public void log(Object message) {
      this.logger.info("{}[SoupAPI]:{} {}", Formatting.GRAY, Formatting.RESET, message);
   }

   @Override
   public void minecraftLog(Text... components) {
   }
}
