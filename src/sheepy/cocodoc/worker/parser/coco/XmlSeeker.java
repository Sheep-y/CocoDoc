package sheepy.cocodoc.worker.parser.coco;

import sheepy.cocodoc.worker.error.CocoRunError;
import sheepy.util.Text;

/**
 * This is a quick work; it does not follow spec.
 * Before following spec, the parser's position pattern need to be revised!
 * Consider replace with http://jsoup.org/
 */
public class XmlSeeker {
   private CharSequence text;
   private int start;
   private int end;
   private int elementEnd;
   private CharSequence currentMatch;

   private int mark;
   private CharSequence markMatch;

   public XmlSeeker() { reset(); }

   public XmlSeeker(CharSequence text) { reset(text); }

   public XmlSeeker reset() { return reset(0); }
   public XmlSeeker reset( CharSequence text ) { return reset( text, 0 ); }
   public XmlSeeker reset( CharSequence text, int pos ) { this.text = text; return reset( pos ); }
   public XmlSeeker reset( int pos ) {
      start = pos;
      currentMatch = markMatch = null;
      end = elementEnd = mark = -1;
      return this;
   }

   public CharSequence text() { return text; }
   public int start() { return start; }
   public int end() {
      if ( end < 0 && currentMatch != null ) {
         if ( currentMatch.charAt( 0 ) == '<' ) {
            end = text.toString().indexOf( '>', start + currentMatch.length() );
            if ( end >= 0 ) ++end;
         } else {
            int one = text.toString().indexOf( '\'', start );
            int two = text.toString().indexOf( '"', start );
            end = Math.min( one > 0 ? one : two, two > 0 ? two : one );
         }
      }
      return end;
   }

   private void checkText () {
      if ( text == null ) throw new IllegalStateException( "XmlSeeker uninitialised." );
   }

   private void checkMatch ( CharSequence name ) {
      checkText();
      if ( name == null || name.length() == 0 ) throw new CocoRunError( "Cannot match empty tag or attribute." );
      String str = name.toString();
      if ( str.matches( "[<>\\s]" ) ) throw new CocoRunError( "Invalid tag or attribute." );
   }

   public XmlSeeker mark() {
      mark = start;
      currentMatch = markMatch;
      return this;
   }

   public XmlSeeker restoreMark() {
      start = mark;
      currentMatch = markMatch;
      end = elementEnd = -1;
      return this;
   }

   public boolean findTagBefore( CharSequence tagName ) {
      checkMatch( tagName );
      int pos = start, txtLen = text.length(), len = tagName.length();
      for ( int i = Math.min( pos-1, txtLen - len - 1 ); i >= 0 ; i-- )
         if ( tryMatchTag( i, tagName) ) return true;
      return false;
   }

   public boolean findAttributeValue( CharSequence attrName ) {
      checkMatch( attrName );
      int pos = start, txtLen = text.toString().indexOf( '>', start ), len = attrName.length();
      if ( txtLen < 0 ) return false;
      for ( int i = pos+1, max = txtLen-len-1 ; i < max ; i++ )
         if ( tryMatchAttribute( i, attrName ) ) return true;
      return false;
   }

   private boolean tryMatchAttribute( int pos, CharSequence name ) {
      char chr = text.charAt( pos );
      if ( Character.isWhitespace( chr ) && text.subSequence( pos+1, pos+1+name.length() ).equals( name ) ) {
         int txtLen = text.length(), len = name.length();
         pos += 1 + len; // Moves to the character after attribute
         chr = text.charAt( pos );
         if ( chr == '-' || Character.isAlphabetic( chr ) ) return false; // Not tag boundary

         // Found attribute name; find equal sign
         for ( ; pos < txtLen ; pos++ ) {
            chr = text.charAt( pos );
            if ( Character.isWhitespace( chr ) ) continue; // skip ws
            if ( chr != '=' ) return false; // Anything non-space, non-equal sign

            // Found equal; find value position
            for ( ++pos ; pos < txtLen ; pos++ ) {
               chr = text.charAt( pos );
               if ( Character.isWhitespace( chr ) ) continue; // skip ws
               if ( chr == '"' || chr == '\'' ) ++pos;
               return found( pos, name );
            }
         }
         throw new CocoRunError( "Cannot find value of attribute " + name + " (auto value fill not implemented)" );
      }
      return false;
   }

   private boolean tryMatchTag( int pos, CharSequence name ) {
      if ( text.charAt( pos ) == '<' && text.subSequence( pos + 1, pos + 1 + name.length() ).equals( name ) ) {
         int len = name.length();
         char chr = text.charAt( pos + len+1 );
         if ( chr == ':' || Character.isAlphabetic( chr ) ) return false; // Not tag boundary
         return found( pos, text.subSequence( pos, pos + 1 + len ) );
      }
      return false;
   }

   private boolean found( int pos, CharSequence match ) {
      start = pos;
      end = elementEnd = -1;
      currentMatch = match;
      return true;
   }

   @Override public String toString() {
      if ( text == null ) return "XmlSeeker(uninitialised)";
      String result = "XmlSeeker(" + start;
      if ( end >= 0 ) result += " to " + end;
      return result + '@' + text.length() + " chars: "
            + Text.ellipsis( text.subSequence( start, end() ).toString().split( "\\s" )[0], 10 ) + ")";
   }

}