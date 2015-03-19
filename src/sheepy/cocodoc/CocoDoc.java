package sheepy.cocodoc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.stage.Stage;
import sheepy.cocodoc.worker.directive.Directive;
import static sheepy.cocodoc.worker.directive.Directive.Action.INLINE;
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

         //CocoDoc.launch( args );
         if ( config.runFiles.size() > 0 ) try {
               Directive.create( INLINE,
                  Arrays.asList( new TaskFile().addParam( config.runFiles ), new TaskCoco() )
               ).get();
            } catch ( RuntimeException ex ) {
               ex.printStackTrace();
            } catch ( InterruptedException ex ) {
               System.err.println( "Interrupted" );
               System.exit( -1 );
         } else {
            showHeadlessHelp( null );
         }
         System.exit( 0 );

      } catch ( RuntimeException ex ) {
         showHeadlessHelp( ex );
         System.exit( ex.toString().hashCode() );
      }
   }

   public static void showHeadlessHelp ( Exception ex ) {
      if ( config.help != null ) {
         String doc = CocoUtils.stripHtml( getDoc( config.help ) );
         System.out.println( doc );
         System.exit( 0 );
      } else {
         if ( ex != null ) ex.printStackTrace();
         System.out.println( "Input /? or --help for manual, or --license for the LGPL license.");
      }
   }

   public static String getDoc( String file ) {
      try ( InputStream is = CocoDoc.class.getResourceAsStream( "/doc/" + file ) ) {
         String result = new Scanner(is).useDelimiter( "\\A" ).next();
         if ( file.contains( "_lgpl." ) ) result += "\u00A0\n\u00A0\n\u00A0\n" + getDoc( "license_gpl.html" );
         return result;

      } catch ( IOException | NullPointerException ex ) {
         ex.printStackTrace();
         return "Cannot load documentation " + file + ".";
      }
   }
}