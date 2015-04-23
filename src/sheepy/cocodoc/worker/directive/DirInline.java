package sheepy.cocodoc.worker.directive;

import java.util.List;
import java.util.logging.Level;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.Worker;
import sheepy.cocodoc.worker.task.Task;

public class DirInline extends Directive {

   public DirInline() {
   }

   public DirInline(Action action, List<Task> tasks) {
      super(action, tasks);
   }

   @Override public Directive start( Block context ) {
      log.log( Level.FINEST, "Start inline directive {0}", this );
      Worker.startBlock( new Block( context, this ) );
      return this;
   }

   @Override public Block get() throws InterruptedException {
      try {
         return Worker.getBlockResult( getBlock() );
      } finally {
         log.log( Level.FINEST, "End inline directive {0}", this );
      }
   }

}