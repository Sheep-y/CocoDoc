package sheepy.cocodoc.worker.task;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.worker.error.CocoParseError;
import static sheepy.cocodoc.worker.task.Task.log;
import static sheepy.util.collection.CollectionPredicate.andAlso;
import static sheepy.util.collection.CollectionPredicate.hasItem;
import static sheepy.util.collection.CollectionPredicate.onlyContains;

public class TaskEncode extends Task {

   @Override public Action getAction () { return Action.ENCODE; }

   private static final String[] validParams = new String[]{ "base64","crlf","lf","js","url","html","xml" };
   private static final Predicate<List<String>> validate = andAlso( hasItem(), onlyContains( Arrays.asList( validParams ) ) );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "encode() task should have one or more of " + String.join( ",", validParams ) + ": {0}"; }

   @Override public void run () {
      if ( ! hasParams() ) return;
      for ( String e : getParams() ) {
         switch ( e.toLowerCase() ) {
            case "base64" :
               byte[] data = getBlock().getBinary();
               getBlock().setText( Base64.getEncoder().encodeToString( data ) );
               log.log( Level.FINE, "Converted {0} bytes to Base64", data.length );
               break;

            default :
               throw new CocoParseError( "Unknown encode parameter: " + e );
         }
      }
   }
}