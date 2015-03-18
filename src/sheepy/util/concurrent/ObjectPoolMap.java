package sheepy.util.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A mapped object pool.
 * Acquired item must be returned before it can be reused.
 *
 * To use, subclass and implement build method.
 *
 * @param <K> Map key
 * @param <V> Map value
 */
public abstract class ObjectPoolMap<K,V> {
   private final Map<K, List<V>> cache = new HashMap<>();

   public V get ( K key ) {
      V result = null;
      List<V> pool;
      // Try to read from cache

      synchronized ( cache ) {
         pool = cache.get( key );
      }
      if ( pool != null ) synchronized ( pool ) {
         if ( ! pool.isEmpty() )
            result = pool.remove( 0 );
      }

      // If necessary, build result and put into cache
      if ( result == null ) return build( key );
      return result;
   }

   /**
    * Release given object back to the pool.
    */
   public void recycle( K key, V object ) {
      List<V> pool;
      object = refresh( key, object );
      if ( object == null ) return;

      synchronized ( cache ) {
         pool = cache.get( key );
         if ( pool == null ) {
            pool = new ArrayList<>(8);
            cache.put( key, pool );
         }
      }
      synchronized( pool ) {
         pool.add( object );
      }
   }

   /**
    * Build value for given key to be pooled.
    */
   protected abstract V build( K key );

   /**
    * Refresh an object before it is released back to the pool.
    * Return null to not reuse the object.
    */
   protected V refresh( K key, V object ) { return object; }

   public static <K,V> ObjectPoolMap<K,V> create ( Function<K,V> builder ) {
      return new ObjectPoolMap<K,V> () {
         @Override protected V build ( K key ) { return builder.apply( key ); }
      };
   }

   public static <K,V> ObjectPoolMap<K,V> create ( Function<K,V> builder, UnaryOperator<V> recycler ) {
      return new ObjectPoolMap<K,V> () {
         @Override protected V build ( K key ) { return builder.apply( key ); }
         @Override protected V refresh( K key, V object ) { return recycler.apply( object ); }
      };
   }
}