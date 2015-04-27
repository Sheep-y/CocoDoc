package sheepy.cocodoc.worker.task;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.CocoUtils;
import static sheepy.cocodoc.CocoUtils.formatTime;
import static sheepy.cocodoc.CocoUtils.milliToZonedDateTime;

public class TaskVar extends Task {

   @Override public Action getAction () { return Action.VAR; }

   @Override protected Predicate<List<String>> validParam() { return nonEmpty; }
   @Override protected String invalidParamMessage() { return "var() task should start with parameters mtime, btime, or cocotime."; }

   ZonedDateTime app_build_time;
   @Override public void run () {
      if ( getParams().isEmpty() ) return;

      String varname = getParam( 0 );
      String value = "";
      switch ( varname.toLowerCase() ) {
         case "mtime":
            // TODO: wait until second pass
            value = formatTime( getBlock().getRoot().getModifiedTime() );
            break;

         case "btime":
            value = formatTime( getBlock().getBuildTime() );
            break;

         case "now":
            value = formatTime( ZonedDateTime.now() );
            break;

         case "cocotime":
            if ( app_build_time == null ) {
               long epoch = CocoUtils.getBuildTime().orElse( 0l );
               if ( epoch > 0 )
                  value = formatTime( milliToZonedDateTime( epoch ) );
            }
            break;

         default:
            log.log( Level.WARNING, "Unknown variable {0}", varname );
            return;
      }
      getBlock().getText().append( value );
   }
}