package sheepy.cocodoc.worker.task;

import java.util.List;
import java.util.function.Predicate;
import sheepy.cocodoc.CocoParseError;

public class TaskPosition extends Task {

   @Override public Action getAction () { return Action.POSITION; }

   @Override protected Predicate<List<String>> validParam() { return nonEmpty; }
   @Override protected String invalidParamMessage() { return "position() task should not be empty."; }

   @Override public void init() {
      super.init();
      switch ( getDirective().getAction() ) {
         case INLINE :
         case START  :
            break;
         default     :
            throwOrWarn( new CocoParseError( "position() task only supports <?coco-inline?> and <?coco-start?>. Current: coco-" + getDirective().getAction().name().toLowerCase() ) );
      }
   }

   @Override protected void run() {
      // Position task is handled by ParserCoco
   }
}