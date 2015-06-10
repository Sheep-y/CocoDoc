package sheepy.util.text;

import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Text {
   public static final Charset UTF16 = Charset.forName("UTF-16");
   public static final Charset UTF8 = Charset.forName("UTF-8");

   public static String toString ( IntStream in ) {
      return in.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
   }

   public static String toString ( Stream<?> in ) {
      return toString( ", ", in );
   }

   public static String toString ( Collection<?> list ) {
      return toString( ", ", list );
   }

   public static String toString ( CharSequence delimiter, Stream<?> stream ) {
      return toString( delimiter, stream.map( Object::toString ).collect( Collectors.toList() ) );
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

   public static String nonNull( CharSequence s ) {
      return ifNull( s, "" );
   }

   public static String ifNull( CharSequence s, String ifnull ) {
      return s == null ? ifnull: s.toString();
   }

   public static String trim( CharSequence s ) {
      return s == null ? null : s.toString().trim();
   }


   public static String unquote ( CharSequence subject, char start )                              { return unquote( subject, start, start, null  ); }
   public static String unquote ( CharSequence subject, char start, char end )                    { return unquote( subject, start, end  , null  ); }
   public static String unquote ( CharSequence subject, char start, UnaryOperator<String> strip ) { return unquote( subject, start, start, strip ); }
   public static String unquote ( CharSequence subject, char start, char end, UnaryOperator<String> strip ) {
      if ( subject == null ) return null;
      int len = subject.length();
      if ( len < 2 || subject.charAt( 0 ) != start || subject.charAt( len - 1 ) != end ) {
         return subject.toString();
      }
      String result = subject.subSequence( 1, len - 1 ).toString();
      return strip == null ? result : strip.apply( result );
   }
   public static String unquote ( CharSequence subject, char[] start ) {
      if ( subject == null ) return null;
      for ( char chr : start ) {
         String result = unquote( subject, chr );
         if ( result.length() != subject.length() ) return result;
      }
      return subject.toString();
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
      String txt = text.toString().replaceAll( "[\r\n]+", " " ).trim();
      if ( txt.length() <= head + tail ) return txt.toString();
      return ellipsisAfter( txt, head ) + ellipsisBefore( txt, tail ).toString().substring( 1 );
   }

   public static String toCrLf ( CharSequence text ) {
      if ( text == null ) return null;
      return text.toString().replaceAll( "(?<!\r)\n", "\r\n" );
   }

   public static String toLf ( CharSequence text ) {
      if ( text == null ) return null;
      return text.toString().replaceAll( "\r\n", "\n" );
   }

   /**
    * Return an object that, when toString is called, calls supplied function and use it as result.
    * The result is cached in a SoftReference.
    *
    * @param func Supplier of toString result
    * @return Object with deferred toString execution.
    */
   public static Object defer ( Supplier<? extends CharSequence> func ) {
      return new Object () {
         private SoftReference<String> cache;
         @Override public synchronized String toString() {
            CharSequence result = null;
            if ( cache != null ) result = cache.get();
            if ( result == null )
               cache = new SoftReference<>( nonNull( result = func.get() ) );
            return result.toString();
         }
      };
   }
}