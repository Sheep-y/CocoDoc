package sheepy.cocodoc.worker.parser.coco;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import sheepy.util.collection.NullData;
import sheepy.util.text.Escape;
import sheepy.util.text.Text;

public class XmlNode implements Cloneable {

   public static enum NODE_TYPE {
      TAG,
      ATTRIBUTE,
      VALUE,
      TEXT,
      DIRECTIVE,
      COMMENT
   }

   NODE_TYPE type;
   CharSequence value;
   TextRange range;
   List<XmlNode> child;
   XmlNode parent;

   public XmlNode ( NODE_TYPE type, CharSequence value, int start, int end ) {
      this.type = type;
      this.value = value != null && value.length() > 0 ? value : null;
      if (start < 0 && value != null) {
         start = end - value.length();
      }
      this.range = new TextRange(start, end).setContext( this );
   }

   public NODE_TYPE getType() { return type; }

   public CharSequence getValue() { return value; }

   public boolean hasAttribute ( CharSequence attr ) {
      return getAttribute( attr ) != null;
   }
   public XmlNode getAttribute ( CharSequence attr ) {
      if ( child == null ) return null;
      return child.stream().filter( e -> e.getType() == NODE_TYPE.ATTRIBUTE && e.getValue().equals( attr ) ).findFirst().orElse( null );
   }

   public CharSequence getAttributeValue() {
      assert( type == NODE_TYPE.ATTRIBUTE );
      if ( ! hasChildren() ) return getValue();
      return Escape.unHtml( Text.unquote( children(0).getValue(), new char[]{ '\'', '"' } ) );
   }

   public TextRange getRange() { return range; }

   public TextRange range( TextRange range ) {
      return range( range.start, range.end );
   }
   public TextRange range( int start ) {
      return range( start, start );
   }
   public TextRange range( int start, int end ) {
      return new TextRange( start, end ).setContext( this );
   }

   public boolean isValid() { return range != null && range.isValid(); }

   public boolean hasChildren () { return child != null && ! child.isEmpty(); }
   public XmlNode children ( int i ) { return child.get( i ); }
   public List<XmlNode> children () { return NullData.nonNull(child); }

   @Override public String toString() {
      String typeStr = type == null ? "XmlNode" : type.name().toLowerCase();
      return typeStr + '[' + range.start + ',' + range.end + "]: " + Text.ellipsisWithin(value, 8);
   }

   @Override public XmlNode clone() {
      try {
         XmlNode result = (XmlNode) super.clone();
         if ( child != null ) {
            result.child = new ArrayList<>( child.size() );
            for ( XmlNode node : child ) {
               node = node.clone();
               result.child.add( node );
               node.parent = this;
            }
         }
         result.parent = null;
         return result;
      } catch ( CloneNotSupportedException ignored ) { return null; }
   }

   static StringBuilder prefix = new StringBuilder();
   private CharSequence treeToString () {
      StringBuilder build = new StringBuilder( prefix + toString() ).append( '\n' );
      if ( child != null ) {
         prefix.append( "--" );
         for ( XmlNode o : children() ) build.append( o.treeToString()).append( '\n' );
         prefix.setLength( prefix.length() - 2 );
      }
      build.setLength( build.length() - 1 );
      return build;
   }

   public CharSequence getTextContent() { return getTextContent( new StringBuilder() ); }
   private CharSequence getTextContent( StringBuilder result ) {
      switch ( getType() ) {
         case COMMENT  :
         case DIRECTIVE:
            return result.append( getValue() );
         case TEXT     :
            return result.append( Escape.unHtml( getValue() ) );
         case VALUE    :
            return result.append( getAttributeValue() );
         case ATTRIBUTE:
         case TAG      :
            for ( XmlNode child : children() ) {
               switch ( child.getType() ) {
                  case TAG  :
                  case TEXT :
                  case VALUE:
                     child.getTextContent( result );
               }
            }
      }
      return result;
   }

   public CharSequence getXml () { return getXml( new StringBuilder() ); }
   private CharSequence getXml( StringBuilder result ) {
      switch ( getType() ) {
         case COMMENT  :
         case DIRECTIVE:
         case TEXT     :
         case VALUE    :
            return result.append( getValue() );

         case ATTRIBUTE:
            result.append( getValue() );
            if ( hasChildren() )
               result.append( '=' ).append( children(0).getValue().toString() );
            break;

         case TAG      :
            result.append( '<' ).append( getValue() );
            List<XmlNode> nodes = children();
            int i = 0, len = nodes.size();

            // Attributes
            for ( ; i < len ; i++ ) {
               XmlNode child = nodes.get( i );
               if ( child.getType() == NODE_TYPE.ATTRIBUTE )
                  child.getXml( result.append( ' ' ) );
               else
                  break;
            }

            // Self-closible tags from HTML4 (tags with no end tag) and HTML5 (void elements)
            if ( i >= len ) // If we have no non-attribute children
               switch ( getValue().toString().toLowerCase() ) {
                  case "area": case "base": case "br": case "col": case "command": case "embed":
                  case "hr": case "img": case "input": case "keygen": case "link": case "menuitem":
                  case "meta": case "param": case "source" : case "track": case "wbr":
                     return result.append( "/>" );
               }

            // Non self closing end tag
            result.append( '>' );
            for ( ; i < len ; i++ )
               nodes.get( i ).getXml( result );
            result.append( "</" ).append( getValue() ).append( ">" );
      }
      return result;
   }

   /*******************************************************************/
   // Modification

   List<XmlNode> makeList () {
      if (child == null) {
         child = new ArrayList<>(4);
      }
      return child;
   }

   public void add ( XmlNode node ) {
      if ( node.value == null || ! node.isValid() || node.range.length() <= 0 ) {
         return;
      }
      if ( node.parent != null ) {
         node.parent.child.remove( node );
      }
      makeList().add( node );
      node.parent = this;
   }

   public void remove ( XmlNode node ) {
      if ( child == null || ! child.remove( node ) ) throw new IndexOutOfBoundsException();
   }

   public void setAttribute ( CharSequence attr, CharSequence value ) {
      // TODO: Adjust range
      XmlNode node = getAttribute( attr );
      if ( node != null ) {
         if ( value == null ) { // Exists and delete
            remove( node );
            return;
         } else if ( node.hasChildren() ) // Exists and set
            node.child.clear();
      } else {
         if ( value == null ) return; // Non exists and delete
         add( node = new XmlNode( NODE_TYPE.ATTRIBUTE, attr, 0, attr.length() ) ); // Non exists and set
      }
      // Set
      if ( value != null )
         node.add( new XmlNode( NODE_TYPE.VALUE, '"' + Escape.xml( value ) + '"', 0, value.length()+2 ) );
   }

   void shiftInserted (int atPosition, int deviation) {
      range.shiftInserted(atPosition, deviation);
      for (XmlNode c : children() ) {
         c.shiftInserted(atPosition, deviation);
      }
   }

   void shiftDeleted (TextRange range, boolean stayWhenDeleted) {
      range.shiftDeleted(range, stayWhenDeleted);
      for (XmlNode c : children() ) {
         c.shiftDeleted(range, stayWhenDeleted);
      }
   }

   public XmlNode striptAttribute ( String ... attr ) {
      this.stream( XmlNode.NODE_TYPE.TAG ).forEach( e -> {
         for ( String a : attr ) e.setAttribute( a, null );
      });
      return this;
   }

   /*******************************************************************/
   // Navigation

   public XmlNode getParent() { return parent; }

   public XmlNode root () {
      return parent != null ? parent : this;
   }

   public Stream<XmlNode> stream () { return stream( null ); }
   public Stream<XmlNode> stream ( NODE_TYPE type ) {
      Builder<XmlNode> builder = Stream.builder();
      buildStream( builder, type );
      return builder.build();
   }

   private void buildStream ( Builder<XmlNode> builder, NODE_TYPE type ) {
      if ( ! isValid() ) return;
      if ( type == null || type == getType() ) builder.add( this );
      for ( XmlNode e : children() )
         e.buildStream( builder, type );
   }

   XmlNode findChildrenOver ( int position ) {
      if ( ! range.isValid() || range.start > position || range.end <= position ) {
         return null;
      }
      if ( child != null ) {
         for ( XmlNode c : child ) {
            XmlNode result = c.findChildrenOver(position);
            if ( result != null ) {
               return result;
            }
         }
      }
      return this;
   }

   XmlNode findChildrenBefore ( int position ) {
      XmlNode result = this;
      for ( XmlNode node : children() ) {
         if ( ! node.range.isValid() ) continue;
         if ( node.range.start < position ) result = node;
         if ( node.range.end >= position ) break;
      }
      if ( result != this ) return result.findChildrenBefore( position );
      return result;
   }

   public XmlNode before() {
      if ( parent == null ) return null;
      List<XmlNode> child = parent.child;
      if ( child.get(0) == this ) return parent;
      return child.get( child.indexOf( this ) - 1 );
   }

   public XmlNode after() {
      if ( parent == null ) return null;
      List<XmlNode> child = parent.child;
      if ( child.get( child.size()-1 ) == this ) return parent;
      return child.get( child.indexOf( this ) + 1 );
   }

   public int index () {
      if ( parent == null ) return -1;
      return parent.children().indexOf( this );
   }

   public XmlNode nextElement () {
      if ( parent == null ) return null;
      List<XmlNode> child = parent.child;
      for ( int i = index()+1 ; i < child.size() ; i++ ) {
         if ( child.get( i ).getType() == NODE_TYPE.TAG )
            return child.get( i );
      }
      return null;
   }
}