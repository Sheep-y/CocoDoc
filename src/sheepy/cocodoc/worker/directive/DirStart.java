package sheepy.cocodoc.worker.directive;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import sheepy.cocodoc.CocoObserver;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.BlockStats;
import sheepy.cocodoc.worker.Worker;
import sheepy.cocodoc.worker.parser.Parser;
import sheepy.cocodoc.worker.task.Task;
import sheepy.util.text.Text;

public class DirStart extends Directive {

   public DirStart ( Action action, List<Task> tasks ) {
      super( action, tasks );
   }

   @Override public Directive start( Block parent ) {
      if ( parent != null ) setObserver( parent.getObserver() ); // Same thread as parent
      log( Level.FINEST, "Started parsing", this );

      Block block = new Block( parent, this );
      block.setBasePath( parent.getBasePath() ); // Make sure base path is always same as parent.
      final Parser parser = parent.getParser().clone(); // Must have a parser, because Start is created by a parser!
      parser.start( block );
      log( Level.FINEST, "Block parsed", this );
      // By this time all subblock parsing has finished, and parent can continue.

      Worker.run( () -> { // And this part is about running the start directive's tasks.
         if ( branchObserver( parent, toString() ) != null )
            getObserver().start( (Long) parent.stats().getVar( BlockStats.NANO_BUILD ) );
         try {
            block.setText( parser.get() );
            log( Level.FINEST, "Copied {1} ({0} chars) to start block", this, block.getText().length(), Text.ellipsis( block.getText(), 10 ) );
            block.run();
         } catch ( Exception ex ) {
            if ( ! block.isDone() ) block.stop( ex );
         } finally {
            if ( getObserver() != null ) getObserver().done();
         }
      } );
      setBlock( block );

      return this;
   }

   @Override public Block get () throws InterruptedException {
      try {
         return getBlock().get();
      } catch ( ExecutionException ex ) {
         if ( ex.getCause() != null && ex.getCause() instanceof CocoRunError )
            throw (CocoRunError) ex.getCause();
         throw new CocoRunError( ex );
      }
   }
}