package sheepy.cocodoc.ui;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sheepy.cocodoc.CocoConfig;
import sheepy.cocodoc.CocoDoc;
import static sheepy.cocodoc.CocoDoc.config;
import sheepy.cocodoc.CocoObserver;
import sheepy.cocodoc.CocoUtils;
import sheepy.cocodoc.worker.Worker;
import sheepy.util.Time;
import sheepy.util.collection.NullData;

public class MainStage {

   private final Stage stage;
   private final ProgressPanel progress = new ProgressPanel( this );

   private final BorderPane   pnlC = new BorderPane();
     private final TabPane        tabs = new TabPane();
       private final Node           workPane = progress.getPanel();
       private final TabPane        docPane = new TabPane();
         private final WebView web = new WebView();
     private final Button       btnRun = new Button();

   public MainStage ( Stage stage ) {
      this.stage = stage;
      stage.setTitle( "ChocoDoc 1.1" );
      stage.setScene( new Scene( pnlC, 760, 580 ) );
      stage.setOnCloseRequest( e -> Worker.stop() );

      pnlC.setCenter( tabs );
      pnlC.setBottom( btnRun );
      resetBtnRun();
      btnRun.setMaxWidth( Double.MAX_VALUE );

      createTab( tabs, "⚒ Build", workPane );
      createTab( tabs, "📖 Documents", docPane );

      createDocTab( docPane, "📖 Help", CocoConfig.HELP_FILE );
      createDocTab( docPane, "⚖ License 1", CocoConfig.LGPL_FILE );
      createDocTab( docPane, "⚖ License 2", CocoConfig.GPL_FILE );
      createDocTab( docPane, "⚙ Program Design", CocoConfig.DESIGN_FILE );

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

      try {
         stage.getIcons().add( new Image( CocoUtils.getStream( "res/img/favicon.gif" ) ) );
      } catch ( Exception ignored ) {}
      stage.addEventFilter( MouseEvent.MOUSE_RELEASED, this::stopAutoClose );
      stage.addEventFilter( KeyEvent.KEY_RELEASED, this::stopAutoClose );
      stage.iconifiedProperty().addListener( this::stopAutoClose );
      stage.maximizedProperty().addListener( this::stopAutoClose );
      stage.show();

      if ( ! config.runFiles.isEmpty() ) {
         new Thread( () -> {
            while ( CocoDoc.stage == null ) Time.sleep( 50 );
            CocoDoc.run( NullData.stringArray( config.runFiles ) );
            startAutoClose();
         } ).start();
      }
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
               if ( file == CocoConfig.LGPL_FILE || file == CocoConfig.GPL_FILE ) {
                  web.getEngine().loadContent( "This program is free software: you can redistribute it and/or modify " +
                        "it under the terms of the <a href='http://www.gnu.org/licenses/lgpl.html'>Lesser GNU General Public License</a> " +
                        "as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.");
               } else {
                  web.getEngine().loadContent( ex.getMessage() );
               }
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

   public CocoObserver newNode( String name ) {
      return progress.newNode( name );
   }

   /*******************************************************************************************************************/

   /** Set to true to disable current or fucure autoclose requests */
   boolean noAutoClose = false;

   private void resetBtnRun() {
      btnRun.setText( "⚒ New Build ⚒" );
      btnRun.setOnAction( this::btnRunOnAction );
   }

   private FileChooser dlgOpen;
   public void btnRunOnAction ( Event evt ) {
      if ( dlgOpen == null ) {
         dlgOpen = new FileChooser();
         dlgOpen.setTitle( "Run CocoDoc on..." );
      }
      File file = dlgOpen.showOpenDialog( stage );
      if ( file != null )
         Worker.run( () -> CocoDoc.run( file.toString() ) );
   }

   /*******************************************************************************************************************/

   Future autoClose; // Non-null if autoclose is in progress. Call to cancel it.

   public void startAutoClose() {
      Platform.runLater(() -> {
         if ( autoClose != null ) return; // Already started
         final int auto_close_second = CocoDoc.option.auto_close_second;
         if ( noAutoClose || auto_close_second < 0 ) return;

         btnRun.setOnAction( this::stopAutoClose ); // Just to be safe.  In fact any click already stop auto close.
         btnRun.requestFocus();
         autoClose = Time.countDown( auto_close_second, 1000, ( e ) -> {
            Platform.runLater( () -> {
               btnRun.setText("🚪 Auto close 🚪 in " + e + " (Stop)" );
               if ( e <= 0 )
                  stage.close();
            } );
         } );
      } );
   }

   private void stopAutoClose( Observable o ) { stopAutoClose( (Event) null ); }
   private void stopAutoClose( Event evt ) {
      noAutoClose = true;
      if ( autoClose == null ) return;
      autoClose.cancel( true );
      autoClose = null;
      stage.removeEventFilter( MouseEvent.MOUSE_RELEASED, this::stopAutoClose );
      stage.removeEventFilter( KeyEvent.KEY_RELEASED, this::stopAutoClose );
      stage.iconifiedProperty().removeListener( this::stopAutoClose );
      stage.maximizedProperty().removeListener( this::stopAutoClose );
      // Run reset in new thread to ensure it happens in a new event queue, to avoid double trigger of button action
      new Thread( () -> {
         Platform.runLater( MainStage.this::resetBtnRun );
      }, "Stop autoclose" ).start();
   }
}