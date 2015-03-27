package sheepy.cocodoc.worker.task;

import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Pattern;
import sheepy.util.collection.CollectionPredicate;
import static sheepy.util.collection.CollectionPredicate.onlyContains;

/**
 * This class is unused because it should be implemented as quoted printable or other intelligence wrap instead.
 * Cutting a word or tag into two is not fun.
 */
public class TaskWrap extends Task {

   @Override public Action getAction () { throw new UnsupportedOperationException("Deprecated"); }

   private static final Predicate<List<String>> validate = CollectionPredicate.<List<String>>size( 0, 2 ).and( onlyContains( Pattern.compile( "[1-9]\\d{0,4}|(cr)?lf" ) ) );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "wrap() task accepts a number of characters (default 72), and 'crlf' or 'lf' to indicate line breaks (default same as platform)"; }

   @Override public void run () {
      int width = 72, newLfCount = 0, oldLfCount = 0;
      String lf = System.lineSeparator();
      for ( String s : getParams() ) {
         if ( s.equals( "crlf" ) ) lf = "\r\n";
         else if ( s.equals( "lf" ) ) lf = "\n";
         else width = Integer.parseInt( s );
      }
      int lfWidth = lf.length();

      StringBuilder text = getBlock().getText();
      for ( int i = 0, counter = 0, len = text.length() ; i < len ; i++ ) {
         char c = text.charAt( i );
         if ( c == '\r' || c == '\n' ) {
            counter = 0;
            ++oldLfCount;
         } else {
            ++counter;
            if ( Character.isHighSurrogate( c ) ) {
               ++i;
               if ( i >= len ) break;
            }
            if ( counter >= width ) {
               text.insert( i, lf );
               i += lfWidth;
               len += lfWidth;
               counter = 0;
               ++newLfCount;
            }
         }
      }
      log.log( Level.FINE, "Wrapped {1} lines into {2} lines of {0} char max.", new Object[]{ width, oldLfCount+1, newLfCount+oldLfCount } );
      getBlock().setText( text );
   }
}