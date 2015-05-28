package sheepy.cocodoc;

import java.util.logging.Level;

/**
 * Application options
 */
public class CocoOption {

   /** Auto close seconds.  -1 = do not close. */
   public static int auto_close_second = 5;

   /** Collapse to which level. 2 = shows two level, 0 = collapse all, Integer.MAX = do not collapse */
   public static int auto_collapse_level = 2;

   /** Auto open created files? */
   public static boolean auto_open = false;

   /** GUI log level */
   public static int log_level_gui = Level.FINEST.intValue();

   /** Whether to log parser actions */
   public static boolean log_parser = false;

}