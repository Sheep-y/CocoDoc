package sheepy.cocodoc.ui;

import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import sheepy.cocodoc.CocoMonitor;

public class ProgressPanel implements CocoMonitor {

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

   @Override public CocoMonitor newNode( String name ) {
      ProgressTab result = new ProgressTab();
      synchronized ( this ) {
         Platform.runLater( () -> {
            if ( tabWelcome != null ) {
               tabPane.getTabs().remove( tabWelcome );
               tabWelcome = null;
            }
            tabPane.getTabs().add( result.tab );
         });
      }
      return result;
   }

   @Override public CocoMonitor setText( String name ) {
      throw new UnsupportedOperationException("Cannot set text of workspace; please call newNode() to create new tree.");
   }

   @Override public CocoMonitor setDone ( boolean done ) {
      throw new UnsupportedOperationException("Cannot set status of workspace; please call newNode() to create new tree.");
   }

   /*******************************************************************************************************************/

   private abstract class TreeItemMonitor implements CocoMonitor {
      protected volatile boolean done = false;
      protected volatile String name = "";
      protected final TreeItem<String> node = new TreeItem( "Initialising" );
      protected String displayText() {
         return done ? name : ( "*" + name + "*" );
      }
      @Override public CocoMonitor setText ( String name ) {
         this.name = name;
         Platform.runLater( () -> {
            node.setValue( displayText() );
         });
         return this;
      }
      @Override public CocoMonitor setDone ( boolean done ) {
         this.done = true;
         Platform.runLater( () -> {
            node.setExpanded( false );
            setText( name );
         } );
         return this;
      }
   }

   // Main job tab
   private class ProgressTab extends TreeItemMonitor {
      private final Tab tab = new Tab( "New Job" );
      private final ProgressBar progress = new ProgressBar( ProgressBar.INDETERMINATE_PROGRESS );
      private final AtomicInteger maxProgress = new AtomicInteger();
      private final AtomicInteger curProgress = new AtomicInteger();

      public ProgressTab() {
         ScrollPane pnlCC = new ScrollPane( new TreeView<String>( node ) );
         pnlCC.setFitToWidth( true );
         node.setExpanded( true );

         BorderPane pnlC = new BorderPane( pnlCC, progress, null, null, null );
         progress.setMaxWidth( Double.MAX_VALUE );

         tab.setContent( pnlC );
         tab.setClosable( false );
      }

      void register       () { maxProgress.incrementAndGet(); updateProgress(); }
      void arrive         () { curProgress.incrementAndGet(); updateProgress(); }
      void updateProgress () {
         Platform.runLater( () -> {
            progress.setProgress( (double) curProgress.intValue() / maxProgress.intValue() );
         });
      }

      @Override public CocoMonitor newNode ( String name ) {
         return new ProgressNode( this, node ).setText( name );
      }

      @Override public CocoMonitor setText ( String name ) {
         super.setText( name );
         Platform.runLater( () -> {
            tab.setText( displayText() );
         });
         return this;
      }

      @Override public CocoMonitor setDone ( boolean done ) {
         super.setDone( done );
         Platform.runLater( () -> {
            progress.setProgress( 1 );
            tab.setClosable( true );
         } );
         return this;
      }
   }

   // Child job node
   private class ProgressNode extends TreeItemMonitor {
      private final ProgressTab tab;

      public ProgressNode( ProgressTab tab, TreeItem parent ) {
         this.tab = tab;
         tab.register();
         node.setExpanded( true );
         parent.getChildren().add( node );
      }

      @Override public CocoMonitor newNode ( String name ) {
         return new ProgressNode( tab, node ).setText( name );
      }

      @Override public CocoMonitor setDone ( boolean done ) {
         tab.arrive();
         return super.setDone( done );
      }
   }
}