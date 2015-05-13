package sheepy.cocodoc.worker.parser.coco;

import java.util.List;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.worker.parser.coco.XmlNode.NODE_TYPE;
import sheepy.cocodoc.worker.task.Task;
import sheepy.util.Text;
import sheepy.util.collection.NullData;

/**
 * Represents a selector applicable to xml documents.
 */
abstract class XmlSelector {
   protected XmlSelector next;

   XmlSelector() { }

   XmlSelector setNext ( XmlSelector next ) {
      this.next = next;
      return this;
   }

   TextRange locate ( CharSequence text, TextRange range ) {
      try {
         TextRange result = find( text, range );
         if ( next != null ) {
            if ( result == null ) throw new CocoRunError( "Cannot find " + toString() );
            return next.locate( text, result );
         }
         return result;
      } catch ( CocoRunError ex ) {
         throw new CocoRunError( "Cannot find " + toString(), ex.getCause() == null ? ex : ex.getCause() );
      }
   }

   protected XmlNode firstTag( XmlNode subject ) {
      while ( ! isTag( subject ) )
         subject = subject.before();
      return subject;
   }

   protected boolean isTag( XmlNode node ) {
      return node == null || ( node.isValid() && node.getType() == NODE_TYPE.TAG );
   }

   abstract TextRange find ( CharSequence text, TextRange range );

   protected String nextToString() {
      return next == null ? "" : next.toString();
   }

   /**********************************************************************************************************/
   // Sub classes

   static class PosThis extends XmlSelector {
      @Override TextRange find ( CharSequence text, TextRange range ) {
         return range;
      }
      @Override public String toString() {
         return ( nextToString() + " this" ).trim();
      }
   }

   static class PosLine extends XmlSelector {
      protected String count; // may be null. Otherwise is all/2/3/4/etc.
      protected String position; // before/after;

      PosLine ( String count, String position ) {
         if ( count != null ) {
            if ( count.equals( "the" ) ) count = null;
            else if ( ! count.equals( "all" ) ) {
               count = count.replaceAll( "\\D", "" );
               if ( count.equals( "1" ) ) count = null;
            }
            if ( count != null && count.isEmpty() ) count = null;
         }

         this.count = count;
         this.position = position;
      }

      @Override TextRange find ( CharSequence text, TextRange range ) {
         int howMany = count == null ? 1 : Integer.parseInt( count );
         String str = text.toString();
         if ( this.position.equals( "before" ) ) {
            int pos = range.start;
            while ( howMany > 0 && pos > 0 ) {
               pos = str.lastIndexOf( '\n', pos );
               if ( pos > 0 && --howMany <= 0 ) {
                  int pos2 = str.lastIndexOf( '\n', pos-1 );
                  if ( pos2 >= 0 ) return new TextRange( pos2+1, pos ).setContext( range.context.root() );
               }
            }
         } else {
            int pos = range.end;
            while ( howMany > 0 && pos < str.length() ) {
               pos = str.indexOf( '\n', pos );
               if ( pos >= 0 && pos < str.length() &&  --howMany <= 0 ) {
                  int pos2 = str.indexOf( '\n', pos+1 );
                  if ( pos2 >= 0 ) return new TextRange( pos, pos2 ).setContext( range.context.root() );
               }
            }
         }
         throw new CocoRunError( "Cannot find " + this );
      }

      @Override public String toString() {
         return ( nextToString() + " " + Text.ifNull( count, "the" ) + " line " + position ).trim();
      }
   }

   static class PosElement extends PosLine {
      private String tag;
      private List<PosElementAttr> attr; // Never be null.

      PosElement ( String count, String tag, List<PosElementAttr> attr, String position ) {
         super( count, position );
         this.tag = tag;
         this.attr = NullData.copy( attr );
      }

      @Override TextRange find ( CharSequence text, TextRange range ) {
         int howMany = count == null ? 1 : Integer.parseInt( count );
         if ( this.position.equals( "before" ) ) {
            XmlNode pos = firstTag( range.context.root().findChildrenBefore( range.start ) );
            do {
               pos = firstTag( pos.before() );
               if ( ! this.match( pos ) ) break;
               if ( --howMany <= 0 ) return pos.range;
            } while ( pos != null && howMany > 0 );
         } else {
            throw new UnsupportedOperationException( "Not implemented" );
         }
         throw new CocoRunError( "Cannot find " + this );
      }

      private boolean match( XmlNode node ) {
         return node != null && isTag( node ) && node.getValue().toString().equals( tag );
      }
      @Override public String toString() {
         return ( nextToString() + " " + Text.ifNull( count, "the" ) + " " + tag + Text.toString( "[", "][", "]", attr ) + " " + position ).trim();
      }


      static class PosElementAttr {
         private String attr;
         private String matcher;
         private String value;
         private boolean caseInsensitive;

         PosElementAttr(String attr, String matcher, String value, boolean caseInsensitive) {
            this.attr = attr;
            this.matcher = matcher;
            this.value = value;
            this.caseInsensitive = caseInsensitive;
         }

         @Override public String toString() {
            if ( matcher == null ) return attr;
            return attr + matcher + Task.quote( value ) + ( caseInsensitive ? " i" : "" );
         }
      }
   }

   static class PosAttr extends XmlSelector {
      String attr;
      PosAttr(String attr) { this.attr = attr; }

      @Override TextRange find ( CharSequence text, TextRange range ) {
         XmlNode base = firstTag( range.context.root().findChildrenBefore( range.start ) );
         for ( XmlNode child : base.children() )
            if ( match( child ) )
               return child.range;
         throw new CocoRunError( "Cannot find " + this );
      }
      private boolean match( XmlNode node ) {
         return node.isValid() && node.getType() == NODE_TYPE.ATTRIBUTE && node.value.toString().equals( attr );
      }
      @Override public String toString() {
         return attr + " of";
      }
   }

   static class PosBefore extends XmlSelector {
      @Override TextRange find ( CharSequence text, TextRange range ) {
         return new TextRange( range.start ).setContext( range.context.root() );
      }
   }

   static class PosAfter extends XmlSelector {
      @Override TextRange find ( CharSequence text, TextRange range ) {
         return new TextRange( range.end ).setContext( range.context.root() );
      }
   }
}