package sheepy.cocodoc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CocoConfig {

   public String help;
   public List<String> runFiles = new ArrayList<>(0);

   public CocoConfig parseCommandLine( String[] args ) {
      if ( Arrays.stream( args ).map( String::toLowerCase ).anyMatch( a -> "/?".equals(a) || "--help".equals(a) ) ) {
         help = "";
      } else {
         runFiles = new ArrayList<>( Arrays.asList( args ) );
      }
      return this;
   }
}
