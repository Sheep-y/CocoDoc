package sheepy.cocodoc.worker.directive;

import java.util.List;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.task.Task;

public class DirEnd extends Directive {

   public DirEnd() {
      this( Action.END, null );
   }

   public DirEnd(Action action, List<Task> tasks) {
      super(action, tasks);
   }

   @Override public Directive start( Block context ) {
      throw new UnsupportedOperationException();
   }

   @Override public Block get() throws InterruptedException {
      throw new UnsupportedOperationException();
   }

}