package sheepy.cocodoc;

public interface CocoMonitor {
   /** Create a new child node and return its monitor */
   public CocoMonitor newNode( String name );

   /** Set the status text of this progress */
   public CocoMonitor setText( String name );

   /** Set the status of this progress */
   public CocoMonitor setDone( boolean done );
}
