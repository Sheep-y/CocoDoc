package sheepy.cocodoc.worker.task;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.worker.Block;
import sheepy.util.collection.CollectionPredicate;

public class TaskXsl extends Task {

   @Override public Action getAction () { return Action.XSL; }

   private static final Predicate<List<String>> validate = CollectionPredicate.size(1);
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "xsl() task should take a single xsl file as parameter."; }

   ZonedDateTime app_build_time;
   @Override protected void run () {
      if ( ! hasParams() ) {
         log( Level.WARNING, "Skipping xsl(), no parameter" );
         return;
      }

      log( Level.FINER, "Applying XSL" );
      Block block = getBlock();
      File base = block.getParentBasePath();

      for ( String s : getParams() ) try {

         log( Level.FINEST, "Applying XSL {0}", s );
         String data = block.getText().toString();
         StringWriter buf = new StringWriter();
         StreamResult out = new StreamResult( buf );

         Transformer transform = TransformerFactory.newInstance().newTransformer( new StreamSource( new File( base, s ) ) );
         transform.transform( new StreamSource( new StringReader( data) ), out );
         block.setText( buf.toString() );
         log( Level.FINEST, "Applied XSL: {0} -> {1}", block.getText().length(), buf.getBuffer().length() );

      } catch ( Exception ex ) {
         try {
            throwOrWarn( ex );
         } catch ( Exception x ) {
            throw new CocoRunError( "Cannot apply XSL", x );
         }
      }
   }
}