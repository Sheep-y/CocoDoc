package sheepy.cocodoc.worker.parser;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sheepy.cocodoc.CocoConfig;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.BlockStats;
import sheepy.cocodoc.worker.parser.coco.XmlNode;
import sheepy.cocodoc.worker.parser.coco.XmlParser;
import sheepy.util.text.Text;

public class ParserHtml extends Parser {
   private String lastId = null;

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
                  if ( node.hasChildren() ) {
                     String cocoName = getCocoAttr( node.getValue() );
                     if ( cocoName != null )
                        handleCocoDataAttr( cocoName, node.getAttributeValue().toString(), node.getParent() );
                     else if ( isId( node.getValue() ) && ! isDisabled( node.getParent() ) )
                        lastId = node.getAttributeValue().toString();
                  }
            }
         });
         context.stats().setVar( VAR_HEADER, title );
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

   /*******************************************************************************************************************/
   // Generic checkers

   public static boolean isId ( CharSequence tagName ) {
      return tagName.length() == 2
            && tagName.toString().toLowerCase().equals( "id" );
   }

   private void updateId ( XmlNode node ) {
      XmlNode id = node.getAttribute( "id" );
      if ( id != null && id.hasChildren() )
         lastId = id.getAttributeValue().toString();
   }

   private boolean isDisabled ( XmlNode node ) {
      assert( node.getType() == XmlNode.NODE_TYPE.TAG );
      return node.hasAttribute( "data-coco-disabled" );
   }

   /*******************************************************************************************************************/
   // Headers

   public static String VAR_HEADER = "__html.headers__";

   public class Header {
      public final Header parent;
      public final XmlNode tag;
      public final String id;
      public final byte level;
      public volatile List<Header> children;

      private Header () { this( null, null, (byte) 0 ); }
      private Header ( Header parent, XmlNode tag, byte level ) {
         this.parent = parent;
         this.tag = tag;
         this.id = Text.nonNull( lastId );
         this.level = level;
      }

      private int index () {
         if ( parent == null ) return -1;
         return parent.children.indexOf( this ) + 1;
      }

      private StringBuilder indexString ( StringBuilder buf ) {
         if ( parent == null )
            return buf == null ? new StringBuilder( "0" ) : buf;

         if ( buf == null )
            buf = new StringBuilder();
         else
            buf.insert( 0, '.' );
         buf.insert( 0, index() );
         if ( parent != null ) parent.indexString( buf );

         return buf;
      }

      @Override public String toString() { return tag.getXml().toString(); }
   }

   private Header title = new Header();
   private Header lastHeader = title;
   private int headerCount = 0;

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
      if ( isDisabled( node ) ) return;
      CharSequence content = node.getXml();
      log( CocoConfig.MICRO, "Found header {0}", content );
      if ( node.stream( XmlNode.NODE_TYPE.TAG ).skip( 1 ).map( XmlNode::getValue ).anyMatch( ParserHtml::isHeader ) )
         throw new CocoParseError( "Recursive headers are ignored (" + content + ")" );

      // Identify id
      updateId( node );
      if ( lastId == null || lastId.isEmpty() )
         throw new CocoRunError( "No anchor found for header " + content );
      if ( lastHeader != null && lastId.equals( lastHeader.id )  )
         throw new CocoRunError( "Duplicate anchor " + lastId + " found for header " + content );

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
      Header h = new Header( lastHeader, node.clone().striptAttribute( "id" ), level );
      if ( lastHeader.children == null )
         lastHeader.children = new ArrayList<>();
      lastHeader.children.add( h );
      lastHeader = h;
      ++headerCount;
   }

   public void handleTitle ( XmlNode node ) {
      if ( isDisabled( node ) ) return;
      log( CocoConfig.MICRO, "Found title {0}", Text.defer( node::getXml ) );
      Header orig = title;
      title = new Header( null, node.clone().striptAttribute( "id" ), (byte) 0 );
      title.children = orig.children;
      if ( lastHeader == orig ) lastHeader = title;
   }

   /*******************************************************************************************************************/
   // Attributes

   private int indexCount = 0;
   private int glossaryCount = 0;

   public int getIndexCount () { return indexCount; }
   public int getGlossaryCount () { return glossaryCount; }

   public static String VAR_INDEX ( CharSequence name ) {
      if ( name == null || name.length() <= 0 )
         return "__html.index__";
      return "__html.index." + name + "__";
   }

   public static String VAR_GLOSSARY ( CharSequence name ) {
      if ( name == null || name.length() <= 0 )
         return "__html.glossary__";
      return "__html.glossary." + name + "__";
   }

   public static String getCocoAttr ( CharSequence tagName ) {
      if ( tagName.length() > 10 // "data-coco-"
            && tagName.charAt(0) == 'd' && tagName.charAt( 5 ) == 'c' && tagName.charAt( 9 ) == '-' ) {
         String name = tagName.toString();
         if ( name.startsWith( "data-coco-" ) )
            return name.substring( 10 );
      }
      return null;
   }

   private void handleCocoDataAttr ( String type, String key, XmlNode node ) {
      if ( isDisabled( node ) ) return;

      String full_type = type;
      String name = "";
      int pos = type.indexOf( '-' );
      if ( pos > 0 ) {
         name = type.substring( pos + 1 );
         type = type.substring( 0, pos );
      }

      if ( type.equals( "index" ) || type.equals( "glossary" ) ) {
         log( CocoConfig.NANO, "Found {0} attribute on {1}", type, node );

         String varKey = type.equals( "index" ) ? VAR_INDEX( name ) : VAR_GLOSSARY( name );
         BlockStats stats = context.getRoot().stats();

         // Get or create the index / glossary list
         List<XmlNode> list = null;
         try ( Closeable lock = stats.lockVar() ) {
            Map<String,List<XmlNode>> data = stats.createVar( varKey, new HashMap<>() ) ;
            list = data.get( key );
            if ( list == null ) data.put( key, list = new ArrayList<>(4) );
         } catch ( IOException ignored ) {}

         if ( type.equals( "index" ) )
            handleIndexData( list, full_type, node );
         else
            handleGlossaryData( list, full_type, node );

      } else {
         throw new CocoParseError( "Unknown HTML attribute: data-coco-" + type );
      }
   }

   /** Return index link */
   private void handleIndexData ( List<XmlNode> list, String name, XmlNode node ) {
      // Identify id
      updateId( node );
      if ( lastId == null || lastId.isEmpty() )
         throw new CocoRunError( "No anchor found for index " + node.getXml() );

      // Create and return an index link
      XmlNode result = new XmlNode( XmlNode.NODE_TYPE.TAG, "a", 0, 1 );
      if ( node.hasAttribute( "id" ) ) lastId = node.getAttribute( "id" ).getAttributeValue().toString();
      result.setAttribute( "href", '#' + lastId );
      CharSequence text = lastHeader.indexString( null ) + ". " + lastHeader.tag.getTextContent().toString().trim();
      result.add( new XmlNode( XmlNode.NODE_TYPE.TEXT, text, 0, 5 ) );

      ++ indexCount;
      log( CocoConfig.MICRO, "Saved index {0}", Text.defer( node::getXml ) );
      list.add( result );
   }

   /** Return glossary definition */
   private void handleGlossaryData ( List<XmlNode> list, String name, XmlNode node ) {
      //while ( node.getParent() != null ) {
         assert( node.getType() == XmlNode.NODE_TYPE.ATTRIBUTE );
      /************************************************************** Disabled so that *any* tag can be glossary
         String tagName = node.getValue().toString().toLowerCase();
         switch ( tagName ) {
            case "p":
            case "dd":
            case "article": case "aside": case "nav": case "section":
      */
               node = node.clone().striptAttribute( "id", "data-coco-" + name );
               list.add( node );
               ++glossaryCount;
               log( CocoConfig.MICRO, "Saved glossary {0}", node );
      /*
               return;

            case "dt":
               do {
                  node = node.nextElement();
                  if ( node != null && node.getType() == XmlNode.NODE_TYPE.ATTRIBUTE ) {
                     if ( node.getValue().toString().toLowerCase().equals( "dd" ) ) {
                        XmlNode item = node.striptAttribute( "id", "data-coco-" + name );
                        list.add( item );
                        ++glossaryCount;
                        if ( logDetails ) log( Level.FINEST, "Saved glossary {0}", item );
                     } else
                        return;
                  }
               } while ( node != null );
         }
         node = node.getParent();
      }
      */
   }
}