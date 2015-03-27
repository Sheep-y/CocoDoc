package sheepy.cocodoc.worker.directive;

import java.util.List;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.Worker;
import sheepy.cocodoc.worker.parser.Parser;
import sheepy.cocodoc.worker.task.Task;

public class DirStart extends Directive {

   public DirStart() {
   }

   public DirStart(Action action, List<Task> tasks) {
      super(action, tasks);
   }

   @Override public Directive start( Block context ) {
      Block b = new Block( context, this );
      try ( Parser parser = context.getParser().clone() ) {
         b.setText( parser.parse( b ) );
         Worker.startBlock( b );
         return this;
      }
   }

   @Override public Block get() throws InterruptedException {
      return getBlock();
   }

}