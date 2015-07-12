package sheepy.cocodoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import static sheepy.util.collection.CollectionPredicate.contains;

/**
 * Fixed configs, like file location, or command line parameters.
 */
public class CocoConfig {

   public String help;
   public List<String> runFiles = new ArrayList<>(0);

   public static final String DEFAULT_BUILD    = "build.cocodoc.conf";
   public static final String DOC_PATH    = "res/";
   public static final String HELP_FILE   = DOC_PATH+ "manual.xhtml";
   public static final String DESIGN_FILE = DOC_PATH+ "design.xhtml";
   public static final String LGPL_FILE   = DOC_PATH+ "license_lgpl.xhtml";
   public static final String GPL_FILE    = DOC_PATH+ "license_gpl.xhtml";

   public static final Level MICRO = new Level( "MICRO", 200 ){};
   public static final Level NANO  = new Level( "NANO", 100 ){};

   public CocoConfig parseCommandLine( String[] args ) {
      if ( args == null || args.length <= 0 ) {
         String hasFile = null;
         try { hasFile = CocoUtils.getText( DEFAULT_BUILD ); } catch ( IOException ex ) {}
         if ( hasFile != null ) {
            args = new String[]{ DEFAULT_BUILD };
         } else {
            help = HELP_FILE;
         }
      }
      List<String> arguments = Arrays.asList( args );

      if ( contains( Pattern.compile( "/\\?|--help" ) ).test( arguments ) ) {
         help = HELP_FILE;
      } else if ( contains( Pattern.compile( "--license" ) ).test( arguments ) ) {
         help = LGPL_FILE;
      } else {
         runFiles = new ArrayList<>( Arrays.asList( args ) );
      }

      return this;
   }
}
