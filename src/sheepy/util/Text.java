package sheepy.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

public class Text {

   public static String toString ( IntStream in ) {
      return in.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
   }

   public static String toString ( CharSequence delimiter, Collection<?> list ) {
      if ( list == null || list.isEmpty() ) return "";
      return String.join( delimiter, list.stream().map( Object::toString ).toArray( String[]::new ) );
   }
   public static String toString ( CharSequence prefix, CharSequence delimiter, CharSequence suffix, Collection<?> list ) {
      return toString( prefix, delimiter, suffix, list, "" );
   }
   public static String toString ( CharSequence prefix, CharSequence delimiter, CharSequence suffix, Collection<?> list, CharSequence ifEmpty ) {
      if ( list == null || list.isEmpty() ) return ifEmpty == null ? null : ifEmpty.toString();
      StringJoiner joiner = new StringJoiner( delimiter, prefix, suffix );
      list.stream().map( Object::toString ).forEach( joiner::add );
      return joiner.toString();
   }
   public static <T> String toString ( CharSequence delimiter, Collection<T> list, Function<T,? extends CharSequence> map ) {
      if ( list == null || list.isEmpty() ) return "";
      StringJoiner joiner = new StringJoiner( delimiter );
      list.stream().map( map ).forEach( joiner::add );
      return joiner.toString();
   }

   public static String nonNull( CharSequence s ) { return ifNull( s, "" ); }
   public static String ifNull( CharSequence s, String ifnull ) {
      return s == null ? ifnull: s.toString();
   }

   public static String trim( CharSequence s ) {
      return s == null ? null : s.toString().trim();
   }


   public static String unquote ( CharSequence subject, char start )           { return unquote( subject, start, start, null ); }
   public static String unquote ( CharSequence subject, char start, char end ) { return unquote( subject, start, end, null ); }
   public static String unquote ( CharSequence subject, char start, UnaryOperator<String> strip ) {
      return unquote( subject, start, start, strip );
   }
   public static String unquote ( CharSequence subject, char start, char end, UnaryOperator<String> strip ) {
      if ( subject == null ) return null;
      int len = subject.length();
      if ( len < 2 || subject.charAt( 0 ) != start || subject.charAt( len - 1 ) != end ) {
         return subject.toString();
      }
      String result = subject.subSequence( 1, len - 1 ).toString();
      return strip == null ? result : strip.apply(result);
   }

   public static String ellipsis ( CharSequence text, int max ) {
      return ellipsisAfter( text, max );
   }
   public static String ellipsisBefore ( CharSequence text, int max ) {
      if ( text == null ) return null;
      if ( max <= 0 ) throw new IllegalArgumentException();
      String txt = text.toString().replaceAll( "[\r\n]+", " " ).trim();
      return txt.length() < max ? txt : '…' + txt.substring( txt.length()-max, txt.length() );
   }
   public static String ellipsisAfter ( CharSequence text, int max ) {
      if ( text == null ) return null;
      if ( max <= 0 ) throw new IllegalArgumentException();
      String txt = text.toString().replaceAll( "[\r\n]+", " " ).trim();
      return txt.length() < max ? txt : txt.substring( 0, max-1 ) + '…';
   }
   public static String ellipsisAround ( CharSequence text, int position, int around ) {
      return ellipsisAround( text, position, 'λ', around, around );
   }
   public static String ellipsisAround ( CharSequence text, int position, char infix, int before, int after ) {
      if ( text == null ) return null;
      position = Math.max( 0, Math.min( position, text.length() ) );
      return ellipsisBefore( text.subSequence( 0, position ), before ) + infix + ellipsisAfter( text.subSequence( position, text.length() ), after );
   }
   public static String ellipsisWithin ( CharSequence text, int size ) {
      return ellipsisWithin( text, size, size );
   }
   public static String ellipsisWithin ( CharSequence text, int head, int tail ) {
      if ( text == null ) return null;
      if ( text.length() <= head + tail ) return text.toString();
      return ellipsisAfter( text, head ) + ellipsisBefore( text, tail ).toString().substring( 1 );
   }

   public static String toCrLf ( CharSequence text ) {
      if ( text == null ) return null;
      return text.toString().replaceAll( "(?<!\r)\n", "\r\n" );
   }

   public static String toLf ( CharSequence text ) {
      if ( text == null ) return null;
      return text.toString().replaceAll( "\r\n", "\n" );
   }

   @SuppressWarnings("deprecation")
   public static String escapeUrl ( CharSequence text ) {
      if ( text == null ) return null;
      try {
         return URLEncoder.encode( text.toString(), "UTF-8" );
      } catch ( UnsupportedEncodingException ex ) {
         return URLEncoder.encode( text.toString() );
      }
   }

   public static String escapeJavaScript ( CharSequence text ) {
      if ( text == null ) return null;
      return text.toString().replaceAll( "[\"'`\n\r]", "\\$0" );
   }

   public static String escapeXml ( CharSequence text ) {
      if ( text == null ) return null;
      return text.toString()
            .replaceAll( "&", "&#38;" ).replaceAll( "/" , "&#34;" )
            .replaceAll( "'", "&#39;" ).replaceAll( "\"", "&#47;" )
            .replaceAll( "<", "&lt;"  ).replaceAll( "<" , "&gt;"  );
   }
}