package sheepy.cocodoc.worker;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.CocoRunError;

/**
 * Creates and run block
 */
public class Worker {
   static final Logger log = Logger.getLogger( Worker.class.getName() );
   static final ExecutorService thread_pool = Executors.newCachedThreadPool( Worker::newThread );
   static {
      log.setLevel( Level.ALL );
   }

   /**
    * Run a block.
    * @param block
    */
   public static void run( Block block ) {
      run( block, block );
   }

   /**
    * Run a job for a block.
    * @param block
    */
   public static void run( Runnable job, Block context ) {
      thread_pool.execute( job );
   }

   private static final AtomicInteger thread_count = new AtomicInteger(0);
   private static Thread newThread ( Runnable r ) {
      Thread result = new Thread( r, "Worker #" + thread_count.getAndIncrement() );
      result.setPriority( 3 ); // Between min and normal
      result.setDaemon( true );
      return result;
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

}