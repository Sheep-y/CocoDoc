package sheepy.cocodoc.worker.task;

import java.util.List;
import java.util.function.Predicate;
import sheepy.cocodoc.worker.Block;

public class TaskPostProcess extends Task {

   @Override public Action getAction () { return Action.POSTPROCESS; }

   @Override protected Predicate<List<String>> validParam() { return isEmpty; }
   @Override protected String invalidParamMessage() { return "postprocess() task should have no parameters."; }

   @Override protected void run () {
      Block block = getBlock();
      if ( block.hasData() ) {
         block.getText();
         block.postprocess();
      }
   }
}