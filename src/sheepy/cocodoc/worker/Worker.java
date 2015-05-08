package sheepy.cocodoc.worker;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.CocoRunError;

/**
 * Runs block in a thread pool
 */
public class Worker {
   static final Logger log = Logger.getLogger( Worker.class.getName() );
   // Do not use fixed thread pool, because blocks will wait for sub blocks.  Limited thread = deadlock.
   static final ExecutorService thread_pool = Executors.newCachedThreadPool( Worker::newThread );
   static {
      log.setLevel( Level.ALL );
   }

   public static void run( Block block ) {
      run( block, block );
   }

   public static void stop () {
      thread_pool.shutdownNow();
   }

   public static void run( Runnable job, Block context ) {
      try {
         thread_pool.execute( job );
      } catch ( RejectedExecutionException ex ) {
         throw new CocoRunError( ex );
      }
   }

   private static final AtomicInteger thread_count = new AtomicInteger(0);
   private static Thread newThread ( Runnable r ) {
      Thread result = new Thread( r, "Worker #" + thread_count.getAndIncrement() );
      log.log( Level.FINER, "Created worker thread #" + thread_count.get() );
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