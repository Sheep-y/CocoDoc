package sheepy.cocodoc.worker.task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.StreamSupport;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.CocoUtils;
import sheepy.cocodoc.worker.Block;
import static sheepy.cocodoc.worker.directive.Directive.Action.INLINE;
import sheepy.util.text.Text;

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
               log( Level.WARNING, "Only the last file() will be effective in <?coco-output?>" );
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

   @Override protected void run () {
      if ( ! hasParams() ) {
         log( Level.INFO, "Skipping file(), no parameter" );
         return;
      }
      Block block = getBlock();

      if ( getDirective().getAction() == INLINE ) { // Read file and add to block

         File base = block.getParentBasePath();
         for ( String s : getParams() ) try {
            File f = new File( base, s );
            log( Level.FINE, "Reading {0}", Text.defer( () -> f.toPath().normalize().toString() ) );
            byte[] buffer;
            try ( InputStream in = CocoUtils.getStream( f.getPath() ) ) {
               if ( in == null ) throw new IOException( "File not found: " + f );
               buffer = new byte[ in.available() ];
               int len = in.read( buffer );
               if ( len < buffer.length ) buffer = Arrays.copyOfRange( buffer, 0, len ); // In case file size changed
               if ( f.exists() && f.isFile() && f.canRead() && getBlock().getObserver() != null )
                  getBlock().getObserver().monitor( f );
            }
            block.stats().addInBytes( buffer.length );
            log( Level.FINEST, "Read {0} bytes from {1}.", buffer.length, f );
            block.appendBinary( buffer )
                 .setBasePath( f.getParentFile() ).setName( f.toString() )
                 .stats().setMTime( CocoUtils.milliToZonedDateTime( f.lastModified() ) );
         } catch ( IOException ex ) { // If not throwing, continue with next parameter
            throwOrWarn( new CocoRunError( ex ) );
         }
         log( Level.FINEST, "Finished reading." );

      } else { // Set block output
         block.setOutputTarget( this );
         log( Level.FINER, "Output target set." );
      }
   }
}