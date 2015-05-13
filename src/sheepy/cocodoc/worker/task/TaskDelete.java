package sheepy.cocodoc.worker.task;

import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Pattern;
import static sheepy.util.collection.CollectionPredicate.notContains;

public class TaskDelete extends Task {

   @Override public Action getAction () { return Action.DELETE; }

   private static final Predicate<List<String>> validate = nonEmpty.and( notContains( Pattern.compile( "^\\s*(before|after|replace)\\b" ) ) );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "delete() task should not be empty and must not start with before, after, or replace."; }

   @Override protected void run() {
      // Delete task is handled by ParserCoco
      log( Level.FINER, "Deferred to upper level parser" );
   }
}