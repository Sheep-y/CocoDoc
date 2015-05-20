package sheepy.cocodoc.worker.task;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.worker.Block;
import static sheepy.util.collection.CollectionPredicate.onlyContains;
import sheepy.util.text.Escape;
import sheepy.util.text.Text;

public class TaskEncode extends Task {

   @Override public Action getAction () { return Action.ENCODE; }

   private static final String[] validParams = new String[]{ "base64","crlf","lf","js","url","html","xhtml","xml" };
   private static final Predicate<List<String>> validate = nonEmpty.and( onlyContains( Arrays.asList( validParams ) ) );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "encode() task should have one or more of " + String.join( ",", validParams ) + ". Actual: {0}"; }

   @Override protected void run () {
      if ( ! hasParams() ) return;
      log( Level.FINER, "Encoding data" );

      Block block = getBlock();
      for ( String e : getParams() ) {
         log( Level.FINEST, "Encode to {0}", e.toLowerCase() );
         switch ( e.toLowerCase() ) {
            case "base64" :
               byte[] data = block.getBinary();
               block.setText( Base64.getEncoder().encodeToString( data ) );
               break;

            case "crlf" :
               block.setText( Text.toCrLf( block.getText() ) );
               break;

            case "lf" :
               block.setText( Text.toLf( block.getText() ) );
               break;

            case "js" :
               block.setText(Escape.javascript( block.getText() ) );
               break;

            case "url":
               block.setText(Escape.url( block.getText() ) );
               break;

            case "html" :
            case "xhtml" :
            case "xml" :
               block.setText(Escape.xml( block.getText() ) );
               break;

            default :
               throwOrWarn( new CocoParseError( "Unknown encode parameter: " + e ) );
         }
      }
      log( Level.FINEST, "Data Encoded." );
   }
}