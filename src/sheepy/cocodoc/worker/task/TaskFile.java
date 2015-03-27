package sheepy.cocodoc.worker.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.StreamSupport;
import sheepy.cocodoc.worker.Block;
import static sheepy.cocodoc.worker.directive.Directive.Action.INLINE;
import sheepy.cocodoc.worker.error.CocoParseError;
import sheepy.cocodoc.worker.error.CocoRunError;

public class TaskFile extends Task {
   @Override public Action getAction () { return Action.FILE; }

   @Override protected Predicate<List<String>> validParam() { return nonEmpty; }
   @Override protected String invalidParamMessage() { return "file() task should have parameters."; }

   @Override public void init () {
      super.init();
      switch ( getDirective().getAction() ) {
         case INLINE :
            break;
         case OUTPUT :
            if ( params.size() > 1 ) {
               log.warning( "Only the last file() will be effective in <?coco-output?>" );
               while ( params.size() > 1 ) params.remove( 0 );
            }
            break;
         default     :
            throwOrWarn( new CocoParseError( "file() task only supports <?coco-inline?> and <?coco-output?>. Current: coco-" + getDirective().getAction().name().toLowerCase() ) );
      }
   }

   public Task addParam ( File ... files ) {
      return addParam( Arrays.asList( files ) );
   }

   public Task addParam ( Iterable<?> files ) {
      return addParam( StreamSupport.stream( files.spliterator(), false ).map( Object::toString ).toArray( String[]::new ) );
   }

   @Override public void run () {
      Block block = getBlock();
      if ( getDirective().getAction() == INLINE ) { // Read file and add to block
         File base = block.getParentBasePath();
         for ( String s : getParams() ) try {
            log.log( Level.FINE, "Reading {1} from base path {0}.", new Object[]{ base, s } );
            File f = new File( base, s );
            try ( FileInputStream in = new FileInputStream( f ) ) {
               byte[] buffer = new byte[ (int) f.length() ];
               int len = in.read( buffer );
               if ( len < buffer.length ) buffer = Arrays.copyOfRange( buffer, 0, len );

               log.log( Level.INFO, "Read {0} bytes from {1}.", new Object[]{ buffer.length, f } );
               block.setBasePath( f.getParentFile() ).appendBinary( buffer );
            }
         } catch ( IOException ex ) { // If not throwing, continue with next parameter
            if ( throwError ) throw new CocoRunError( ex );
         }
      } else { // Set block output
         block.setOutputTarget( this );
      }
   }
}