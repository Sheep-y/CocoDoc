package sheepy.cocodoc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.stage.Stage;
import sheepy.cocodoc.worker.Directive;
import static sheepy.cocodoc.worker.Directive.Action.INLINE;
import sheepy.cocodoc.worker.task.TaskCoco;
import sheepy.cocodoc.worker.task.TaskFile;
import sheepy.cocodoc.worker.util.CocoUtils;

public class CocoDoc extends Application {
   private static final CocoConfig config = new CocoConfig();

   @Override public void start ( Stage stage ) {
      /*
      stage.setTitle( "ChocoDoc" );
      stage.setScene( new SceneMain() );
      stage.show();
      */
   }

   public static void main ( String[] args ) {
      try {

         config.parseCommandLine( args );

         System.getProperties().setProperty( "java.util.logging.SimpleFormatter.format", "%5$s\n" );
         Logger.getGlobal().getParent().getHandlers()[0].setLevel( Level.FINE );

         if ( config.runFiles.isEmpty() && new File( "build.cocodoc.conf" ).exists() ) {
            config.runFiles = Collections.singletonList( "build.cocodoc.conf" );
         }
            //CocoDoc.launch( args );
         if ( config.runFiles.size() > 0 ) try {
            new Directive( INLINE,
                  Arrays.asList( new TaskFile().addParam( config.runFiles ), new TaskCoco() )
            ).get();
         } catch ( RuntimeException ex ) {
            ex.printStackTrace();
         } catch (InterruptedException ex) {
            System.err.println( "Interrupted" );
            System.exit( -1 );
         } else {
            throw new IllegalArgumentException( "Build file not specified." );
         }
         System.exit( 0 );

      } catch ( RuntimeException ex ) {

         if ( config.help != null || config.runFiles.isEmpty() ) {
            String doc = CocoUtils.stripHtml( getDoc() );
            System.out.println( doc );
         } else {
            ex.printStackTrace();
            System.out.println( "Input /? or --help for manual.");
         }
         //Platform.exit();
         System.exit( ex.toString().hashCode() );
      }
   }

   public static String getDoc() {
      try ( InputStream is = CocoDoc.class.getResourceAsStream("/doc/manual.xhtml") ) {
         return new Scanner(is).useDelimiter("\\A").next();
      } catch ( IOException | NullPointerException ex ) {
         ex.printStackTrace();
         return "Cannot load documentation.";
      }
   }
}