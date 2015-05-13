package sheepy.cocodoc.worker.task;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.worker.Block;
import static sheepy.util.collection.CollectionPredicate.noDuplicate;
import static sheepy.util.collection.CollectionPredicate.onlyContains;

public class TaskTest extends Task {

   @Override public Action getAction () { return Action.TEST; }

   private static final String[] validParams = new String[]{ "noerr","xml" };
   private static final Predicate<List<String>> validate = nonEmpty.and( noDuplicate() ).and( onlyContains( Arrays.asList( validParams ) ) );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "test() task should have one or more of " + String.join( ",", validParams ) + " with no duplicate. Actual: {0}"; }

   @Override protected void run () {
      if ( ! hasParams() ) {
         log( Level.INFO, "Skipping, no parameter" );
         return;
      }
      log( Level.FINER, "Validating data" );

      Block block = getBlock();
      for ( String e : getParams() ) {
         log( Level.FINER, "Validating data as {0}", e );
         switch ( e ) {
            case "xml":
               try {
                  if ( ! block.hasData() ) throw new SAXException( "No data" );
                  SAXParserFactory factory = SAXParserFactory.newInstance();
                  SAXParser parser = factory.newSAXParser();
                  DefaultHandler handler = new DefaultHandler();
                  InputSource source = new InputSource();
                  if ( block.hasText() ) {
                     log( Level.FINEST, "Validating text data as XML." );
                     source.setCharacterStream( new StringReader( block.getText().toString() ) );
                  } else if ( block.hasBinary() ) {
                     log( Level.FINEST, "Validating binary data as XML." );
                     source.setByteStream( new ByteArrayInputStream( block.getBinary() ) );
                  }
                  parser.parse( source, handler );
                  log( Level.INFO, "Data is valid XML." );
               } catch ( IOException | ParserConfigurationException ex ) {
                  throwOrWarn( new CocoRunError( ex ) );
               } catch ( SAXException ex ) {
                  throwOrWarn( new CocoRunError( "Test failed: Invalid XML.", ex ) );
               }
               break;
            default :
               throwOrWarn( new CocoParseError( "Unknown test parameter: " + e ) );
         }
      }
      log( Level.FINER, "Data validated." );
   }
}