package sheepy.util;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import sheepy.util.concurrent.AbstractFuture;

public class Time {

   public static void sleep ( long millsec ) {
      try {
         Thread.sleep( millsec );
      } catch ( InterruptedException ex ) {
         Thread.currentThread().interrupt();
      }
   }

   public static Future defer ( long interval, Runnable task ) {
      // Borrow countDown so that the defer can be cancelled
      return countDown( 1, interval, ( e ) -> {
         if ( e <= 0 ) task.run();
      } );
   }

   /** Relatively inaccurate countdown, suitable for short and quick use */
   public static Future countDown ( long init, long interval, Consumer< Long > callback ) {
      return new Counter( callback, interval, init );
   }

   private static class Counter extends AbstractFuture<Object> {

      private final Thread thread = new Thread( this, "Count down" ); // TODO: Can switch to a single Timer thread, scheduling countdown as needed?
      private final long init;
      private final Consumer<Long> callback;
      private final long interval;

      private Counter ( Consumer<Long> callback, long interval, long init ) {
         this.callback = callback;
         this.init = init;
         this.interval = interval;
         thread.setDaemon( true );
         thread.setPriority( Thread.NORM_PRIORITY+1 );
         thread.start();
      }

      @Override protected Object implRun() {
         long count = init;
         do {
            if ( Thread.currentThread().isInterrupted() ) return null;
            try { callback.accept( count ); } catch ( Exception ex ) { ex.printStackTrace(); }
            if ( Thread.currentThread().isInterrupted() ) return null;
            if ( --count < 0 ) break;
            Time.sleep( interval );
         } while ( count >= 0 );
         return null;
      }
   }

}