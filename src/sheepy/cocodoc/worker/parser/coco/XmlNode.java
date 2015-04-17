package sheepy.cocodoc.worker.parser.coco;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import sheepy.util.Text;
import sheepy.util.collection.NullData;

public class XmlNode {

   static enum NODE_TYPE {
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

   XmlNode ( NODE_TYPE type, CharSequence value, int start, int end ) {
      this.type = type;
      this.value = value != null && value.length() > 0 ? value : null;
      if (start < 0 && value != null) {
         start = end - value.length();
      }
      this.range = new TextRange(start, end).setContext( this );
   }

   public NODE_TYPE getType() { return type; }

   public CharSequence getValue() { return value; }

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

   public boolean isValid() { return range.isValid(); }

   public List<XmlNode> children() {
      return NullData.nonNull(child);
   }

   @Override public String toString() {
      String typeStr = type == null ? "XmlNode" : type.name().toLowerCase();
      return typeStr + '[' + range.start + ',' + range.end + "]: " + Text.ellipsisWithin(value, 8);
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

   /*******************************************************************/
   // Modification

   List<XmlNode> makeList() {
      if (child == null) {
         child = new ArrayList<>(4);
      }
      return child;
   }

   void add(XmlNode node) {
      if (node.value == null || node.range == null || node.range.start == node.range.end) {
         return;
      }
      if (node.parent != null) {
         node.parent.child.remove(node);
      }
      makeList().add(node);
      node.parent = this;
   }

   void shiftInserted(int atPosition, int deviation) {
      range.shiftInserted(atPosition, deviation);
      for (XmlNode c : children() ) {
         c.shiftInserted(atPosition, deviation);
      }
   }

   void shiftDeleted(TextRange range, boolean stayWhenDeleted) {
      range.shiftDeleted(range, stayWhenDeleted);
      for (XmlNode c : children() ) {
         c.shiftDeleted(range, stayWhenDeleted);
      }
   }

   /*******************************************************************/
   // Navigation

   public XmlNode root () {
      return parent != null ? parent : this;
   }

   Stream<XmlNode> stream() { return stream( null ); }
   Stream<XmlNode> stream( NODE_TYPE type ) {
      Builder<XmlNode> builder = Stream.builder();
      buildStream( builder, type );
      return builder.build();
   }

   private void buildStream( Builder<XmlNode> builder, NODE_TYPE type ) {
      if ( ! isValid() ) return;
      if ( type == null || type == getType() ) builder.add( this );
      for ( XmlNode e : children() )
         e.buildStream( builder, type );
   }

   XmlNode findContainer(int position) {
      if (!range.isValid() || range.start > position || range.end < position) {
         return null;
      }
      if (child != null) {
         for (XmlNode c : child) {
            XmlNode result = c.findContainer(position);
            if (result != null) {
               return result;
            }
         }
      }
      return this;
   }

   XmlNode findBefore( int position ) {
      XmlNode result = this;
      for ( XmlNode node : children() ) {
         if ( ! node.range.isValid() ) continue;
         if ( node.range.start < position ) result = node;
         if ( node.range.end >= position ) break;
      }
      if ( result != this ) return result.findBefore( position );
      return result;
   }

   XmlNode before() {
      if ( parent == null ) return null;
      List<XmlNode> child = parent.child;
      if ( child.get(0) == this ) return parent;
      return child.get( child.indexOf( this ) - 1 );
   }

   XmlNode after() {
      if ( parent == null ) return null;
      List<XmlNode> child = parent.child;
      if ( child.get( child.size()-1 ) == this ) return parent;
      return child.get( child.indexOf( this ) + 1 );
   }
}