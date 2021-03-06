package sheepy.util.collection;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A factory that builds predicates for collections (nullable).
 */
public interface CollectionPredicate {

   public static <C extends Collection> Predicate<C> hasItem () { return min(1); }
   public static <C extends Collection> Predicate<C> isEmpty () { return max(1); }

   public static <C extends Collection> Predicate<C> min ( int i ) { return e -> e != null && e.size() >= i; }
   public static <C extends Collection> Predicate<C> max ( int i ) { return e -> e == null || e.size() <= i; }
   public static <C extends Collection> Predicate<C> size ( int size ) { return size( size, size ); }
   public static <C extends Collection> Predicate<C> size ( int min, int max ) {
      assert( min <= max );
      return e -> e == null ? min <= 0 : ( e.size() >= min && e.size() <= max );
   }
   public static <T,C extends Collection<T>> Predicate<C> hasDuplicate () {
      return e -> e != null && new HashSet<>( e ).size() <  e.size();
   }
   public static <T,C extends Collection<T>> Predicate<C> noDuplicate  () {
      return e -> e == null || new HashSet<>( e ).size() >= e.size();
   }

   /** true if collection only have items on the candidate list, and nothing else */
   public static <T,C extends Collection<T>> Predicate<C> onlyContains ( C items ) {
      return e -> e == null || e.stream().allMatch( items::contains );
   }
   public static <C extends Collection<? extends CharSequence>> Predicate<C> onlyContains ( Pattern regx ) {
      return e -> {
         if ( e == null || e.isEmpty() ) return true;
         Matcher m = regx.matcher( "" );
         return e.stream().anyMatch( i -> m.reset( i ).matches() );
      };
   }

   /** true if collection does not have any items on the candidate list */
   public static <T,C extends Collection<T>> Predicate<C> notContains ( C items ) {
      return e -> e == null || ! e.stream().anyMatch( items::contains );
   }
   public static <C extends Collection<? extends CharSequence>> Predicate<C> notContains ( Pattern regx ) {
      return e -> {
         if ( e == null || e.isEmpty() ) return true;
         Matcher m = regx.matcher( "" );
         return ! e.stream().anyMatch( i -> m.reset( i ).matches() );
      };
   }

   /** true if collection have any items on the candidate list */
   public static <T,C extends Collection<T>> Predicate<C> contains ( C items ) {
      return e -> e != null && e.stream().anyMatch( items::contains );
   }
   public static <C extends Collection<? extends CharSequence>> Predicate<C> contains ( Pattern regx ) {
      return e -> {
         if ( e == null || e.isEmpty() ) return false;
         Matcher m = regx.matcher( "" );
         return e.stream().anyMatch( i -> m.reset( i ).matches() );
      };
   }
}