package sheepy.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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

   public static String ellipsis ( CharSequence text, int max ) {
      if ( text == null ) return null;
      if ( max <= 0 ) throw new IllegalArgumentException();
      String txt = text.toString().replaceAll( "[\r\n]+", " " ).trim();
      return txt.length() < max ? txt : txt.substring( 0, max-1 ) + "…";
   }

   public static String toCrLf ( CharSequence text ) {
      return text.toString().replaceAll( "(?<!\r)\n", "\r\n" );
   }

   public static String toLf ( CharSequence text ) {
      return text.toString().replaceAll( "\r\n", "\n" );
   }

   public static String escapeUrl ( CharSequence text ) {
      try {
         return URLEncoder.encode( text.toString(), "UTF-8" );
      } catch (UnsupportedEncodingException ex) {
         return URLEncoder.encode( text.toString() );
      }
   }

   public static String escapeJavaScript ( CharSequence text ) {
      return text.toString().replaceAll( "[\"'`\n\r]", "\\$0" );
   }

   public static String escapeXml ( CharSequence text ) {
      return text.toString()
            .replaceAll( "&", "&#38;" ).replaceAll( "/" , "&#34;" )
            .replaceAll( "'", "&#39;" ).replaceAll( "\"", "&#47;" )
            .replaceAll( "<", "&lt;"  ).replaceAll( "<" , "&gt;"  );
   }
}