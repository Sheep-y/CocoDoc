package sheepy.cocodoc.worker.directive;

import java.util.List;
import java.util.logging.Level;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.task.Task;

public class DirOutput extends Directive {

   public DirOutput() {
   }

   public DirOutput(Action action, List<Task> tasks) {
      super(action, tasks);
      for ( Task t : tasks ) {
         switch ( t.getAction() ) {
            case FILE:
            case DELETE:
               break;
            default:
               throw new CocoParseError( "Unsupported task in Output action: " + t );
         }
      }
   }

   @Override public Directive start( Block context ) {
      log.log( Level.FINEST, "Start output directive {0}", this );
      setBlock( context );
      // No point in starting a new Thread if we only have FILE and DELETE.
      for ( Task task : getTasks() )
         task.process();
      log.log( Level.FINEST, "End output directive {0}", this );
      return this;
   }

   @Override public Block get() throws InterruptedException {
      if ( getBlock() == null ) throw new IllegalStateException();
      return getBlock();
   }

}