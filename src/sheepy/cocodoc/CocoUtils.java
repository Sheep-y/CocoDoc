package sheepy.cocodoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Scanner;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.concurrent.CacheMap;
import sheepy.util.concurrent.ObjectPoolMap;

/**
 * Assorted routines to simplify other CocoDoc classes.
 */
public class CocoUtils {

   /** Cache Pattern and Matcher for simple reuse. */
   private static final CacheMap<String, Pattern> patternPool = CacheMap.create(
         (pattern) -> Pattern.compile(pattern, Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL)
   );
   public static final ObjectPoolMap<String, Matcher> tagPool = ObjectPoolMap.create(
         (key) -> patternPool.get(key).matcher(""), (v) -> v.reset("")
   );

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


   /**
    * Find the build time of given class.
    *
    * @return The date if it can be determined
    */
   public static Optional<Long> getBuildTime( final Class currentClass ) {
      long d = 0;
      try {
         URL resource = currentClass.getResource( currentClass.getSimpleName() + ".class" );
         if ( resource == null ) return null;
         switch ( resource.getProtocol() ) {
            case "file":
               d = new File( resource.toURI() ).lastModified();
               break;

            case "jar":
               String path = resource.getPath();
               d = new File( path.substring( 5, path.indexOf( "!" ) ) ).lastModified();
               break;

            case "zip":
               path = resource.getPath();
               File jarFileOnDisk = new File( path.substring( 0, path.indexOf("!") ) );
               //long jfodLastModifiedLong = jarFileOnDisk.lastModified ();
               //Date jfodLasModifiedDate = new Date(jfodLastModifiedLong);
               try( JarFile jf = new JarFile( jarFileOnDisk ) ) {
                  d = jf.getEntry( path.substring( path.indexOf( "!" ) + 2 ) ).getTime(); //Skip the ! and the //
               }
         }
      } catch ( URISyntaxException | IOException | SecurityException ignored) { }
      return Optional.ofNullable( d == 0 ? null : d );
   }

   public static Optional<Long> getBuildTime() {
      return getBuildTime( CocoUtils.class );
   }

   public static ZonedDateTime milliToZonedDateTime ( long epoch ) {
      return Instant.ofEpochMilli( epoch ).atZone( ZoneId.systemDefault() );
   }

   public static String formatTime ( ZonedDateTime time ) {
      return time.truncatedTo( ChronoUnit.SECONDS ).format( DateTimeFormatter.ISO_INSTANT );
   }

   public static InputStream getStream ( String file ) {
      try {
         File f = new File( file );
         if ( f.exists() && f.isFile() && f.canRead() ) {
            return new FileInputStream( f );
         } else {
            file = file.replace( '\\', '/' );
            if ( ! file.startsWith( "/" ) ) file = '/' + file;
            return CocoUtils.class.getResourceAsStream( file );
         }
      } catch ( FileNotFoundException ex ) {
         return null;
      }
   }

   public static String getText ( String file ) throws IOException {
      InputStream is = getStream( file );
      if ( is == null ) throw new FileNotFoundException( "Resource not found: " + file );
      try {
         return new Scanner( is, "UTF-8" ).useDelimiter("\\A").next();
      } finally {
         is.close();
      }
   }
}