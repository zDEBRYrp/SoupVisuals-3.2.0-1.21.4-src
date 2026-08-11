package padej.soup.api.accessor;

import java.util.List;
import padej.soup.implement.features.modules.visuals.Trails;

@FunctionalInterface
public interface IEntity {
   List<Trails.Trail> getTrails();
}
