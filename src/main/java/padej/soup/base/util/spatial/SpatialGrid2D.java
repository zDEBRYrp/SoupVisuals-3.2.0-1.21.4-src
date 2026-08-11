package padej.soup.base.util.spatial;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpatialGrid2D<T> {
   private final float cellSize;
   private final Map<SpatialGrid2D.GridKey2D, List<SpatialGrid2D.GridEntry2D<T>>> grid;

   public SpatialGrid2D(float cellSize) {
      this.cellSize = cellSize;
      this.grid = new ConcurrentHashMap<>();
   }

   public void insert(T object, float x, float y) {
      SpatialGrid2D.GridKey2D key = this.getGridKey(x, y);
      this.grid.computeIfAbsent(key, k -> new ArrayList<>()).add(new SpatialGrid2D.GridEntry2D<>(object, x, y));
   }

   public void update(T object, float oldX, float oldY, float newX, float newY) {
      SpatialGrid2D.GridKey2D oldKey = this.getGridKey(oldX, oldY);
      SpatialGrid2D.GridKey2D newKey = this.getGridKey(newX, newY);
      if (oldKey.equals(newKey)) {
         List<SpatialGrid2D.GridEntry2D<T>> entries = this.grid.get(oldKey);
         if (entries != null) {
            for (SpatialGrid2D.GridEntry2D<T> entry : entries) {
               if (entry.object.equals(object)) {
                  entry.x = newX;
                  entry.y = newY;
                  return;
               }
            }
         }
      } else {
         this.remove(object, oldX, oldY);
         this.insert(object, newX, newY);
      }
   }

   public void remove(T object, float x, float y) {
      SpatialGrid2D.GridKey2D key = this.getGridKey(x, y);
      List<SpatialGrid2D.GridEntry2D<T>> entries = this.grid.get(key);
      if (entries != null) {
         entries.removeIf(entry -> entry.object.equals(object));
         if (entries.isEmpty()) {
            this.grid.remove(key);
         }
      }
   }

   public List<SpatialGrid2D.GridEntry2D<T>> queryRadiusWithPositions(float x, float y, float radius) {
      List<SpatialGrid2D.GridEntry2D<T>> results = new ArrayList<>();
      float radiusSquared = radius * radius;
      int minCellX = (int)Math.floor((x - radius) / this.cellSize);
      int maxCellX = (int)Math.floor((x + radius) / this.cellSize);
      int minCellY = (int)Math.floor((y - radius) / this.cellSize);
      int maxCellY = (int)Math.floor((y + radius) / this.cellSize);

      for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
         for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
            SpatialGrid2D.GridKey2D key = new SpatialGrid2D.GridKey2D(cellX, cellY);
            List<SpatialGrid2D.GridEntry2D<T>> entries = this.grid.get(key);
            if (entries != null) {
               for (SpatialGrid2D.GridEntry2D<T> entry : entries) {
                  float dx = entry.x - x;
                  float dy = entry.y - y;
                  float distSquared = dx * dx + dy * dy;
                  if (distSquared <= radiusSquared) {
                     results.add(entry);
                  }
               }
            }
         }
      }

      return results;
   }

   public void clear() {
      this.grid.clear();
   }

   private SpatialGrid2D.GridKey2D getGridKey(float x, float y) {
      return new SpatialGrid2D.GridKey2D((int)Math.floor(x / this.cellSize), (int)Math.floor(y / this.cellSize));
   }

   public static class GridEntry2D<T> {
      private final T object;
      private float x;
      private float y;

      GridEntry2D(T object, float x, float y) {
         this.object = object;
         this.x = x;
         this.y = y;
      }

      public T getObject() {
         return this.object;
      }

      public float getX() {
         return this.x;
      }

      public float getY() {
         return this.y;
      }
   }

   private static class GridKey2D {
      final int x;
      final int y;

      GridKey2D(int x, int y) {
         this.x = x;
         this.y = y;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            SpatialGrid2D.GridKey2D that = (SpatialGrid2D.GridKey2D)o;
            return this.x == that.x && this.y == that.y;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return 31 * this.x + this.y;
      }
   }
}
