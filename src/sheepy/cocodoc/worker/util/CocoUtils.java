package sheepy.cocodoc.worker.util;

/**
 * Assorted routines to simplify other CocoDoc classes.
 */
public class CocoUtils {

   public static String trimLine( String input ) {
      return input.replaceAll( "(\\s*\r?\n)+", System.lineSeparator() ); // Empty lines
   }

   public static String stripHtml( String html ) {
      return trimLine( html
            .replaceAll( "\\r", "" ) // Normalise
            .replaceAll( "<!--(.|\n)*?-->", "" ) // Comments
            .replaceFirst( "<head[^>]*>(.*|\n)*?</head[ \t\n]*>", "" ) // <head>, I do not have the time to support optional head start tag
            .replaceAll( "<[^>]+>", "" ) // Tags
            .replaceAll( "&lt;?", "<" ).replaceAll( "&gt;?", ">" ).replace( "&quot;?", "\"" ) // TODO: Replace with http://www.unbescape.org/
            .replaceAll( "\n\n+", "\n" ) // Remove blank lines
            .replaceAll( "^\n+|\n+$", "" ) // Remove leading / trailing blank line
         ); // Un-escape HTML
   }


}
