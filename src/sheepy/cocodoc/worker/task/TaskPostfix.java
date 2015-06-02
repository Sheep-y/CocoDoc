package sheepy.cocodoc.worker.task;

import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;

public class TaskPostfix extends Task {

   @Override public Action getAction () { return Action.POSTFIX; }

   @Override protected Predicate<List<String>> validParam() { return nonEmpty; }
   @Override protected String invalidParamMessage() { return "postfix() task should have parameter."; }

   @Override protected void run () {
      if ( ! hasParams() ) {
         log( Level.INFO, "Skipping postfix(), no parameter" );
         return;
      }
      log( Level.FINER, "Adding postfix" );

      StringBuilder affix = new StringBuilder();
      for ( String s : getParams() ) affix.append( s );
      log( Level.FINE, "Adding {0} characters as postfix", affix.length() );
      if ( affix.length() <= 0 ) return;

      getBlock().setText( getBlock().getText().append( affix ) );
      log( Level.FINEST, "Added {0} characters as postfix", affix.length() );
   }
}