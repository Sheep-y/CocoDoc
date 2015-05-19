package sheepy.cocodoc.worker.task;

import java.awt.Desktop;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.worker.BlockStats;

/**
 * Call OS to open specified file or last file in same directive.
 */
public class TaskOpen extends Task {

   private static final String VAR_OPEN_LIST = "__open.list__";

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
         open( params.toArray( new String[ params.size() ] ) );
      }
   }

   /** Add file to open queue.  Start opener thread if this is the first file. */
   private void open ( String ... files ) {
      final BlockStats stats = getBlock().stats();
      List<File> list = (List<File>) stats.getVar( VAR_OPEN_LIST );
      if ( list == null ) try ( Closeable lock = stats.lockVar() ) {
         if ( ! stats.hasVar( VAR_OPEN_LIST ) ) {
            stats.setVar( VAR_OPEN_LIST, list = new Vector<>() );
            Thread opener = new Thread( this::open );
            opener.setPriority( Thread.MIN_PRIORITY );
            opener.setDaemon( true );
            opener.start();
         }
      } catch ( IOException ignored ) {}
      File base = getBlock().getBasePath();
      for ( String f : files )
         list.add( new File( base, f ) );
   }

   /** Body of daemon opener thread.  Wait until main block done than call open */
   private void open () {
      log( Level.FINE, "Opener thread waiting" );
      while ( getBlock().getRoot().isRunning() ) try { // Should not exit as long as run() has not returned.
         Thread.sleep( 100 );
      } catch ( InterruptedException ex ) {
         return;
      }
      List<File> list = (List<File>) getBlock().stats().getVar( VAR_OPEN_LIST );
      log( Level.FINE, "Opens {0} files", list.size() );
      for ( File f : list ) try {
         log( Level.FINEST, "Opening {0}", f );
         Desktop.getDesktop().open( f );
         log( Level.FINEST, "Opened {0}", f );
      } catch (IOException ex) {
         log( Level.WARNING, "Cannot open {0}: {1}", f, ex );
      }
   }
}