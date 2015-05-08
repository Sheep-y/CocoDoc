package sheepy.cocodoc;

public interface CocoMonitor {
   /** Create a new child node and return its monitor */
   public CocoMonitor newNode ( String name );

   /** Set the status text of this progress */
   public CocoMonitor setName ( String name );

   /** Mark that the process has started. */
   public void start();

   /** Mark that the process ended. */
   public void done();
}
