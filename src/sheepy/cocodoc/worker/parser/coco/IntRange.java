package sheepy.cocodoc.worker.parser.coco;

import sheepy.util.Text;

/**
 * Represents a range in integer
 */
class IntRange {
   int start;
   int end;

   IntRange(int start, int end) {
      assert (start <= end);
      this.start = start;
      this.end = end;
   }

   IntRange(int start) {
      this(start, start);
   }

   int length() {
      return end - start;
   }

   // 0 x 1 x 2 x 3 x 4
   //     1 range 3      test cases. Please make them a test...
   //     1       2      add before: [3,5]
   //         2   2      add within: [1,5]
   // 0  -1              delete before: [0,2]
   //     1      -2      full delete: [-1,-1]
   // 0              -4  full delete: [-1,-1]
   // 0      -2          partial delete: [0,1]
   //         2      -2  partial delete: [1,2]

   void shiftInserted ( int atPosition, int deviation ) {
      if (deviation == 0 || atPosition >= end || start < 0) {
         return; // No change, insert or delete after us, or we were deleted.
      }
      if (atPosition <= start) {
         start += deviation; // Inserting before us (otherwish inserting withing us)
      }
      end += deviation;
   }

   void shiftDeleted ( IntRange range, boolean stayWhenDeleted ) {
      if ( ! range.isValid() || ! isValid() || range.start >= end )  return; // No change, insert or delete after us, or we were deleted.
      int atPosition = range.start;
      int affect_end = range.end;
      int deviation = range.length();
      if ( atPosition <= start) {
         if (affect_end <= start) {
            // Delete before us, simple adjustment.
            start -= deviation;
            end -= deviation;
         } else if (affect_end >= end) {
            start = end = stayWhenDeleted ? range.end : -1; // We are fully deleted
         } else {
            // Head is deleted.
            start += -deviation + (start - atPosition);
            end -= deviation;
         }
      } else {
         // Tail is deleted.
         end += -deviation + (affect_end - end);
      }
   }

   String showInText ( CharSequence text ) {
      if ( ! isValid() ) return "(invalid range)";
      if ( start >= text.length() || end >= text.length() ) return "(out of text range)";
      return Text.ellipsisBefore( text.subSequence( 0, start ), 12 )
           + 'λ'
           + text.subSequence(start, end).toString().replaceAll( "[\r\n]", "" )
           + 'λ'
           + Text.ellipsisAfter( text.subSequence( end, text.length() ), 12 );
   }

   boolean isValid() {
      return start >= 0;
   }

   @Override
   public String toString() {
      return "TextRange [" + start + "," + end + ']';
   }
}