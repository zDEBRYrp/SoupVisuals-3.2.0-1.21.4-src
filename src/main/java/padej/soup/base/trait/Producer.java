package padej.soup.base.trait;

@FunctionalInterface
public interface Producer<T> {
   T create();
}
