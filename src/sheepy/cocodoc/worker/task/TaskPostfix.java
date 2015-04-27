package sheepy.cocodoc.worker.task;

import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import static sheepy.cocodoc.worker.task.Task.log;

public class TaskPostfix extends Task {

   @Override public Action getAction () { return Action.POSTFIX; }

   @Override protected Predicate<List<String>> validParam() { return nonEmpty; }
   @Override protected String invalidParamMessage() { return "postfix() task should have parameter."; }

   @Override protected void run () {
      if ( ! hasParams() ) return;
      StringBuilder affix = new StringBuilder();
      for ( String s : getParams() ) affix.append( s );
      log.log( Level.FINE, "Adding {0} characters as postfix", affix.length() );
      if ( affix.length() <= 0 ) return;

      getBlock().setText( getBlock().getText().append( affix ) );
   }
}