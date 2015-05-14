package sheepy.cocodoc.worker.parser;

import java.nio.charset.CodingErrorAction;
import sheepy.cocodoc.worker.parser.coco.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.worker.Block;
import sheepy.util.Escape;
import sheepy.util.Text;

public class ParserHtml extends Parser {
   private static final boolean logDetails = false;

   private String latestId = null;

   public ParserHtml () {}
   public ParserHtml ( Parser parent ) {
      super( parent );
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
      try {
         XmlNode doc = new XmlParser( context ).parse( text );
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
                  else if ( isId( node.getValue() ) && node.hasChildren() )
                     latestId = node.getAttributeValue().toString();
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

   @Override public CharSequence get () { throw new UnsupportedOperationException(); }

   public static boolean isId ( CharSequence tagName ) {
      return tagName.length() == 2
            && tagName.toString().toLowerCase().equals( "id" );
   }

   /*******************************************************************************************************************/
   // Headers

   public class Header {
      public final Header parent;
      public final XmlNode tag;
      public final String html;
      public final String id;
      public final byte level;
      public volatile List<Header> children;

      private Header () { this( null, null, null, (byte) 0 ); }
      private Header ( Header parent, XmlNode tag, CharSequence html, byte level ) {
         this.parent = parent;
         this.tag = tag;
         this.html = Text.nonNull( html );
         this.id = Text.nonNull( latestId );
         this.level = level;
      }

      @Override public String toString() { return html == null ? "" : html.toString(); }
   }

   private Header title = new Header();
   private Header lastHeader = title;
   private int headerCount = 0;

   public Header getHeaders () { return title; }
   public int getHeaderCount () { return headerCount; }

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
      if ( node.stream( XmlNode.NODE_TYPE.TAG ).skip( 1 ).map( XmlNode::getValue ).anyMatch( ParserHtml::isHeader ) )
         throw new CocoParseError( "Recursive headers are ignored (" + content + ")" );

      // Identify id
      node.stream( XmlNode.NODE_TYPE.ATTRIBUTE ).filter( e -> isId( e.getValue() ) && e.hasChildren() )
            .findFirst().ifPresent( e -> latestId = e.getAttributeValue().toString() );
      if ( latestId == null || latestId.isEmpty() )
         throw new CocoRunError( "No anchor found for header " + content );
      if ( lastHeader != null && latestId.equals( lastHeader.id )  )
         throw new CocoRunError( "Duplicate anchor " + latestId + " found for header " + content );

      // Moves to correct position in header tree
      byte level = Byte.parseByte( node.getValue().subSequence( 1, 2 ).toString() );

      if ( level == lastHeader.level ) {
         lastHeader = lastHeader.parent;

      } else if ( level > lastHeader.level ) { // Sublevel, nothing to change

      } else if ( level < lastHeader.level ) { // up level
         while ( lastHeader.parent != null && lastHeader.level >= level )
            lastHeader = lastHeader.parent;
      }

      // Insert to header tree
      Header h = new Header( lastHeader, node.clone(), content, level );
      if ( lastHeader.children == null )
         lastHeader.children = new ArrayList<>();
      lastHeader.children.add( h );
      lastHeader = h;
      ++headerCount;
   }

   public void handleTitle ( XmlNode node ) {
      CharSequence content = node.getXml();
      if ( logDetails ) log( Level.FINEST, "Found title {0}", content );
      Header orig = title;
      title = new Header( null, node.clone(), content, (byte) 0 );
      title.children = orig.children;
      if ( lastHeader == orig ) lastHeader = title;
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
         throw new CocoParseError( "Unknown HTML attribute: " + node.getValue() );
      }
   }

}