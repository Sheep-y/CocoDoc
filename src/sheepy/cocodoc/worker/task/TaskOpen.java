package sheepy.cocodoc.worker.task;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.worker.BlockStats;
import sheepy.util.collection.NullData;

/**
 * Call OS to open specified file or last file in same directive.
 */
public class TaskOpen extends Task {

   @Override public Action getAction () { return Action.OPEN; }

   @Override protected Predicate<List<String>> validParam() { return null; }

   @Override protected void run () {
      if ( ! hasParams() ) {
         String file = null;
         for ( Task task : getDirective().getTasks() ) {
            if ( task instanceof TaskFile ) {
               List<String> params = task.getParams();
               file = params.get( params.size()-1 );
            }
         }
         if ( file == null && getBlock().getOutputTarget() != null )
            file = getBlock().getOutputTarget().getParam( 0 );
         if ( file == null ) {
            log( Level.WARNING, "Nothing to open" );
         } else {
            log( Level.FINER, "Added {0} to open queue", file );
            open( file );
         }
      } else {
         final List<String> params = getParams();
         log( Level.FINER, "Adding {0} files to open queue", params.size() );
         open( NullData.stringArray( params ) );
      }
   }

   /** Add file to open queue.  Start opener thread if this is the first file. */
   private void open ( String ... files ) {
      final BlockStats stats = getBlock().stats();
      getBlock().addOnDone( (b) -> {
         log( Level.FINE, "Opens {0} files", files.length );
         File base = getBlock().getBasePath();
         for ( String f : files ) try {
            log( Level.FINEST, "Opening {0}", f );
            Desktop.getDesktop().open( new File( base, f ) );
            log( Level.FINEST, "Opened {0}", f );
         } catch (IOException ex ) {
            log( Level.WARNING, "Cannot open {0}: {1}", f, ex );
         }
      });
   }
}