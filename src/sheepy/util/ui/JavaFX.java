package sheepy.util.ui;

import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;

public class JavaFX {
   public static void runNow( Runnable run ) {
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
