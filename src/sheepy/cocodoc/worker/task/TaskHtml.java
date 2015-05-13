package sheepy.cocodoc.worker.task;

import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.worker.parser.ParserHtml;

public class TaskHtml extends Task {
   private static final boolean logDetails = true;

   @Override public Action getAction () { return Action.HTML; }

   @Override protected Predicate<List<String>> validParam() { return isEmpty; }
   @Override protected String invalidParamMessage() { return "html() task should have no parameters."; }

   @Override protected void run () {
      log( Level.FINER, "Parsing HTML tags" );
      ParserHtml p = new ParserHtml();
      p.start( getBlock() );
      p.get();
   }
}