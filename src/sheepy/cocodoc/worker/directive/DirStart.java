package sheepy.cocodoc.worker.directive;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.Worker;
import static sheepy.cocodoc.worker.directive.Directive.log;
import sheepy.cocodoc.worker.parser.Parser;
import sheepy.cocodoc.worker.task.Task;
import sheepy.util.Text;

public class DirStart extends Directive {

   private final CountDownLatch countdown = new CountDownLatch( 1 );

   public DirStart ( Action action, List<Task> tasks ) {
      super( action, tasks );
   }

   @Override public Directive start( Block parent ) {
      if ( countdown.getCount() <= 0 ) throw new IllegalStateException( "Start directive should not be started more than once." );
      log.log( Level.FINEST, "Start start directive {0}", this );
      if ( branchMonitor( parent, toString() ) != null )
         getMonitor().start(); // Notice start before parsing (and before block start)

      Block b = new Block( parent, this );
      final Parser parser = parent.getParser().clone(); // Must have a parser, because Start is created by a parser!
      parser.start( b );  // By this time all subblock parsing has finished.
      Worker.run( () -> { // And this part is about running the start directive's tasks.
         try {
            b.setText( parser.get() );
            log.log( Level.FINEST, "Copied {0} characters to start block: {1}", new Object[]{ b.getText().length(), Text.ellipsis( b.getText(), 12 ) } );
            b.run();
         } finally {
            countdown.countDown();
         }
      }, b );

      return this;
   }

   @Override public Block get () throws InterruptedException {
      if ( countdown.getCount() > 0 ) log.log( Level.FINEST, "Waiting for {0}", this );
      countdown.await();
      log.log( Level.FINEST, "End start directive {0}", this );
      return getBlock();
   }

}