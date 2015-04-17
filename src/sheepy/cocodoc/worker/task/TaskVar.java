package sheepy.cocodoc.worker.task;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;

public class TaskVar extends Task {

   @Override public Action getAction () { return Action.VAR; }

   @Override protected Predicate<List<String>> validParam() { return nonEmpty; }
   @Override protected String invalidParamMessage() { return "var() task should start with parameters mtime, btime, or cocotime."; }

   @Override public void run () {
      if ( getParams().isEmpty() ) return;

      String varname = getParam( 0 );
      switch ( varname.toLowerCase() ) {
         case "mtime":
            // TODO: distinguish between total mtime, local mtime, and this file mtime.
            getBlock().getText().append( formatTime( Instant.now().atZone( ZoneId.systemDefault() ), getParam( 1 ) ) );
            break;

         case "btime":
            // TODO: use build start time
         case "now":
            getBlock().getText().append( formatTime( Instant.now().atZone( ZoneId.systemDefault() ), getParam( 1 ) ) );
            break;

         case "cocotime":
            // TODO: Implement
            getBlock().getText().append( formatTime( Instant.now().atZone( ZoneId.systemDefault() ), getParam( 1 ) ) );
            /*
            if ( app_build_time == null ) {
               long time = LongStream.of(
                     CocoBuilder.getClassBuildTime( CocoParser.class ).orElse( 0l )
                     , CocoBuilder.getClassBuildTime( HtmlParser.class ).orElse( 0l )
                     , CocoBuilder.getClassBuildTime( CocoBuilder.class ).orElse( 0l ) ).max().orElse( 0l );
               app_build_time = ZonedDateTime.ofInstant( new Date(time).toInstant(), ZoneId.systemDefault() );
            }
            return formatTime( app_build_time, tag.param );
            */
            break;

         default:
            log.log( Level.WARNING, "Unknown variable {0}", varname );
      }
   }

   /**
    * Format time using Java DateTimeFormatter pattern.
    * (Use "iso8601" as shortcut for ISO8601 format)
    */
   private static String formatTime( ZonedDateTime time, String format ) {
      // May need to cache formatter if proven to be time consuming.
      if ( format == null ) format = "iso8601";
      if ( format.toLowerCase().equals( "iso8601" ) ) format = "yyyy-MM-dd'T'HH:mm:ssZ";
      return time.format( DateTimeFormatter.ofPattern( format ) );
   }

}