package sheepy.cocodoc.worker.task;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.CocoRunError;
import sheepy.util.collection.NullData;
import sheepy.util.text.I18n;

/**
 * Call OS to open specified file or last file in same directive.
 */
public class TaskExec extends Task {

   @Override public Action getAction() { return Action.EXEC; }

   @Override protected Predicate<List<String>> validParam() { return nonEmpty; }
   @Override protected String invalidParamMessage() { return "exec() task should have one or more parameters: executable and parameters."; }

   @Override protected void run () {
      if ( ! hasParams() ) return;
      String[] params = NullData.stringArray( getParams() );
      String cmd = String.join( " ", params );
      log( Level.FINER, "Executing {0}", cmd );

      try {
         try ( InputStream in = Runtime.getRuntime().exec( params ).getInputStream() ) {
            // TODO: handle output as binary!
            String txt = new Scanner( in ).useDelimiter("\\A").next();
            log( Level.FINEST, "Read {0} characturs from {1}.", txt.length(), getParam( 0 ) );
            getBlock().stats().addInBytes( txt.getBytes( I18n.UTF8 ).length );
            getBlock().getText().append( txt );
         }
      } catch ( IOException ex ) {
         throwOrWarn( new CocoRunError( ex ) );
      }

      log( Level.FINEST, "Executed {0}", cmd );
   }


}