package sheepy.cocodoc.ui;

import java.io.IOException;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import sheepy.cocodoc.CocoConfig;
import sheepy.cocodoc.CocoUtils;

public class MainStage {

   private WebView web = new WebView();

   private TabPane tabs = new TabPane();
   private TabPane docPane = new TabPane();
   //BorderPane docPane = new BorderPane( web );

   public MainStage( Stage stage ) {
      stage.setTitle( "ChocoDoc" );
      stage.setScene(new Scene( tabs, 400, 300 ) );

      createTab( tabs, "Documents", docPane );

      createDocTab( docPane, "Help", CocoConfig.HELP_FILE );
      createDocTab( docPane, "License 1", CocoConfig.LGPL_FILE );
      createDocTab( docPane, "License 2", CocoConfig.GPL_FILE );
      createDocTab( docPane, "Program Design", CocoConfig.DESIGN_FILE );

      stage.show();
   }

   private Tab selectedDocTab;
   public Tab createDocTab( TabPane parent, String title, String file ) {
      Tab tab = createTab( parent, title, null );
      tab.setOnSelectionChanged( e -> {
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

   public Tab createTab( TabPane parent, String title, Node content ) {
      Tab tab = new Tab( title, content );
      tab.setClosable( false );
      parent.getTabs().add( tab );
      return tab;
   }

}