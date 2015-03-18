package sheepy.util;

import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

public class Text {

   public static String toString ( IntStream in ) {
      return in.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
   }

   public static String unquote ( CharSequence subject, char start )           { return unquote( subject, start, start, null ); }
   public static String unquote ( CharSequence subject, char start, char end ) { return unquote( subject, start, end, null ); }
   public static String unquote ( CharSequence subject, char start, UnaryOperator<String> strip ) {
      return unquote( subject, start, start, strip );
   }
   public static String unquote ( CharSequence subject, char start, char end, UnaryOperator<String> strip ) {
      if ( subject == null ) {
         return null;
      }
      int len = subject.length();
      if ( len < 2 || subject.charAt( 0 ) != start || subject.charAt( len - 1 ) != end ) {
         return subject.toString();
      }
      String result = subject.subSequence( 1, len - 1 ).toString();
      return strip == null ? result : strip.apply(result);
   }

}
