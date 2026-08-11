package padej.soup.base.util.other;

import java.util.ArrayDeque;
import java.util.Queue;
import padej.soup.base.trait.Producer;

public class Pool<T> {
   private final Queue<T> items = new ArrayDeque<>();
   private final Producer<T> producer;

   public Pool(Producer<T> producer) {
      this.producer = producer;
   }

   public synchronized T get() {
      return !this.items.isEmpty() ? this.items.poll() : this.producer.create();
   }

   public synchronized void free(T obj) {
      this.items.offer(obj);
   }
}
