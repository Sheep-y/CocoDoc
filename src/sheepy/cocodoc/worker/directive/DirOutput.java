package sheepy.cocodoc.worker.directive;

import java.util.List;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.task.Task;

public class DirOutput extends Directive {

   public DirOutput() {
   }

   public DirOutput(Action action, List<Task> tasks) {
      super(action, tasks);
   }

   @Override public Directive start( Block context ) {
      setBlock( context );
      for ( Task task : getTasks() )
         task.process();
      return this;
   }

   @Override public Block get() throws InterruptedException {
      if ( getBlock() == null ) throw new IllegalStateException();
      return getBlock();
   }

}