package sheepy.cocodoc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import static sheepy.util.collection.CollectionPredicate.contains;

public class CocoConfig {

   public String help;
   public List<String> runFiles = new ArrayList<>(0);

   public CocoConfig parseCommandLine( String[] args ) {
      if ( args == null || args.length <= 0 ) {
         if ( new File( "build.cocodoc.conf" ).exists() ) {
            args = new String[]{ "build.cocodoc.conf" };
         }
      }
      List<String> arguments = Arrays.asList( args );

      if ( contains( Pattern.compile( "/\\?|--help" ) ).test( arguments ) ) {
         help = "manual.xhtml";
      } else if ( contains( Pattern.compile( "--license" ) ).test( arguments ) ) {
         help = "license_lgpl.html";
      } else {
         runFiles = new ArrayList<>( Arrays.asList( args ) );
      }

      return this;
   }
}
