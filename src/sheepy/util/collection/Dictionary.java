package sheepy.util.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Supplier;

/**
 * A HashMap that automatically create values.
 * The value constructor is supplied during creation.
 *
 * Use subclass Directory.List and Directory.Map for common usages.
 *
 * @param <K> Key type of map.
 * @param <V> Value type of map.
 */
public class Dictionary< K, V > extends HashMap< K, V > {

   private final Supplier<V> builder;

   public Dictionary ( Supplier<V> builder ) {
      this.builder = builder;
   }

   @Override public V get( Object key ) {
      K k = (K) key;
      if ( ! containsKey( k ) )
         put( k, builder.get() );
      return super.get( key );
   }

   public static class List < K, V > extends Dictionary< K, java.util.List< V > > {
      public List () {
         super( ArrayList::new );
      }
   }

   public static class Set < K, V > extends Dictionary< K, java.util.Set< V > > {
      public Set () {
         super( HashSet::new );
      }
   }

   public static class Map < K1, K2, V > extends Dictionary< K1, java.util.Map< K2, V > > {
      public Map () {
         super( HashMap::new );
      }
   }
}
