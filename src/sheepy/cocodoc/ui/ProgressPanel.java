package sheepy.cocodoc.ui;

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
import javafx.scene.layout.Region;
import sheepy.cocodoc.CocoDoc;
import sheepy.cocodoc.CocoObserver;
import sheepy.util.ui.JavaFX;
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
      JavaFX.runNow( () -> {
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
      private final ProgressBar progress = new ProgressBar( ProgressBar.INDETERMINATE_PROGRESS );
      private final Button btnCloseLog = new Button();
      private final SplitPane pnlC = new SplitPane();

      private final AtomicInteger maxProgress = new AtomicInteger();
      private final AtomicInteger curProgress = new AtomicInteger();

      public ProgressTab( String name ) {
         super( name );
         Region pnlTree = createTreePane();
         Region pnlLog = createLogPane();

         pnlC.getItems().addAll( pnlLog, pnlTree );
         pnlC.setOrientation( Orientation.VERTICAL );

         node.setExpanded( true );
         tab.setContent( pnlC );
         tab.setClosable( false );
         collapseLog( null );
         updateLog();
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
         });

         ScrollPane result = new ScrollPane( tree );
         result.setFitToWidth ( true );
         result.setFitToHeight( true );
         return result;
      }

      private Region createLogPane() {
         BorderPane pnlT = new BorderPane( progress );
         pnlT.setLeft( btnCloseLog );
         progress.setMaxSize( Double.MAX_VALUE, Double.MAX_VALUE );

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
         splitPos = pnlC.getDividerPositions()[0];
         btnCloseLog.setText( "▲" );
         btnCloseLog.setOnAction( this::collapseLog );
         int i = tree.getSelectionModel().getSelectedIndex();
         if ( i >= 0 ) tree.scrollTo( i );
      }

      private void collapseLog ( ActionEvent evt ) {
         splitPos = pnlC.getDividerPositions()[0];
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
            if ( CocoDoc.option.auto_collapse_level <= 0 )
               node.setExpanded( false );
         } );
      }
   }

   /**
    * Child job node
    */
   private static class ProgressNode extends ObserverEntity {
      final ProgressTab tab;

      public ProgressNode( ProgressTab tab, TreeItem parent, String name ) {
         super( name );
         this.tab = tab;
         if ( parent == tab.node || parent.getParent() == tab.node ) // Count progress of first two levels
            tab.register();
         JavaFX.runNow( () -> {
            node.setExpanded( true );
            parent.getChildren().add( node );
         } );
      }

      @Override public CocoObserver newNode ( String name ) {
         return new ProgressNode( tab, node, name );
      }

      @Override public void done () {
         super.done();
         int level = findLevel( node );
         if ( level <= 2 ) // Count progress of first two levels
            tab.arrive();
         Platform.runLater( () -> {
            if ( level >= CocoDoc.option.auto_collapse_level )
               node.setExpanded( false );
         } );
      }

      /** Tree root is 0, top visible nodes are 1. */
      private static int findLevel( TreeItem t ) {
         int level = -1;
         while ( t != null ) {
            t = t.getParent();
            ++level;
         }
         return level;
      }
   }
}