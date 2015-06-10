package sheepy.util.concurrent;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A mapped object pool.
 * Acquired item must be returned before it can be reused.
 * Pooled objects are stored in SoftReference, and may be released at any time.
 *
 * To use, subclass and implement build method, or use one of the create methods.
 *
 * @param <K> Map key
 * @param <V> Map value
 */
public abstract class ObjectPoolMap<K,V> {
   private final Map<K, List<Reference<V>>> cache = new HashMap<>();

   /**
    * Get an object from pool.  Will create a new object if pool is empty.
    *
    * @param key Key to get
    * @return Pooled object
    */
   public V get ( K key ) {
      V result = null;
      List<Reference<V>> pool;
      // Try to read from cache

      synchronized ( cache ) {
         pool = cache.get( key );
      }
      if ( pool != null ) synchronized ( pool ) {
         while ( result == null && ! pool.isEmpty() ) {
            result = pool.remove( 0 ).get();
         }
      }

      // If necessary, build result and put into cache
      if ( result == null ) return build( key );
      return result;
   }

   /**
    * Release given object back to the pool.
    */
   public void recycle( K key, V object ) {
      List<Reference<V>> pool;
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
         pool.add( new SoftReference<>( object ) );
      }
   }

   /**
    * Build value for given key to be pooled.
    */
   protected abstract V build( K key );

   /**
    * Refresh / reset / cleanup an object before it is released back to the pool.
    * Return null to not reuse the object.
    */
   protected V refresh( K key, V object ) { return object; }

   /**
    * Create a new object pool that builds object on demand.
    *
    * @param <K> Key of pool
    * @param <V> Value of pool
    * @param builder Object builder.
    * @return Object pool
    * @throws NullPointerException if builder is null
    */
   public static <K,V> ObjectPoolMap<K,V> create ( Function<K,V> builder ) {
      return new ObjectPoolMap<K,V> () {
         @Override protected V build ( K key ) { return builder.apply( key ); }
      };
   }

   /**
    * Create a new object pool that builds object on demand.
    *
    * @param <K> Key of pool
    * @param <V> Value of pool
    * @param builder Object builder
    * @param recycler Object recycler, see {@link #refresh(java.lang.Object, java.lang.Object)}.
    * @return Object pool
    * @throws NullPointerException if builder is null
    */
   public static <K,V> ObjectPoolMap<K,V> create ( Function<K,V> builder, UnaryOperator<V> recycler ) {
      return new ObjectPoolMap<K,V> () {
         @Override protected V build ( K key ) { return builder.apply( key ); }
         @Override protected V refresh( K key, V object ) { return recycler.apply( object ); }
      };
   }
}