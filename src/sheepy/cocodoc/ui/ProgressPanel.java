package sheepy.cocodoc.ui;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import sheepy.cocodoc.CocoDoc;
import sheepy.cocodoc.CocoObserver;
import sheepy.util.ui.ObservableArrayList;

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
      final ProgressTab result = new ProgressTab( name );
      Platform.runLater( () -> {
         if ( tabWelcome != null ) {
            tabPane.getTabs().remove( tabWelcome );
            tabWelcome = null;
         }
         tabPane.getTabs().add( result.tab );
         tabPane.getSelectionModel().select( result.tab );
      } );
      return result;
   }

   /*******************************************************************************************************************/

   /**
    * Main job tab
    */
   private static class ProgressTab extends ObserverEntity {
      final Tab tab = new Tab( "New Job" );
      private final TreeTableView<ObserverEntity> tree = new TreeTableView<>( node );
      private final ObservableList<ObserverEntity.Log> log = new ObservableArrayList<>();
      private final TableView<ObserverEntity.Log> tblLog = new TableView<>();
      private final ProgressBar progress = new ProgressBar();
      private final Button btnCloseLog = new Button();
      private final Button btnPauseRerun = new Button( "↻" );
      private final Label lblThread = new Label();
      private final SplitPane pnlC = new SplitPane();

      private final AtomicInteger runningThread = new AtomicInteger( 0 );
      private Timer timer; // Can use a global timer, but only if we keep a list of active tab
      private long startTime; // Coarse timer

      private final AtomicInteger maxProgress = new AtomicInteger();
      private final AtomicInteger curProgress = new AtomicInteger();

      public ProgressTab( String name ) {
         super( name );
         Platform.runLater( () -> {
            Region pnlTree = createTreePane();
            Region pnlLog = createLogPane();
            pnlC.getItems().addAll( pnlLog, pnlTree );
            pnlC.setOrientation( Orientation.VERTICAL );
            tab.setContent( pnlC );
            reset( null );
         } );
      }

      private Region createTreePane() {
         TreeTableColumn<ObserverEntity, String> colName   = new TreeTableColumn<>( "Name"   );
         TreeTableColumn<ObserverEntity, String> colMsg    = new TreeTableColumn<>( "Log"    );
         TreeTableColumn<ObserverEntity, String> colStatus = new TreeTableColumn<>( "Status" );
         colName  .setCellValueFactory( new TreeItemPropertyValueFactory<>( "name"    ) );
         colMsg   .setCellValueFactory( new TreeItemPropertyValueFactory<>( "message" ) );
         colStatus.setCellValueFactory( new TreeItemPropertyValueFactory<>( "status"  ) );
         colName  .setPrefWidth( 300 );
         colMsg   .setPrefWidth( 330 );
         colStatus.setPrefWidth( 100 );
         tree.getColumns().addAll( colName, colMsg, colStatus );
         tree.getSelectionModel().selectedItemProperty().addListener( this::updateLog );
         tree.setOnMouseClicked( evt -> {
            if ( evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2 )
               expandLog( null );
         } );

         ScrollPane result = new ScrollPane( tree );
         result.setFitToWidth ( true );
         result.setFitToHeight( true );
         return result;
      }

      private Region createLogPane() {
         HBox pnlTR = new HBox( lblThread, btnPauseRerun );
         BorderPane pnlT = new BorderPane( progress );
         pnlT.setLeft( btnCloseLog );
         pnlT.setRight( pnlTR );
         progress.setMaxSize( Double.MAX_VALUE, Double.MAX_VALUE );
         btnPauseRerun.setOnAction( this::rerun );

         TableColumn<ObserverEntity.Log, String> colTime    = new TableColumn<>( "Time (ms)" );
         TableColumn<ObserverEntity.Log, String> colMessage = new TableColumn<>( "Message"   );
         colTime   .setCellValueFactory( new PropertyValueFactory<>( "time"    ) );
         colMessage.setCellValueFactory( new PropertyValueFactory<>( "message" ) );
         colTime   .setPrefWidth( 100 );
         colMessage.setPrefWidth( 630 );
         tblLog.getColumns().addAll( colTime, colMessage );
         ScrollPane pnlLogScroll = new ScrollPane( tblLog );
         pnlLogScroll.setFitToWidth ( true );
         pnlLogScroll.setFitToHeight( true );
         pnlLogScroll.setMinHeight( 0 );
         tblLog.setItems( log );

         BorderPane pnlC = new BorderPane( pnlLogScroll );
         pnlC.setTop( pnlT );
         return pnlC;
      }

      private double splitPos = 0.5;

      private void expandLog ( ActionEvent evt ) {
         if ( ( progress.getHeight() + 5 ) <= (int) Math.round( pnlC.getHeight() * pnlC.getDividerPositions()[0] ) )
            return;
         pnlC.setDividerPosition( 0, splitPos );
         splitPos = pnlC.getDividerPositions()[ 0 ];
         btnCloseLog.setText( "▲" );
         btnCloseLog.setOnAction( this::collapseLog );
         int i = tree.getSelectionModel().getSelectedIndex();
         if ( i >= 0 ) tree.scrollTo( i );
      }

      private void collapseLog ( ActionEvent evt ) {
         splitPos = pnlC.getDividerPositions()[ 0 ];
         pnlC.setDividerPosition( 0, 0 );
         btnCloseLog.setText( "▼" );
         btnCloseLog.setOnAction( this::expandLog );
      }

      private void updateLog() { updateLog( null, null, tree.getSelectionModel().getSelectedItem() ); }
      private void updateLog( ObservableValue<? extends TreeItem<ObserverEntity>> observable, TreeItem<ObserverEntity> oldValue, TreeItem<ObserverEntity> newValue ) {
         log.clear();
         if ( newValue == null ) {
            log.add( new Log( "Select a process to see message log" ) );
         } else {
            expandLog( null );
            log.addAll( newValue.getValue().getLogs() );
            tblLog.scrollTo( 0 );
         }
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
            tab.setText( ( isDone() ? "✔ " : "▶ " ) + name );
         });
         if ( ! name.equals( nameProperty().get() ) ) super.setName( name );
         return this;
      }

      private void updateThread () {
         Platform.runLater( () -> {
            String txt = "Threads:  " + runningThread.get();
            if ( ! isDone() ) {
               long sec = Math.round( ( System.currentTimeMillis() - startTime ) / 1000 );
               txt += ", Time:  " + sec + "  s";
            }
            lblThread.setText( txt );
         } );
      }

      @Override public void start ( Thread curThread, long baseTime ) {
         super.start( curThread, baseTime );
         runningThread.incrementAndGet();
         // updateThread();
      }

      @Override public void done ( Thread curThread ) {
         if ( isDone() ) return;
         super.done( curThread );
         Platform.runLater( () -> {
            timer.cancel();
            timer = null;
            runningThread.decrementAndGet();
            updateThread();
            setName( nameProperty().get() );
            tab.setClosable( true );
            progress.setProgress( 1 );
            btnPauseRerun.setDisable( false );
            if ( CocoDoc.option.auto_collapse_level <= 0 && canCollapse() )
               node.setExpanded( false );
         } );
      }

      public CocoObserver monitor ( File f ) {
         // TODO: Implement auto rerun
         return this;
      }

      /** Reset panel to initial state, ready to monitor a new job */
      private void reset ( ActionEvent evt ) {
         super.reset();
         assert( Platform.isFxApplicationThread() );
         String name = nameProperty().get();

         if ( timer != null ) timer.cancel();
         startTime = System.currentTimeMillis();
         timer = new Timer( name + " Timer", true );
         timer.schedule( new TimerTask() { @Override public void run() {
            updateThread();
         } }, 100, 100 );

         setName( name );
         tab.setClosable( false );
         btnPauseRerun.setDisable( true );
         progress.setProgress( ProgressBar.INDETERMINATE_PROGRESS );
         maxProgress.set( 0 );
         curProgress.set( 0 );

         node.setExpanded( true );
         collapseLog( null );
         updateLog();
      }

      private void rerun ( ActionEvent evt ) {
         assert( runningThread.get() == 0 );
         reset( null );
         new Thread( () -> CocoDoc.run( this, nameProperty().get() ) ).start();
      }
   }

   /**
    * Child job node
    */
   private static class ProgressNode extends ObserverEntity {
      final ProgressTab tab;

      public ProgressNode ( ProgressTab tab, TreeItem parent, String name ) {
         super( name );
         this.tab = tab;
         Platform.runLater( () -> {
            if ( parent == tab.node || parent.getParent() == tab.node ) // Count progress of first two levels
               tab.register();
            node.setExpanded( true );
            parent.getChildren().add( node );
         } );
      }

      @Override public CocoObserver newNode ( String name ) {
         return new ProgressNode( tab, node, name );
      }

      @Override public void start ( Thread curThread, long baseTime ) {
         super.start( curThread, baseTime );
         tab.runningThread.incrementAndGet();
         // tab.updateThread();
      }

      @Override public void done ( Thread curThread ) {
         super.done( curThread );
         Platform.runLater( () -> {
            tab.runningThread.decrementAndGet();
            // tab.updateThread();
            int level = findLevel( node );
            if ( level <= 2 ) // Count progress of first two levels
               tab.arrive();
            if ( level >= CocoDoc.option.auto_collapse_level && canCollapse() )
               node.setExpanded( false );
         } );
      }

      @Override protected boolean canCollapse () {
         // Cannot collapse if selected
         return super.canCollapse() && ! tab.tree.getSelectionModel().getSelectedItems().contains( node ) ;
      }

      @Override public CocoObserver monitor ( File f ) {
         return tab.monitor( f );
      }

      /** Tree root is 0, top visible nodes are 1. */
      private static int findLevel ( TreeItem t ) {
         int level = -1;
         while ( t != null ) {
            t = t.getParent();
            ++level;
         }
         return level;
      }
   }
}