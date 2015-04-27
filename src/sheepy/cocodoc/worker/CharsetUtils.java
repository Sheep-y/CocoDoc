package sheepy.cocodoc.worker;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

/**
 * Charset encode / decode helper functions
 */
public class CharsetUtils {

   public static String decode ( byte[] data, CharsetDecoder decoder ) throws CharacterCodingException {
      if ( data.length <= 0 ) return "";
      return decoder.decode(ByteBuffer.wrap(data)).toString();
   }

   public static byte[] encode ( CharSequence text, CharsetEncoder encoder ) throws CharacterCodingException {
      if ( text.length() <= 0 ) return new byte[0];
      ByteBuffer buf = encoder.encode(CharBuffer.wrap(text));
      byte[] result = new byte[ buf.limit() ];
      buf.get( result );
      return result;
   }

   public static CharsetDecoder strictDecoder(Charset charset) {
      CharsetDecoder result = charset.newDecoder();
      result.onMalformedInput(CodingErrorAction.REPORT);
      result.onUnmappableCharacter(CodingErrorAction.REPORT);
      return result;
   }

   public static CharsetEncoder strictEncoder(Charset charset) {
      CharsetEncoder result = charset.newEncoder();
      result.onMalformedInput(CodingErrorAction.REPORT);
      result.onUnmappableCharacter(CodingErrorAction.REPORT);
      return result;
   }

   // Returns String or CharacterCodingException
   public static Object tryDecode(byte[] data, CharsetDecoder decoder) {
      try {
         return decode(data, decoder);
      } catch (CharacterCodingException ex) {
         return ex;
      }
   }

   // Returns byte[] or CharacterCodingException
   public static Object tryEncode(CharSequence text, CharsetEncoder encoder) {
      try {
         return encode(text, encoder);
      } catch (CharacterCodingException ex) {
         return ex;
      }
   }

}
