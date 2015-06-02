package sheepy.util.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * Utilities for handling nullable collection variables.
 */
public class NullData {

   public static <T> List<T> copy ( List<T> subject ) {
      return subject == null ? Collections.emptyList() : Collections.unmodifiableList(subject);
   }

   public static <T> T get ( List<T> subject, int index ) {
      return subject == null || subject.size() <= index ? null : subject.get( index );
   }
   public static <T> T first ( List<T> subject ) { return get( subject, 0 ); }
   public static <T> T last  ( List<T> subject ) {
      return subject == null || subject.isEmpty() ? null : subject.get( subject.size()-1 );
   }

   public static <T> Set<T> nonNull ( Set<T> subject ) {
      return subject == null ? Collections.emptySet() : subject;
   }
   public static <T> List<T> nonNull ( List<T> subject ) {
      return subject == null ? Collections.emptyList() : subject;
   }
   public static <K, V> Map<K, V> nonNull ( Map<K, V> subject ) {
      return subject == null ? Collections.emptyMap() : subject;
   }

   public static <T> T[] nullOrArray ( Collection<T> subject, IntFunction<T[]> builder ) {
      if (subject == null) return null;
      return subject.toArray( builder.apply( subject.size() ) );
   }
   public static <T> T[] toArray( Collection<T> subject, IntFunction<T[]> builder ) {
      if (subject == null) return builder.apply( 0 );
      return subject.toArray( builder.apply( subject.size() ) );
   }
   public static String[] stringArray ( Collection<String> subject ) {
      return toArray( subject, String[]::new );
   }
   public static <T extends Collection> T nullIfEmpty ( T subject ) {
      return isEmpty( subject ) ? null : subject;
   }

   public static boolean isEmpty ( Collection subject ) {
      return subject == null || subject.isEmpty();
   }
   public static <T,Q extends T> boolean contains ( Collection<T> subject, Q query ) {
      return subject == null ? false : subject.contains( query );
   }
}