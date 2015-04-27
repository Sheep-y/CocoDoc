package sheepy.cocodoc.worker.task;

import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.CocoRunError;
import static sheepy.cocodoc.worker.task.Task.log;

public class TaskCData extends Task {

   @Override public Action getAction () { return Action.CDATA; }

   @Override protected Predicate<List<String>> validParam() { return isEmpty; }
   @Override protected String invalidParamMessage() { return "cdata() task should have no parameters."; }

   @Override protected void run () {
      StringBuilder text = getBlock().getText();
      if ( text.length() <= 0 ) return;
      if ( text.indexOf( "<![CDATA[" ) >= 0 || text.indexOf( "]]>" ) >= 0 )
         throwOrWarn( new CocoRunError( "cdata() task cannot wrap content containing cdata" ) );
      log.log( Level.FINE, "Adding CDATA for {0} characters.", text.length() );
      text.insert( 0, "<![CDATA[" ).append( "]]>" );
      getBlock().setText( text );
   }
}