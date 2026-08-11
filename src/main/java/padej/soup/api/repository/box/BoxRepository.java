package padej.soup.api.repository.box;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.EntityType;
import padej.soup.api.event.EventManager;
import padej.soup.base.QuickImports;
import padej.soup.base.QuickLogger;

public class BoxRepository implements QuickImports, QuickLogger {
   private final Map<EntityType<?>, Boolean> entities = new HashMap<>();

   public BoxRepository(EventManager eventManager) {
      eventManager.register(this);
   }

   public Map<EntityType<?>, Boolean> getEntities() {
      return this.entities;
   }
}
