package sheepy.cocodoc.ui;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.BorderPane;
import sheepy.cocodoc.CocoObserver;

public class ProgressPanel {

   private final TabPane tabPane = new TabPane();

   private Tab tabWelcome;

   public ProgressPanel() {
      tabWelcome = new Tab( "Welcome", new Label( "Click the New Build button below to launch a new job." ) );
      tabWelcome.setClosable( false );
      tabPane.getTabs().add( tabWelcome );
   }

   Node getPanel() {
      return tabPane;
   }

   /*******************************************************************************************************************/

   public CocoObserver newNode( String name ) {
      ProgressTab result = new ProgressTab( name );
      Platform.runLater( () -> {
         if ( tabWelcome != null ) {
            tabPane.getTabs().remove( tabWelcome );
            tabWelcome = null;
         }
         tabPane.getTabs().add( result.tab );
      });
      return result;
   }

   /*******************************************************************************************************************/

   /**
    * Main job tab
    */
   private class ProgressTab extends ObserverTreeItem {
      final Tab tab = new Tab( "New Job" );
      private final ProgressBar progress = new ProgressBar( ProgressBar.INDETERMINATE_PROGRESS );
      private final AtomicInteger maxProgress = new AtomicInteger();
      private final AtomicInteger curProgress = new AtomicInteger();

      public ProgressTab( String name ) {
         super( name );
         TreeTableView<ObserverTreeItem> tree = new TreeTableView<>( node );
         TreeTableColumn<ObserverTreeItem, String> colName   = new TreeTableColumn<>( "Name"   );
         TreeTableColumn<ObserverTreeItem, String> colMsg    = new TreeTableColumn<>( "Log"    );
         TreeTableColumn<ObserverTreeItem, String> colStatus = new TreeTableColumn<>( "Status" );
         colName  .setCellValueFactory( new TreeItemPropertyValueFactory<>( "name"    ) );
         colMsg   .setCellValueFactory( new TreeItemPropertyValueFactory<>( "message" ) );
         colStatus.setCellValueFactory( new TreeItemPropertyValueFactory<>( "status"  ) );
         colName  .setPrefWidth( 300 );
         colMsg   .setPrefWidth( 350 );
         colStatus.setPrefWidth( 100 );
         tree.getColumns().addAll( colName, colMsg, colStatus );
         node.setExpanded( true );

         ScrollPane pnlCC = new ScrollPane( tree );
         pnlCC.setFitToWidth ( true );
         pnlCC.setFitToHeight( true );

         BorderPane pnlC = new BorderPane( pnlCC, progress, null, null, null );
         progress.setMaxWidth( Double.MAX_VALUE );

         tab.setContent( pnlC );
         tab.setClosable( false );
      }

      void register       () { maxProgress.incrementAndGet(); updateProgress(); }
      void arrive         () { curProgress.incrementAndGet(); updateProgress(); }
      void updateProgress () {
         Platform.runLater( () -> {
            progress.setProgress(
               curProgress.intValue() == 0
                  ? ProgressBar.INDETERMINATE_PROGRESS
                  : (double) curProgress.intValue() / maxProgress.intValue() );
         } );
      }

      @Override public CocoObserver newNode ( String name ) {
         return new ProgressNode( this, node, name );
      }

      @Override public CocoObserver setName ( String name ) {
         Platform.runLater( () -> {
            tab.setText( isDone() ? name : ( "*" + name + "*" ) );
         });
         return super.setName( name );
      }

      @Override public void done () {
         if ( isDone() ) return;
         super.done();
         Platform.runLater( () -> {
            progress.setProgress( 1 );
            tab.setText( nameProperty().get() );
            tab.setClosable( true );
         } );
      }
   }

   /**
    * Child job node
    */
   private class ProgressNode extends ObserverTreeItem {
      final ProgressTab tab;

      public ProgressNode( ProgressTab tab, TreeItem parent, String name ) {
         super( name );
         this.tab = tab;
         if ( parent == tab.node || parent.getParent() == tab.node ) // Count progress of first two levels
            tab.register();
         CountDownLatch latch = new CountDownLatch(1);
         Platform.runLater( () -> {
            node.setExpanded( true );
            parent.getChildren().add( node );
            latch.countDown();
         });
         try {
            latch.await();
         } catch ( InterruptedException ignored ) {}
      }

      @Override public CocoObserver newNode ( String name ) {
         return new ProgressNode( tab, node, name );
      }

      @Override public void done () {
         super.done();
         if ( tab.node == node.getParent() || node.getParent().getParent() == tab.node )
            tab.arrive();
         Platform.runLater( () -> node.setExpanded( false ) );
      }
   }
}