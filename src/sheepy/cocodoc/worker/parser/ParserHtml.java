package sheepy.cocodoc.worker.parser;

import sheepy.cocodoc.worker.parser.coco.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.worker.Block;

public class ParserHtml extends Parser {
   private static final boolean logDetails = false;

   private CharSequence text;

   public ParserHtml () {
      assert( text == null );
   }

   public ParserHtml ( Parser parent ) {
      super( parent );
      assert( text == null );
      ParserHtml p = (ParserHtml) parent;
      if ( p != null ) {
         title = p.title;
         lastHeader = p.lastHeader;
      }
   }

   @Override public ParserHtml clone() {
      return new ParserHtml( this );
   }

   @Override public void start ( Block context, String text ) {
      this.text = text;
      try {
         XmlNode doc = new XmlParser().parse( text );
         doc.stream().forEach(node -> {
            if ( node.getType() == null ) return;
            switch ( node.getType() ) {
               case TAG :
                  if ( isHeader( node.getValue() ) )
                     handleHeader( node );
                  else if ( isTitle( node.getValue() ) )
                     handleTitle( node );
                  break;

               case ATTRIBUTE :
                  String cocoName = getCocoAttr( node.getValue() );
                  if ( cocoName != null )
                     handleCocoDataAttr( cocoName, node );
            }
         });
      } catch ( CocoRunError | CocoParseError ex ) {
         throwOrWarn( ex );
      } finally {
         ParserHtml p = (ParserHtml) getParent();
         if ( p != null ) {
            p.title = title;
            p.lastHeader = lastHeader;
         }
      }
   }

   @Override public CharSequence get () {
      return text;
   }

   /*******************************************************************************************************************/
   // Headers

   public class Header {
      Header parent;
      XmlNode tag;
      CharSequence html;
      short level;
      List<Header> children;

      @Override public String toString() { return html.toString(); }
   }

   private Header title = new Header();
   private Header lastHeader = title;

   public static boolean isHeader ( CharSequence tagName ) {
      return tagName.length() == 2
            && ( tagName.charAt(0) == 'h' || tagName.charAt(0) == 'H' )
            && ( tagName.charAt( 1 ) >= '0' && tagName.charAt( 1 ) <= '9' );
   }

   public static boolean isTitle ( CharSequence tagName ) {
      return tagName.length() == 5
            && ( tagName.charAt(0) == 't' || tagName.charAt(0) == 'T' )
            && tagName.toString().toLowerCase().equals( "title" );
   }

   private void handleHeader ( XmlNode node ) {
      CharSequence content = node.getXml();
      if ( logDetails ) log( Level.FINEST, "Found header {0}", content );
      Header h = new Header();
      h.tag = node;
      h.html = content;
      h.level = Short.parseShort( node.getValue().subSequence( 1, 2 ).toString() );

      if ( h.level == lastHeader.level ) {
         lastHeader = lastHeader.parent;

      } else if ( h.level > lastHeader.level ) { // Sublevel, nothing to change

      } else if ( h.level < lastHeader.level ) { // up level
         while ( lastHeader.parent != null && lastHeader.level >= h.level )
            lastHeader = lastHeader.parent;
      }

      h.parent = lastHeader;
      if ( lastHeader.children == null )
         lastHeader.children = new ArrayList<>();
      lastHeader.children.add( h );
      lastHeader = h;
   }

   public void handleTitle ( XmlNode node ) {
      CharSequence content = node.getXml();
      if ( logDetails ) log( Level.FINEST, "Found title {0}", content );
      title.tag = node;
      title.html = content;
   }

   /*******************************************************************************************************************/
   // Attributes

   public static String getCocoAttr ( CharSequence tagName ) {
      if ( tagName.length() > 10 // "data-coco-"
            && tagName.charAt(0) == 'd' && tagName.charAt( 5 ) == 'c' && tagName.charAt( 9 ) == '-' ) {
         String name = tagName.toString();
         if ( name.startsWith( "data-coco-" ) )
            return name.substring( 10 );
      }
      return null;
   }

   private void handleCocoDataAttr ( String name, XmlNode node ) {
      if ( name.startsWith( "index" ) ) {
         if ( logDetails ) log( Level.FINEST, "Found index attribute {0} on {1}", node, node.getParent() );
      } else if ( name.startsWith( "glossary" ) ) {
         if ( logDetails ) log( Level.FINEST, "Found glossary attribute {0} on {1}", node, node.getParent() );
      } else {
         throwOrWarn( new CocoParseError( "Unknown HTML attribute: " + node.getValue() ) );
      }
   }

}