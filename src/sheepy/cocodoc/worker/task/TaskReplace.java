package sheepy.cocodoc.worker.task;

import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.util.collection.CollectionPredicate;

/**
 * Replace a term for another.
 */
public class TaskReplace extends Task {

   @Override public Action getAction () { return Action.REPLACE; }

   private static final Predicate<List<String>> validate = CollectionPredicate.<List<String>>size( 2 );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "replace() task requires two parameters"; }

   @Override protected void run () {
      String find = getParam( 0 );
      String replace = getParam( 1 );
      if ( find == null || replace == null || ! getBlock().hasData() ) return;
      log( Level.FINER, "Replace \"{0}\" with \"{1}\"", find, replace );
      String from = getBlock().getText().toString();
      getBlock().setText( from.replaceAll( find, replace ) );
      log( Level.FINER, "Replaced: {0} chars -> {1} chars", from.length(), getBlock().getText().length() );
   }
}