package sheepy.util.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;

/**
 * A factory that builds predicates for collections (nullable).
 */
public interface CollectionPredicate {

   public static <T,C extends Collection<? extends T>> Predicate<C> hasItem () { return min(1); }
   public static <T,C extends Collection<? extends T>> Predicate<C> isEmpty () { return max(1); }

   public static <T,C extends Collection<? extends T>> Predicate<C> min ( int i ) { return e -> e != null && e.size() >= i; }
   public static <T,C extends Collection<? extends T>> Predicate<C> max ( int i ) { return e -> e == null || e.size() <= i; }
   public static <T,C extends Collection<? extends T>> Predicate<C> size ( int min, int max ) {
      assert( min <= max );
      return e -> e == null ? min <= 0 : ( e.size() >= min && e.size() <= max );
   }
   public static <T,C extends Collection<? extends T>> Predicate<C> hasDuplicate () {
      return e -> e != null && new HashSet<Object>( e ).size() <  e.size();
   }
   public static <T,C extends Collection<? extends T>> Predicate<C> noDuplicate  () {
      return e -> e == null || new HashSet<Object>( e ).size() >= e.size();
   }

   /** true if collection only have items on the candidate list, and nothing else */
   public static <T,C extends Collection<? extends T>> Predicate<C> onlyContains ( C items ) {
      return e -> e == null || e.stream().allMatch( items::contains );
   }
   /** true if collection does not have any items on the candidate list */
   public static <T,C extends Collection<? extends T>> Predicate<C> notContains ( C items ) {
      return e -> e == null || ! e.stream().anyMatch( items::contains );
   }
   /** true if collection have any items on the candidate list */
   public static <T,C extends Collection<? extends T>> Predicate<C> contains ( C items ) {
      return e -> e != null && e.stream().anyMatch( items::contains );
   }

   public static <T,C extends Collection<? extends T>> Predicate<C> not ( Predicate<C> predicate ) {
      return e -> ! predicate.test( e );
   }
   public static <T,C extends Collection<? extends T>> Predicate<C> andAlso ( Predicate<C> ... predicates ) {
      return e -> Arrays.stream( predicates ).allMatch( i -> i.test( e ) );
   }
   public static <T,C extends Collection<? extends T>> Predicate<C> orElse ( Predicate<C> ... predicates ) {
      return e -> Arrays.stream( predicates ).anyMatch( i -> i.test( e ) );
   }
}