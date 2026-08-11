package padej.soup.api.feature.draggable;

import java.util.ArrayList;
import java.util.List;
import padej.soup.implement.features.draggables.Armor;
import padej.soup.implement.features.draggables.BossBars;
import padej.soup.implement.features.draggables.Consumable;
import padej.soup.implement.features.draggables.CoolDowns;
import padej.soup.implement.features.draggables.HotBar;
import padej.soup.implement.features.draggables.HotKeys;
import padej.soup.implement.features.draggables.Inventory;
import padej.soup.implement.features.draggables.ModulesList;
import padej.soup.implement.features.draggables.Notifications;
import padej.soup.implement.features.draggables.PlayerInfo;
import padej.soup.implement.features.draggables.Potions;
import padej.soup.implement.features.draggables.ScoreBoard;
import padej.soup.implement.features.draggables.TargetHud;
import padej.soup.implement.features.draggables.Watermark;

public class DraggableRepository {
   private final List<AbstractDraggable> draggable = new ArrayList<>();
   private static DraggableRepository instance;

   public static DraggableRepository getInstance() {
      return instance;
   }

   public static void setInstance(DraggableRepository repo) {
      instance = repo;
   }

   public void setup() {
      this.register(
         new ModulesList(),
         new TargetHud(),
         new Potions(),
         new HotKeys(),
         new BossBars(),
         new Armor(),
         new Watermark(),
         new Inventory(),
         new Consumable(),
         new CoolDowns(),
         new Notifications(),
         new ScoreBoard(),
         new HotBar(),
         new PlayerInfo()
      );
   }

   public void register(AbstractDraggable... module) {
      this.draggable.addAll(List.of(module));
   }

   public List<AbstractDraggable> draggable() {
      return this.draggable;
   }

   public <T extends AbstractDraggable> T get(String name) {
      for (AbstractDraggable module : this.draggable) {
         if (module.getName().equalsIgnoreCase(name)) {
            return (T)module;
         }
      }

      return null;
   }

   public <T extends AbstractDraggable> T get(Class<T> clazz) {
      for (AbstractDraggable module : this.draggable) {
         if (clazz.isAssignableFrom(module.getClass())) {
            return clazz.cast(module);
         }
      }

      return null;
   }
}
