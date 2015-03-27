package sheepy.cocodoc.worker.parser.coco;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import sheepy.cocodoc.worker.error.CocoRunError;
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

   List<IntRange> locate ( CharSequence text, int position ) {
      return locate( text, new IntRange(position) );
   }

   List<IntRange> locate ( CharSequence text, IntRange position ) {
      return locate( new XmlSeeker( text ), Collections.singletonList(position) );
   }

   List<IntRange> locate ( XmlSeeker seeker, List<IntRange> position ) {
      if ( position == null || position.isEmpty() ) return Collections.emptyList();
      List<IntRange> result = new ArrayList<>( position.size() );
      for ( IntRange range : position ) {
         if ( range != null && ! range.isValid() ) continue;
         seeker.reset( range.start );
         List<IntRange> add = find( seeker, range );
         if ( add != null && ! add.isEmpty() )
            add.stream().filter( e -> e != null && e.isValid() ).forEach( result::add );
      }
      if ( next == null ) return result ;
      return next.locate( seeker, result );
   }

   abstract List<IntRange> find ( XmlSeeker text, IntRange range );

   protected String nextToString() {
      return next == null ? "" : next.toString();
   }

   /**********************************************************************************************************/
   // Sub classes

   static class PosThis extends XmlSelector {
      @Override public List<IntRange> find ( XmlSeeker text, IntRange position) {
         return Collections.singletonList( position );
      }
      @Override public String toString() {
         return ( nextToString() + " this" ).trim();
      }
   }

   static class PosElement extends XmlSelector {
      private String count; // may be null. Otherwise is all/2/3/4/etc.
      private String tag;
      private List<PosElementAttr> attr; // Never be null.
      private String position; // before/after;

      PosElement ( String count, String tag, List<PosElementAttr> attr, String position ) {
         if ( count != null ) {
            if ( count.equals( "the" ) ) count = null;
            else if ( ! count.equals( "all" ) ) {
               count = count.replaceAll( "\\D", "" );
               if ( count.equals( "1" ) ) count = null;
            }
            if ( count != null && count.isEmpty() ) count = null;
         }

         this.count = count;
         this.tag = tag;
         this.attr = NullData.copy( attr );
         this.position = position;
      }

      @Override List<IntRange> find ( XmlSeeker text, IntRange position ) {
         int howMany = count == null ? 1 : Integer.parseInt( count );
         if ( this.position.equals( "before" ) ) {
            if ( tag.equals( "line" ) ) { // before, line
               String str = text.text().toString();
               int end = text.start();
               while ( howMany > 0 ) {
                  end = str.lastIndexOf( '\n', end );
                  if ( end >= 0 && --howMany <= 0 ) {
                     return Collections.singletonList( new IntRange( str.lastIndexOf( '\n', end-1 )+1, end ) );
                  }
               }
            } else { // before, tag
               while ( howMany > 0 ) {
                  if ( ! text.findTagBefore( tag ) || text.end() < 0 ) break;
                  if ( --howMany <= 0 ) {
                     return Collections.singletonList( new IntRange( text.start(), text.end() ) );
                  }
               }
            }
         }
         throw new CocoRunError( "Cannot find " + this );
      }

      @Override public String toString() {
         return ( nextToString() + " " + Text.ifNull( count, "the" ) + " " + tag + Text.toString( "[", "][", "]", attr ) + " " + position ).trim();
      }
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

   static class PosAttr extends XmlSelector {
      String attr;
      PosAttr(String attr) { this.attr = attr; }

      @Override List<IntRange> find ( XmlSeeker text, IntRange position) {
         if ( text.findAttributeValue( attr ) )
            return Collections.singletonList( new IntRange( text.start(), text.end() ) );
         throw new CocoRunError( "Cannot find " + this );
      }
      @Override public String toString() {
         return attr + " of";
      }
   }

   static class PosBefore extends XmlSelector {
      @Override public List<IntRange> find ( XmlSeeker text, IntRange position) {
         return Collections.singletonList( new IntRange( position.start ) );
      }
   }

   static class PosAfter extends XmlSelector {
      @Override public List<IntRange> find ( XmlSeeker text, IntRange position) {
         return Collections.singletonList( new IntRange( position.end ) );
      }
   }
}
