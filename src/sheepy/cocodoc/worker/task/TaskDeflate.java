package sheepy.cocodoc.worker.task;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import sheepy.cocodoc.CocoRunError;
import sheepy.util.collection.CollectionPredicate;
import static sheepy.util.collection.CollectionPredicate.onlyContains;

public class TaskDeflate extends Task {

   @Override public Action getAction () { return Action.DEFLATE; }

   private static final Predicate<List<String>> validate = CollectionPredicate.<List<String>>size( 0, 2 ).and( onlyContains( Pattern.compile( "\\d|zlib" ) ) );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "deflate() task accepts 0-9 for compression level (default 9), and 'zlib' to include zlib header/checksum: {0}"; }

   @Override protected void run () {
      log( Level.FINER, "Deflating data" );
      boolean zlib = getParams().contains( "zlib" );
      int level = getParams().stream().filter( p -> p.length() == 1 ).map( Integer::parseInt ).findFirst().orElse( Integer.valueOf( 9 ) );

      byte[] data = getBlock().getBinary();
      ByteArrayOutputStream buffer = new ByteArrayOutputStream( data.length / 2 );
      try ( DeflaterOutputStream os = new DeflaterOutputStream( buffer, new Deflater( level, ! zlib ) ) ) {
         os.write( data );
         os.finish();
         getBlock().setBinary( buffer );
         log( Level.FINEST, "Deflated, {0} -> {1}", data.length, buffer.size() );
      } catch ( IOException ex ) {
         throwOrWarn( new CocoRunError( ex ) );
      }
   }
}