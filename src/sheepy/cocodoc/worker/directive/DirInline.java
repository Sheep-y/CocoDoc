package sheepy.cocodoc.worker.directive;

import java.util.List;
import java.util.logging.Level;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.Worker;
import sheepy.cocodoc.worker.task.Task;

public class DirInline extends Directive {

   public DirInline() {
      this( Action.INLINE, null );
   }

   public DirInline(Action action, List<Task> tasks) {
      super(action, tasks);
   }

   @Override public Directive start( Block parent ) {
      log.log( Level.FINEST, "Start inline directive {0}", this );
      branchMonitor( parent, "Inline" );
      Worker.run( new Block( parent, this ) );
      return this;
   }

   @Override public Block get() throws InterruptedException {
      try {
         return Worker.getBlockResult( getBlock() );
      } finally {
         log.log( Level.FINEST, "End inline directive {0}", this );
         done();
      }
   }

}