package sheepy.util.ui;

import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;

public class JavaFX {
   /** Run a job on FX thread and wait until it finishes */
   public static void runNow( Runnable run ) {
      if ( Platform.isFxApplicationThread() ) {
         run.run();
      } else {
         CountDownLatch latch = new CountDownLatch(1);
         Platform.runLater( () -> {
            try {
               run.run();
            } finally {
               latch.countDown();
            }
         } );
         try {
            latch.await();
         } catch ( InterruptedException ignored ) {}
      }
   }
}
