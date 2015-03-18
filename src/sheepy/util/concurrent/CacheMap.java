package sheepy.util.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A mapped cache that creates shared item on demand.
 *
 * To use, subclass and implement build method.
 *
 * @param <K> Map key
 * @param <V> Map value
 */
public abstract class CacheMap<K,V> {
   private final Map<K, V> cache = new HashMap<>();

   public V get ( K key ) {
      V result;
      // Try to read from cache

      synchronized ( cache ) {
         result = cache.get( key );
      }
      // If necessary, build result and put into cache
      if ( result == null ) {
         result = build( key );
         synchronized ( cache ) {
            if ( cache.containsKey( key ) )
               result = cache.get( key );
            else
               cache.put( key, result );
         }
      }
      return result;
   }

   /**
    * Build value for given key to be cached.
    *
    * May be called multiple times for same key, but all {@link #get(java.lang.Object)}
    * will only return the first that returns.
    *
    * @param key
    * @return
    */
   protected abstract V build( K key );

   public static <K,V> CacheMap<K,V> create ( final Function<K,V> builder ) {
      return new CacheMap<K,V> () {
         @Override protected V build ( K key ) { return builder.apply( key ); }
      };
   }
}