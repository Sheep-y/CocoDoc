package sheepy.cocodoc.ui;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sheepy.cocodoc.CocoConfig;
import sheepy.cocodoc.CocoDoc;
import sheepy.cocodoc.CocoMonitor;
import sheepy.cocodoc.CocoUtils;
import sheepy.cocodoc.worker.Worker;

public class MainStage {

   private final Stage stage;
   private final ProgressPanel progress = new ProgressPanel();

   private BorderPane   pnlC = new BorderPane();
     private TabPane        tabs = new TabPane();
       private Node           workPane = progress.getPanel();
       private TabPane        docPane = new TabPane();
         private WebView web = new WebView();
     private Button       btnRun = new Button();
   //BorderPane docPane = new BorderPane( web );

   public MainStage( Stage stage ) {
      this.stage = stage;
      stage.setTitle( "ChocoDoc" );
      stage.setScene( new Scene( pnlC, 760, 580 ) );
      stage.setOnCloseRequest( e -> Worker.stop() );

      pnlC.setCenter( tabs );
      pnlC.setBottom( btnRun );
      resetBtnRun();
      btnRun.setMaxWidth( Double.MAX_VALUE );

      createTab( tabs, "Build", workPane );
      createTab( tabs, "Documents", docPane );

      createDocTab( docPane, "Help", CocoConfig.HELP_FILE );
      createDocTab( docPane, "License 1", CocoConfig.LGPL_FILE );
      createDocTab( docPane, "License 2", CocoConfig.GPL_FILE );
      createDocTab( docPane, "Program Design", CocoConfig.DESIGN_FILE );

      if ( CocoDoc.config.help != null ) {
         tabs.getSelectionModel().select( 1 );
         switch ( CocoDoc.config.help ) {
            case CocoConfig.HELP_FILE:
               docPane.getSelectionModel().select( 0 );
               break;
            case CocoConfig.LGPL_FILE:
               docPane.getSelectionModel().select( 1 );
               break;
            case CocoConfig.GPL_FILE:
               docPane.getSelectionModel().select( 2 );
               break;
            case CocoConfig.DESIGN_FILE:
               docPane.getSelectionModel().select( 3 );
               break;
         }
      }

      stage.addEventFilter( MouseEvent.MOUSE_RELEASED, this::stopAutoClose );
      stage.addEventFilter( KeyEvent.KEY_RELEASED, this::stopAutoClose );
      stage.show();
   }

   private Tab selectedDocTab;
   public Tab createDocTab( TabPane parent, String title, String file ) {
      Tab tab = createTab( parent, title, null );
      tab.setOnSelectionChanged( e -> { // Loads and moves web view with tab.
         if ( tab.isSelected() && selectedDocTab != tab ) {
            selectedDocTab = tab;
            for ( Tab t : parent.getTabs() ) t.setContent( null );
            tab.setContent( web );
            try {
               web.getEngine().loadContent( CocoUtils.getText( file ) );
            } catch (IOException ex ) {
               web.getEngine().loadContent( ex.getMessage() );
            }
         }
      } );
      if ( selectedDocTab == null )
         tab.onSelectionChangedProperty().get().handle( null );
      return tab;
   }

   private Tab createTab( TabPane parent, String title, Node content ) {
      Tab tab = new Tab( title, content );
      tab.setClosable( false );
      parent.getTabs().add( tab );
      return tab;
   }

   /*******************************************************************************************************************/

   public CocoMonitor getMonitor () {
      return progress;
   }

   /*******************************************************************************************************************/

   private void resetBtnRun() {
      btnRun.setText( "New Build" );
      btnRun.setOnAction( this::btnRunOnAction );
   }

   private FileChooser dlgOpen;
   public void btnRunOnAction ( ActionEvent evt ) {
      if ( dlgOpen == null ) {
         dlgOpen = new FileChooser();
         dlgOpen.setTitle( "Run CocoDoc on..." );
      }
      File file = dlgOpen.showOpenDialog( stage );
      if ( file != null ) {
         CocoDoc.run( file.toString() );
      }
   }

   Timer autoClose;
   int countdown = 0;
   public synchronized void autoClose() {
      if ( autoClose != null ) autoClose.cancel();

      countdown= 5;
      autoClose = new Timer( "AutoClose", true );

      Platform.runLater( () -> {
         btnRun.setOnAction( this::stopAutoClose );
         btnRun.requestFocus();
         autoClose.schedule( new TimerTask() { @Override public void run() {
            Platform.runLater( () -> {
               btnRun.setText( "Auto close in " + countdown + " (Stop)" );
               if ( countdown-- == 0 ) synchronized( MainStage.this ) {
                  autoClose.cancel();
                  stage.close();
               }
            } );
         } }, 0, 1000 );
      });
   }

   private synchronized void stopAutoClose( Event evt ) {
      if ( autoClose == null ) return;
      autoClose.cancel();
      autoClose = null;
      // Delay reset a little bit to prevent double trigger of button action
      Timer reset = new Timer();
      reset.schedule( new TimerTask() { @Override public void run() {
         Platform.runLater( MainStage.this::resetBtnRun );
         reset.cancel();
      } }, 20 );
   }
}