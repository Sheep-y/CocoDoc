package sheepy.cocodoc.worker.directive;

import java.util.List;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.task.Task;

public class DirOutput extends Directive {

   public DirOutput ( Action action, List<Task> tasks ) {
      super(action, tasks);
      for ( Task t : tasks ) {
         switch ( t.getAction() ) {
            case FILE:
            case OPEN:
            case DELETE:
               break;
            default:
               throw new CocoParseError( "Unsupported task in Output action: " + t );
         }
      }
   }

   @Override public Directive start ( Block parent ) {
      setBlock( parent );
      if ( getObserver() == null )
         setObserver( parent.getObserver() );
      // No point in starting a new Thread if we only have FILE and DELETE.
      for ( Task task : getTasks() )
         task.process();
      return this;
   }

   @Override public Block get () throws InterruptedException {
      if ( getBlock() == null ) throw new IllegalStateException();
      return getBlock();
   }

}