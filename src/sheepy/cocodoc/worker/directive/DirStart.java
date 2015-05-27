package sheepy.cocodoc.worker.directive;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.Worker;
import sheepy.cocodoc.worker.parser.Parser;
import sheepy.cocodoc.worker.task.Task;
import sheepy.util.text.Text;

public class DirStart extends Directive {

   private final CountDownLatch countdown = new CountDownLatch( 1 );

   public DirStart ( Action action, List<Task> tasks ) {
      super( action, tasks );
   }

   @Override public Directive start( Block parent ) {
      if ( countdown.getCount() <= 0 ) throw new IllegalStateException( "Start directive should not be started more than once." );
      if ( branchObserver( parent, toString() ) != null )
         getObserver().start(); // Notice start before parsing (and before block start)
      log( Level.FINEST, "Started parsing", this );

      Block b = new Block( parent, this );
      b.setBasePath( parent.getBasePath() ); // Make sure base path is always same as parent.
      final Parser parser = parent.getParser().clone(); // Must have a parser, because Start is created by a parser!
      parser.start( b );
      log( Level.FINEST, "Block parsed", this );
      // By this time all subblock parsing has finished, and parent can continue.

      Worker.run( () -> { // And this part is about running the start directive's tasks.
         try {
            b.setText( parser.get() );
            log( Level.FINEST, "Copied {1} ({0} chars) to start block", this, b.getText().length(), Text.ellipsis( b.getText(), 10 ) );
            b.run();
         } finally {
            countdown.countDown();
         }
      } );

      return this;
   }

   @Override public Block get () throws InterruptedException {
      if ( countdown.getCount() > 0 ) log( Level.FINEST, "Waiting execution to finish", this );
      countdown.await();
      log( Level.FINEST, "Finished", this );
      return getBlock();
   }

}