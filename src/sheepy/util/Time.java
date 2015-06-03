package sheepy.util;

public class Time {
   public static void sleep ( long millsec ) {
      try {
         Thread.sleep( millsec );
      } catch ( InterruptedException ignored ) {}
   }
}
