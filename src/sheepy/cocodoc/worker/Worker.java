package sheepy.cocodoc.worker;

import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import sheepy.cocodoc.worker.error.CocoParseError;
import sheepy.cocodoc.worker.error.CocoRunError;

/**
 * Get input and create block
 */
public class Worker {
   static final Logger log = Logger.getLogger( Worker.class.getName() );
   static final boolean isMulthThread = true;

   public static Block startBlock( Block block ) {
      return startBlock( block, isMulthThread );
   }

   public static Block startBlock( Block block, boolean multhThread ) {
      run( block, multhThread );
      return block;
   }

   public static void run( Runnable task ) {
      run( task, isMulthThread );
   }

   public static void run( Runnable task, boolean multhThread ) {
      if ( multhThread ) new Thread( task ).start();
      else               task.run();
   }

   public static Block getBlockResult( Block block ) throws InterruptedException {
      try {
         return block.get();
      } catch ( ExecutionException ex ) {
         Throwable e = ex;
         while ( e != null && e instanceof ExecutionException ) e = e.getCause();
         if ( e == null ) e = ex;
         if ( e instanceof CocoRunError   ) throw (CocoRunError  ) e;
         if ( e instanceof CocoParseError ) throw (CocoParseError) e;
         if ( e instanceof Error          ) throw (Error         ) e;
         throw new CocoRunError( e );
      }
   }

   /*
   public void processBlock ( Block block ) throws InterruptedException {
      new Thread( block ).start();

      if ( block.getOutputTarget() != null ) return; // Targeted block will handle its own output.

      if ( block.hasText() || block.hasBinary() ) {
         Block parent = block.getParent();
         if ( parent == null ) System.out.println( block.getText() );
      } else {
         log.warning( "Block does not output any data" );
      }
   }
      */

}