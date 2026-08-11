package padej.soup.base.util.particle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import padej.soup.base.util.spatial.BlockCollisionGrid;

public class ParticleUpdateExecutor {
   private static final int THREAD_COUNT = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
   private static final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT, r -> {
      Thread thread = new Thread(r);
      thread.setName("ParticleUpdate-" + thread.getId());
      thread.setDaemon(true);
      return thread;
   });

   public static <T extends AbstractParticle> List<T> updateParticles(List<T> particles, long currentTime, Predicate<T> isVisible) {
      return updateParticles(particles, currentTime, isVisible, null);
   }

   public static <T extends AbstractParticle> List<T> updateParticles(
      List<T> particles, long currentTime, Predicate<T> isVisible, BlockCollisionGrid collisionGrid
   ) {
      if (particles.isEmpty()) {
         return Collections.emptyList();
      } else if (particles.size() < 50) {
         List<T> toRemove = new ArrayList<>();

         for (T particle : particles) {
            if (particle.update(currentTime, isVisible.test(particle), collisionGrid)) {
               toRemove.add(particle);
            }
         }

         return toRemove;
      } else {
         int chunkSize = Math.max(1, particles.size() / THREAD_COUNT);
         List<Future<List<T>>> futures = new ArrayList<>();

         for (int i = 0; i < particles.size(); i += chunkSize) {
            int start = i;
            int end = Math.min(i + chunkSize, particles.size());
            futures.add(executor.submit(() -> {
               List<T> toRemovex = new ArrayList<>();

               for (int j = start; j < end; j++) {
                  T particlex = particles.get(j);
                  if (particlex.update(currentTime, isVisible.test(particlex), collisionGrid)) {
                     toRemovex.add(particlex);
                  }
               }

               return toRemovex;
            }));
         }

         List<T> toRemove = new ArrayList<>();

         for (Future<List<T>> future : futures) {
            try {
               toRemove.addAll(future.get());
            } catch (ExecutionException | InterruptedException var11) {
               var11.printStackTrace();
            }
         }

         return toRemove;
      }
   }

   public static <T extends AbstractParticle> void updateParticlesInPlace(List<T> particles, long currentTime, Predicate<T> isVisible) {
      updateParticlesInPlace(particles, currentTime, isVisible, null, null);
   }

   public static <T extends AbstractParticle> void updateParticlesInPlace(
      List<T> particles, long currentTime, Predicate<T> isVisible, BlockCollisionGrid collisionGrid, Consumer<T> onRemove
   ) {
      if (!particles.isEmpty()) {
         int write = 0;
         int size = particles.size();

         for (int read = 0; read < size; read++) {
            T particle = (T)particles.get(read);
            boolean shouldRemove = particle.update(currentTime, isVisible.test(particle), collisionGrid);
            if (shouldRemove) {
               if (onRemove != null) {
                  onRemove.accept(particle);
               }
            } else {
               if (write != read) {
                  particles.set(write, particle);
               }

               write++;
            }
         }

         for (int i = particles.size() - 1; i >= write; i--) {
            particles.remove(i);
         }
      }
   }

   public static void shutdown() {
      executor.shutdown();

      try {
         if (!executor.awaitTermination(1L, TimeUnit.SECONDS)) {
            executor.shutdownNow();
         }
      } catch (InterruptedException var1) {
         executor.shutdownNow();
      }
   }
}
