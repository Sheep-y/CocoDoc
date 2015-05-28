package sheepy.cocodoc;

public interface CocoObserver {
   /** Create a new child node and return its observer */
   public CocoObserver newNode ( String name );

   /** Set the status text of this progress */
   public CocoObserver setName ( String name );

   /** Log a message */
   public CocoObserver log ( String message );

   /** Log an error message */
   public CocoObserver error ( String message );

   /** Mark that the process has started. */
   public void start( long baseTime );

   /** Mark that the process ended. */
   public void done();
}