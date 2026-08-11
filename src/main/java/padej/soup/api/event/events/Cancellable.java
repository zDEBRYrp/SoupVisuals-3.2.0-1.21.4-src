package padej.soup.api.event.events;

public interface Cancellable {
   boolean isCancelled();

   void cancel();
}
