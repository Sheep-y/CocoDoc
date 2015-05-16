package sheepy.cocodoc.worker.directive;

import java.util.List;
import java.util.logging.Level;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.Worker;
import sheepy.cocodoc.worker.task.Task;

public class DirInline extends Directive {

   public DirInline ( Action action, List<Task> tasks ) {
      super(action, tasks);
   }

   @Override public Directive start ( Block parent ) {
      branchObserver( parent, toString() );
      log( Level.FINEST, "Created inline job.", this );
      Worker.run( new Block( parent, this ) );
      return this;
   }

   @Override public Block get () throws InterruptedException {
      return Worker.getBlockResult( getBlock() );
   }

}