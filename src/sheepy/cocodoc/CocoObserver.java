package sheepy.cocodoc;

import java.io.File;

public interface CocoObserver {
   /** Create a new child node and return its observer */
   public CocoObserver newNode ( String name );

   /** Set the status text of this progress */
   public CocoObserver setName ( String name );

   /** Monitor change of a file */
   public CocoObserver monitor ( File f );

   /** Log a message */
   public CocoObserver log ( String message );

   /** Log an error message */
   public CocoObserver error ( String message );

   /** mark that the process has started. */
   public default void start ( long basetime ) { start( Thread.currentThread(), basetime ); }
   public void start ( Thread currentThread, long basetime );

   /** Mark that the process ended. */
   public default void done () { done( Thread.currentThread() ); }
   public void done ( Thread currentThread );
}