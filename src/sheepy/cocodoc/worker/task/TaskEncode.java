package sheepy.cocodoc.worker.task;

import SevenZip.Compression.LZMA.Encoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.worker.Block;
import static sheepy.util.collection.CollectionPredicate.onlyContains;
import sheepy.util.text.Escape;
import sheepy.util.text.Text;

public class TaskEncode extends Task {

   @Override public Action getAction () { return Action.ENCODE; }

   private static final String[] validParams = new String[]{ "base64","crlf","lf","js","url","html","xhtml","xml","gz","gzip","zip","7z","lzma" };
   private static final Predicate<List<String>> validate = nonEmpty.and( onlyContains( Arrays.asList( validParams ) ) );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "encode() task should have one or more of " + String.join( ",", validParams ) + ". Actual: {0}"; }

   @Override protected void run () {
      if ( ! hasParams() ) {
         log( Level.INFO, "Skipping encode(), no parameter" );
         return;
      }
      Block block = getBlock();
      if ( ! block.hasData() ) {
         log( Level.INFO, "Skipping encode(), no content" );
         return;
      }
      log( Level.FINER, "Encoding data to {0}", Text.defer( this::getParamText ) );

      int origLen = 0, newLen = 0;
      byte[] data;
      ByteArrayOutputStream buffer;
      boolean gzip = false; // gzip (false) / zlib (true) flag

      for ( String e : getParams() ) {
         log( Level.FINEST, "Encode to {0}", e.toLowerCase() );
         if ( origLen == 0 && ! e.toLowerCase().equals( "base64" ) ) {
            origLen = block.getText().length();
         }
         switch ( e.toLowerCase() ) {
            case "base64" :
               data = block.getBinary();
               if ( origLen == 0 ) origLen = data.length;
               block.setText( Base64.getEncoder().encodeToString( data ) );
               break;

            case "crlf" :
               block.setText( Text.toCrLf( block.getText() ) );
               break;

            case "lf" :
               block.setText( Text.toLf( block.getText() ) );
               break;

            case "js" :
               block.setText( Escape.javascript( block.getText() ) );
               break;

            case "url":
               block.setText( Escape.url( block.getText() ) );
               break;

            case "html" :
            case "xhtml" :
            case "xml" :
               block.setText( Escape.xml( block.getText() ) );
               break;

            case "gz" :
            case "gzip" :
               gzip = true;
               // Fall througth

            case "zip" :
               data = block.getBinary();
               buffer = new ByteArrayOutputStream( data.length / 2 );
               try ( DeflaterOutputStream os = new DeflaterOutputStream( buffer, new Deflater( 9, gzip ) ) ) {
                  os.write( data );
                  os.finish();
                  getBlock().setBinary( buffer );
                  newLen = buffer.size();
               } catch ( IOException ex ) { throwOrWarn( new CocoRunError( ex ) ); }
               break;

            case "7z" :
            case "lzma" :
               data = block.getBinary();
               buffer = new ByteArrayOutputStream( data.length / 2 );
               try ( ByteArrayInputStream inStream = new ByteArrayInputStream( data ) ) {
                  Encoder encoder = new Encoder();
                  encoder.SetEndMarkerMode( true );
                  encoder.SetNumFastBytes( 256 );
                  //encoder.SetDictionarySize( 28 ); // Default 23 = 8M. Max = 28 = 256M.
                  int fileSize = data.length;
                  encoder.WriteCoderProperties( buffer );
                  for (int i = 0; i < 8; i++)
                     buffer.write( ( fileSize >>> (8 * i) ) & 0xFF );
                  encoder.Code( inStream, buffer, -1, -1, null );
                  getBlock().setBinary( buffer );
                  newLen = buffer.size();
               } catch ( IOException ex ) { throwOrWarn( new CocoRunError( ex ) ); }
               break;
               /* Decode:
                 int propertiesSize = 5;
                 byte[] properties = new byte[propertiesSize];
                 if (inStream.read(properties, 0, propertiesSize) != propertiesSize) throw new Exception("input .lzma file is too short");
                 SevenZip.Compression.LZMA.Decoder decoder = new SevenZip.Compression.LZMA.Decoder();
                 if (!decoder.SetDecoderProperties(properties)) throw new Exception("Incorrect stream properties");
                 long outSize = 0;
                 for (int i = 0; i < 8; i++) {
                         int v = inStream.read();
                         if (v < 0) throw new Exception("Can't read stream size");
                         outSize |= ((long)v) << (8 * i);
                 }
                 if (!decoder.Code(inStream, outStream, outSize)) throw new Exception("Error in data stream");
               */

            default :
               throwOrWarn( new CocoParseError( "Unknown encode parameter: " + e ) );
         }
      }
      if ( newLen == 0 ) newLen = block.getText().length();
      log( Level.FINEST, "Data Encoded, {0} -> {1}", new Object[]{ origLen, newLen } );
   }
}