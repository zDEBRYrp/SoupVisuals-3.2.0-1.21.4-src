package padej.soup.base.util.spatial;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SpatialGrid3D<T> {
   private final float cellSize;
   private final Map<SpatialGrid3D.GridKey, List<SpatialGrid3D.GridEntry<T>>> grid;

   public SpatialGrid3D(float cellSize) {
      this.cellSize = cellSize;
      this.grid = new ConcurrentHashMap<>();
   }

   public void insert(T object, float x, float y, float z) {
      SpatialGrid3D.GridKey key = this.getGridKey(x, y, z);
      this.grid.computeIfAbsent(key, k -> new ArrayList<>()).add(new SpatialGrid3D.GridEntry<>(object, x, y, z));
   }

   public void update(T object, float oldX, float oldY, float oldZ, float newX, float newY, float newZ) {
      SpatialGrid3D.GridKey oldKey = this.getGridKey(oldX, oldY, oldZ);
      SpatialGrid3D.GridKey newKey = this.getGridKey(newX, newY, newZ);
      if (oldKey.equals(newKey)) {
         List<SpatialGrid3D.GridEntry<T>> entries = this.grid.get(oldKey);
         if (entries != null) {
            for (SpatialGrid3D.GridEntry<T> entry : entries) {
               if (entry.object.equals(object)) {
                  entry.x = newX;
                  entry.y = newY;
                  entry.z = newZ;
                  return;
               }
            }
         }
      } else {
         this.remove(object, oldX, oldY, oldZ);
         this.insert(object, newX, newY, newZ);
      }
   }

   public void remove(T object, float x, float y, float z) {
      SpatialGrid3D.GridKey key = this.getGridKey(x, y, z);
      List<SpatialGrid3D.GridEntry<T>> entries = this.grid.get(key);
      if (entries != null) {
         entries.removeIf(entry -> entry.object.equals(object));
         if (entries.isEmpty()) {
            this.grid.remove(key);
         }
      }
   }

   public List<T> queryRadius(float x, float y, float z, float radius) {
      List<T> results = new ArrayList<>();
      float radiusSquared = radius * radius;
      int minCellX = (int)Math.floor((x - radius) / this.cellSize);
      int maxCellX = (int)Math.floor((x + radius) / this.cellSize);
      int minCellY = (int)Math.floor((y - radius) / this.cellSize);
      int maxCellY = (int)Math.floor((y + radius) / this.cellSize);
      int minCellZ = (int)Math.floor((z - radius) / this.cellSize);
      int maxCellZ = (int)Math.floor((z + radius) / this.cellSize);

      for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
         for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
               SpatialGrid3D.GridKey key = new SpatialGrid3D.GridKey(cellX, cellY, cellZ);
               List<SpatialGrid3D.GridEntry<T>> entries = this.grid.get(key);
               if (entries != null) {
                  for (SpatialGrid3D.GridEntry<T> entry : entries) {
                     float dx = entry.x - x;
                     float dy = entry.y - y;
                     float dz = entry.z - z;
                     float distSquared = dx * dx + dy * dy + dz * dz;
                     if (distSquared <= radiusSquared) {
                        results.add(entry.object);
                     }
                  }
               }
            }
         }
      }

      return results;
   }

   public List<SpatialGrid3D.GridEntry<T>> queryRadiusWithPositions(float x, float y, float z, float radius) {
      List<SpatialGrid3D.GridEntry<T>> results = new ArrayList<>();
      float radiusSquared = radius * radius;
      int minCellX = (int)Math.floor((x - radius) / this.cellSize);
      int maxCellX = (int)Math.floor((x + radius) / this.cellSize);
      int minCellY = (int)Math.floor((y - radius) / this.cellSize);
      int maxCellY = (int)Math.floor((y + radius) / this.cellSize);
      int minCellZ = (int)Math.floor((z - radius) / this.cellSize);
      int maxCellZ = (int)Math.floor((z + radius) / this.cellSize);

      for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
         for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
               SpatialGrid3D.GridKey key = new SpatialGrid3D.GridKey(cellX, cellY, cellZ);
               List<SpatialGrid3D.GridEntry<T>> entries = this.grid.get(key);
               if (entries != null) {
                  for (SpatialGrid3D.GridEntry<T> entry : entries) {
                     float dx = entry.x - x;
                     float dy = entry.y - y;
                     float dz = entry.z - z;
                     float distSquared = dx * dx + dy * dy + dz * dz;
                     if (distSquared <= radiusSquared) {
                        results.add(entry);
                     }
                  }
               }
            }
         }
      }

      return results;
   }

   public void forEachInRadiusWithPositions(float x, float y, float z, float radius, Consumer<SpatialGrid3D.GridEntry<T>> consumer) {
      float radiusSquared = radius * radius;
      int minCellX = (int)Math.floor((x - radius) / this.cellSize);
      int maxCellX = (int)Math.floor((x + radius) / this.cellSize);
      int minCellY = (int)Math.floor((y - radius) / this.cellSize);
      int maxCellY = (int)Math.floor((y + radius) / this.cellSize);
      int minCellZ = (int)Math.floor((z - radius) / this.cellSize);
      int maxCellZ = (int)Math.floor((z + radius) / this.cellSize);

      for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
         for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
               SpatialGrid3D.GridKey key = new SpatialGrid3D.GridKey(cellX, cellY, cellZ);
               List<SpatialGrid3D.GridEntry<T>> entries = this.grid.get(key);
               if (entries != null) {
                  for (SpatialGrid3D.GridEntry<T> entry : entries) {
                     float dx = entry.x - x;
                     float dy = entry.y - y;
                     float dz = entry.z - z;
                     float distSquared = dx * dx + dy * dy + dz * dz;
                     if (distSquared <= radiusSquared) {
                        consumer.accept(entry);
                     }
                  }
               }
            }
         }
      }
   }

   public void clear() {
      this.grid.clear();
   }

   public int size() {
      return this.grid.values().stream().mapToInt(List::size).sum();
   }

   private SpatialGrid3D.GridKey getGridKey(float x, float y, float z) {
      return new SpatialGrid3D.GridKey((int)Math.floor(x / this.cellSize), (int)Math.floor(y / this.cellSize), (int)Math.floor(z / this.cellSize));
   }

   public static class GridEntry<T> {
      public final T object;
      public float x;
      public float y;
      public float z;

      public GridEntry(T object, float x, float y, float z) {
         this.object = object;
         this.x = x;
         this.y = y;
         this.z = z;
      }

      public float getX() {
         return this.x;
      }

      public float getY() {
         return this.y;
      }

      public float getZ() {
         return this.z;
      }

      public T getObject() {
         return this.object;
      }
   }

   private record GridKey(int x, int y, int z) {
   }
}
