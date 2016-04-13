package sheepy.cocodoc.worker.parser.coco;

import java.util.List;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.worker.parser.coco.XmlNode.NODE_TYPE;
import sheepy.cocodoc.worker.task.Task;
import sheepy.util.collection.NullData;
import sheepy.util.text.Text;

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
         Throwable cause = ex.getCause();
         throw new CocoRunError( "Cannot find " + toString(), cause == null ? ex : cause );
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

   /**
    * Find element using relative location.
    * Used by ParserCoco's delete task and position task.
    *
    * @param text Text to search.
    * @param range Starting range.  May be find within or find outward, depending on the selector.
    * @return Found range, or null if not found.
    */
   abstract TextRange find ( CharSequence text, TextRange range );
   boolean match ( XmlNode node ) {
      throw new UnsupportedOperationException();
   };

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
      @Override boolean match ( XmlNode node ) {
         throw new UnsupportedOperationException();
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
                  if ( pos2 >= 0 ) ++pos2; else pos2 = 0;
                  return new TextRange( pos2, pos+1 ).setContext( range.context.root() );
               }
            }
         } else {
            int pos = range.end, strlen = str.length();
            while ( howMany > 0 && pos < strlen ) {
               pos = str.indexOf( '\n', pos );
               if ( pos >= 0 && pos < str.length() &&  --howMany <= 0 ) {
                  int pos2 = str.indexOf( '\n', pos+1 );
                  if ( pos2 < 0 ) pos2 = strlen;
                  return new TextRange( pos, pos2 ).setContext( range.context.root() );
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
      private final String tag;
      private final List<PosElementAttr> attr; // Never be null.

      PosElement ( String count, String tag, List<PosElementAttr> attr, String position ) {
         super( count, position );
         this.tag = tag;
         this.attr = NullData.copy( attr );
      }

      @Override TextRange find ( CharSequence text, TextRange range ) {
         int howMany = count == null ? 1 : Integer.parseInt( count );
         if ( this.position.equals( "before" ) ) {
            XmlNode pos = firstTag( range.context.root().findChildrenBefore( range.start ) );
            if ( pos.range.end > range.end ) pos = firstTag( pos.before() );
            while ( pos != null && howMany > 0 ) {
               if ( ! this.match( pos ) ) break;
               if ( --howMany <= 0 ) return pos.range;
               pos = firstTag( pos.before() );
            }
         } else {
            throw new UnsupportedOperationException( "After has not been implemented" );
         }
         throw new CocoRunError( "Cannot find " + this );
      }

      @Override boolean match( XmlNode node ) {
         if ( ! isTag( node ) || ( ! tag.equals( "*" ) && ! node.getValue().toString().equals( tag ) ) )
            return false;
         for ( PosElementAttr a : attr ) {
            XmlNode name = node.getAttribute( a.attr );
            if ( name == null ) return false;
            String target = a.value;
            if ( target != null ) {
               String val = name.getAttributeValue().toString();
               if ( a.caseInsensitive ) {
                  val = val.toLowerCase();
                  target = target.toLowerCase();
               }
               switch ( a.matcher ) {
                  case  "=" : if ( ! val.equals    ( target ) ) return false;
                  case "^=" : if ( ! val.startsWith( target ) ) return false;
                  case "$=" : if ( ! val.endsWith  ( target ) ) return false;
               }
            }
         }
         return true;
      }

      @Override public String toString() {
         return ( nextToString() + " " + Text.ifNull( count, "the" ) + " " + tag + Text.toString( "[", "][", "]", attr ) + " " + position ).trim();
      }

      static class PosElementAttr {
         private final String attr;
         private final String matcher;
         private final String value;
         private final boolean caseInsensitive;

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
      private final String attr;
      PosAttr ( String attr ) { this.attr = attr; }

      @Override TextRange find ( CharSequence text, TextRange range ) {
         XmlNode base = firstTag( range.context.root().findChildrenOver( range.start ) );
         for ( XmlNode child : base.children() )
            if ( match( child ) )
               return child.range;
         throw new CocoRunError( "Cannot find " + this );
      }
      @Override boolean match ( XmlNode node ) {
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