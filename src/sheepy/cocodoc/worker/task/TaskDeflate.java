package sheepy.cocodoc.worker.task;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import sheepy.util.collection.NullData;

public class TaskDeflate extends Task {

   @Override public Action getAction () { return Action.DEFLATE; }

   private static final Predicate<List<String>> validate = params ->
         NullData.isEmpty( params ) || ( params.size() <= 2 && params.stream().allMatch( p -> p.equals( "zip" ) || p.matches( "0-9" ) ) );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "deflate() task accepts 0-9 for compression level (default 9), and 'zlib' to include zlib header/checksum: {0}"; }

   @Override public void run () {
      boolean zlib = getParams().contains( "zlib" );
      int level = getParams().stream().filter( p -> p.length() == 1 ).map( Integer::parseInt ).findFirst().orElse( Integer.valueOf( 9 ) );

      byte[] data = getBlock().getBinary();
      ByteArrayOutputStream buffer = new ByteArrayOutputStream( data.length / 2 );
      try ( DeflaterOutputStream os = new DeflaterOutputStream( buffer, new Deflater( level, ! zlib ) ) ) {
         os.write( data );
         os.finish();
         getBlock().setBinary( buffer );
         log.log( Level.FINE, "Deflate({2},{3}) {0} bytes to {1}.", new Object[]{ data.length, buffer.size(), zlib ? "zlib" : "gz", level } );
      } catch ( IOException ex ) {
         Logger.getLogger(TaskDeflate.class.getName()).log(Level.SEVERE, null, ex);
      }
   }
}