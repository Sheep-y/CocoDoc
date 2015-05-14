package sheepy.cocodoc.worker.parser.coco;

import java.util.Objects;
import java.util.logging.Level;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.parser.Parser;
import sheepy.util.Text;

/**
 * Given a string, parse it into XmlNodes.
 *
 * identifier is very loose, but structure requirement is very tight.
 *
 */
public class XmlParser extends Parser {

   private static final int messageQuoteLimit = 20;

   public XmlParser ( Block context ) {
      start( context );
   }

   @Override public Parser clone() { return new XmlParser( context ); }
   @Override public void start( Block context, String text ) {}
   @Override public CharSequence get() { return text; }

   /***********************************************************************************************************/

   private CharSequence text;
   private int pos = 0;
   private XmlNode root;

   private boolean done () {
      return pos >= text.length();
   }
   private boolean isWS( char chr ) {
      return Character.isWhitespace( chr ) || Character.isISOControl( chr );
   }
   private boolean isTagEnd( char chr ) {
      return chr == '>' || chr == '<' || chr == '\u0000' || ( chr == '/' && peek2() == '>' );
   }
   private boolean isIdentifier( char chr ) {
      return chr != '/' && chr != '<' && chr != '>' && chr != '&' && chr != '=' && ! isWS( chr );
   }


   private char peek () {
      return ! done() ? text.charAt( pos ) : '\u0000';
   }
   private char peek2 () {
      return peek(1);
   }
   private char peek ( int i ) {
      return pos < text.length()-i ? text.charAt( pos+i ) : '\u0000';
   }
   private boolean nextIs( CharSequence txt ) {
      if ( text.length() - pos < txt.length() ) return false;
      return Objects.equals( txt, text.subSequence( pos, pos + txt.length() ) );
   }
   private CharSequence ellipsis( int start ) {
      return ellipsis( start, text.length() );
   }
   private CharSequence ellipsis( int start, int end ) {
      return Text.ellipsis( text.subSequence( start, end ), messageQuoteLimit );
   }

   private char skipWS() {
      while ( pos < text.length() && isWS( peek() ) ) pos++;
      return peek();
   }
   private CharSequence remaining () {
      int start = pos;
      return text.subSequence( start, pos = text.length() );
   }
   private CharSequence until ( char find, boolean consume, String warningIfNotFound ) {
      int deviate = consume ? 1 : 0;
      for ( int i = pos, len = text.length() ; i < len ; i++ ) {
         if ( text.charAt( i ) == find ) return text.subSequence( pos, pos = i + deviate );
      }
      if ( warningIfNotFound != null ) {
         CharSequence result = remaining();
         log( Level.WARNING, warningIfNotFound, Text.ellipsis( result, messageQuoteLimit ) );
         return result;
      }
      return null;
   }
   private CharSequence until ( CharSequence find, boolean consume, String warningIfNotFound ) {
      int start = pos;
      int i = text.subSequence( pos, text.length() ).toString().indexOf( find.toString() );
      if ( i >= 0 ) return text.subSequence( start, pos += i + ( consume ? find.length() : 0 ) );
      if ( warningIfNotFound != null ) {
         CharSequence result = remaining();
         log( Level.WARNING, warningIfNotFound, Text.ellipsis( result, messageQuoteLimit ) );
         return result;
      }
      return null;
   }
   private CharSequence parseIdentifier() {
      CharSequence tagName = null;
      int oldPos = pos;
      if ( peek() == '<' ) {
         pos++;
         if ( peek() == '/' ) pos++;
      }
      for ( int i = pos, len = text.length() ; i < len ; i++ ) {
         if ( ! isIdentifier( text.charAt( i ) ) ) {
            tagName = text.subSequence( pos, i );
            pos = i;
            break;
         }
      }
      if ( tagName == null ) {
         log( Level.WARNING, "Unended identifier: {0}", ellipsis( oldPos ) );
         return remaining();
      }
      return tagName;
   }

   public XmlNode parse( CharSequence text ) {
      //if ( root != null ) throw new IllegalStateException( "Please create a new parser." );
      this.text = text;
      pos = 0;
      root = new XmlNode( null, "", 0, text.length() );
      while ( ! done() ) {
         parseTagContent( root );
         if ( nextIs( "</" ) ) { // Must be close tag
            XmlNode outer = new XmlNode( XmlNode.NODE_TYPE.TAG, parseIdentifier(), 0, pos );
            if ( outer.value == null ) {
               log( Level.WARNING, "Invalid tag: {0}", ellipsis( pos ) );
               root.add(new XmlNode( XmlNode.NODE_TYPE.TEXT, remaining(), -1, pos ) );
            } else {
               log( Level.WARNING, "Close tag without open tag: {0}", Text.ellipsis( outer.value, messageQuoteLimit ) );
               if ( root.child != null ) {
                  outer.makeList().addAll( root.child );
                  root.child.clear();
               }
               root.add( outer );
            }
         }
      }
      return root;
   }

   private void parseTagContent ( XmlNode parent ) {
      while ( ! done() ) {
         parent.add(new XmlNode( XmlNode.NODE_TYPE.TEXT, until( '<', false, null ), -1, pos ) );
         if ( peek() == '<' ) {
            char leadChar = peek2();
            if ( leadChar == '!' ) {
               if ( nextIs( "<!--") )
                  parent.add( new XmlNode( XmlNode.NODE_TYPE.COMMENT  , until( "-->", true, "End of comment not found in block: {0}" ), -1, pos ) );
               else if ( nextIs( "<![CDATA[") )
                  parent.add( new XmlNode( XmlNode.NODE_TYPE.TEXT     , until( "]]>", true, "End of CDATA not found in block: {0}"   ), -1, pos ) );
               else
                  parent.add( new XmlNode( XmlNode.NODE_TYPE.DIRECTIVE, until( '>'  , true, "End of doctype not found in block: {0}" ), -1, pos ) );
            }
            else if ( leadChar == '?' )
               parent.add(new XmlNode( XmlNode.NODE_TYPE.DIRECTIVE, until( "?>", true, "End of xml directive not found in block: {0}" ), -1, pos ) );
            else if ( leadChar == '/' && isIdentifier( peek(2) ) )
               return;
            else if ( isIdentifier( leadChar ) )
               parseTag( parent );
            else {
               log( Level.WARNING, "Unescaped '<'" );
               parent.add(new XmlNode( XmlNode.NODE_TYPE.TEXT, "<", pos, ++pos ) );
            }
         } else if ( ! done() ) {
            parent.add(new XmlNode( XmlNode.NODE_TYPE.TEXT, remaining(), -1, pos ) );
         }
      }
   }

   private void parseTag ( XmlNode parent ) {
      assert( peek() == '<' );

      int start = pos;
      CharSequence tagName = parseIdentifier();
      if ( tagName == null ) return;
      XmlNode tag = new XmlNode( XmlNode.NODE_TYPE.TAG, tagName, start, pos );
      parent.add( tag );

      // Parse attribute
      skipWS();
      parseAttribute( tag );
      if ( done() ) {
         log( Level.WARNING, "End tag not found in block: {0}", ellipsis( start ) );
         tag.range.end = pos;
         return;
      }

      // Check self-closing tag
      char chr = peek();
      if ( chr != '<' ) {
         if ( chr == '/' && peek2() == '>' ) {
            pos += 2;
            tag.range.end = pos;
            return;
         }
         ++pos; // skip '>'
      } else {
         log( Level.WARNING, "Open tag before end tag: {0}", ellipsis( start ) );
      }

      parseTagContent( tag );
      char afterChar = peek( tag.value.length() + 2 );
      // Check that the matching close tag really match
      if ( done() || ! nextIs( "</" + tag.value ) || ( afterChar != '>' && ! isWS( afterChar ) ) ) {
         log( Level.WARNING, "Unclosed tag: {0}", ellipsis( start ) );
         return;
      }
      until( '>', true, "Unclosed tag: {0}" );
      tag.range.end = pos;
   }

   private void parseAttribute ( XmlNode parent ) {
      assert( parent.type == XmlNode.NODE_TYPE.TAG && parent.child == null );

      for ( ; pos < text.length() ; pos++ ) {
         char chr = peek();
         if ( isWS( chr ) ) continue;
         if ( isTagEnd( chr ) ) return;

         // Match attribute name
         int start = pos;
         CharSequence name = parseIdentifier();
         int end = pos;
         XmlNode attr = new XmlNode( XmlNode.NODE_TYPE.ATTRIBUTE, name, start, end );
         parent.add( attr );

         parseAttributeValue( attr );
         if ( done() || isTagEnd( peek() ) ) return;
      }
   }

   private void parseAttributeValue( XmlNode attribute ) {
      char chr;
      // Find attribute value
      chr = skipWS();
      if (chr != '=') return;  // Valueless attribute. Just return.

      ++pos; // skip '='
      chr = skipWS(); // most likely a quote
      if ( isTagEnd( chr ) ) return;

      int start = pos;
      if ( chr == '"' || chr == '\'' ) {
         // Quoted attribute
         for ( pos++ ; pos < text.length() ; pos++ ) {
            if ( peek() == chr ) {
               ++pos;
               break;
            }
         }
      } else {
         // Unquoted attribute
         for ( ; pos < text.length() ; pos++ ) {
            if ( isWS( peek() ) || isTagEnd( peek() ) ) break;
         }
      }
      if ( pos != start ) {
         attribute.add(new XmlNode( XmlNode.NODE_TYPE.VALUE, text.subSequence( start, pos ), start, pos ) );
         attribute.range.end = pos;
      }
   }
}