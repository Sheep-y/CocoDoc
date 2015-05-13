package sheepy.cocodoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.stage.Stage;
import static sheepy.cocodoc.CocoUtils.stripHtml;
import sheepy.cocodoc.ui.MainStage;
import sheepy.cocodoc.worker.directive.Directive;
import static sheepy.cocodoc.worker.directive.Directive.Action.INLINE;
import sheepy.cocodoc.worker.task.TaskCoco;
import sheepy.cocodoc.worker.task.TaskFile;

public class CocoDoc extends Application {
   public static final CocoConfig config = new CocoConfig();
   public static MainStage stage;

   public static void main ( String[] args ) {
      try {
         config.parseCommandLine( args );

         System.getProperties().setProperty( "java.util.logging.SimpleFormatter.format", "%5$s\n" );
         Logger.getGlobal().getParent().getHandlers()[0].setLevel( Level.FINE );

         CocoDoc.launch( args );

      } catch ( RuntimeException ex ) {
         showHeadlessHelp( ex );
         System.exit( ex.toString().hashCode() );
      }
   }

   @Override public void start ( Stage stage ) {
      try {

         this.stage = new MainStage( stage );
         new Timer().schedule( new TimerTask () { @Override public void run() {
            List<String> files = config.runFiles;
            if ( files.size() > 0 ) {
               CocoDoc.run( files.toArray( new String[ files.size() ] ) );
               CocoDoc.this.stage.autoClose();
            }
         } }, 100 );

      } catch ( Exception ex ) {
         showHeadlessHelp( ex );
      }
   }

   public static void run ( String ... files ) {
      List<Directive> dirs = new ArrayList<>( config.runFiles.size() );
      for ( String file : files ) try {
         dirs.add( Directive.create( INLINE,
            Arrays.asList( new TaskFile().addParam( file ), new TaskCoco() )
         ).setObserver( stage.newNode( file ) ).start( null ) );
      } catch ( RuntimeException ex ) {
         ex.printStackTrace();
      }
      for ( Directive dir : dirs )try {
         dir.get();
      } catch ( InterruptedException ex ) {}
   }

   public static void showHeadlessHelp ( Exception ex ) {
      if ( config.help != null ) {

         String doc;
         try {
            doc = stripHtml( CocoUtils.getText( CocoDoc.config.help ) );
            if ( config.help.equals( CocoConfig.LGPL_FILE ) )
               doc += "\u00A0\n\u00A0\n\u00A0\n" + stripHtml( CocoUtils.getText( CocoConfig.GPL_FILE ) );
         } catch ( IOException err ) {
            doc = err.getMessage();
         }

         System.out.println( doc );
         System.exit( 0 );

      } else {
         if ( ex != null ) ex.printStackTrace();
         System.out.println( "Input /? or --help for manual, or --license for the license.");

      }
   }
}