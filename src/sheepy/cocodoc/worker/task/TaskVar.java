package sheepy.cocodoc.worker.task;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.CocoUtils;
import static sheepy.cocodoc.CocoUtils.formatTime;
import static sheepy.cocodoc.CocoUtils.milliToZonedDateTime;
import sheepy.cocodoc.worker.directive.Directive;

public class TaskVar extends Task {

   @Override public Action getAction () { return Action.VAR; }

   @Override protected Predicate<List<String>> validParam() { return nonEmpty; }
   @Override protected String invalidParamMessage() { return "var() task should start with parameters mtime, btime, or cocotime."; }

   ZonedDateTime app_build_time;
   @Override protected void run () {
      if ( getParams().isEmpty() ) return;

      String varname = getParam( 0 );
      String value = "";
      if ( getDirective().getTasks().size() == 1 )
         getBlock().setName( "var(" + varname + ")" );

      switch ( varname.toLowerCase() ) {
         case "mtime":
            if ( getDirective().getAction() == Directive.Action.POSTPROCESS )
               value = formatTime( getBlock().getRoot().stats().getModifiedTime() );
            else
               value = "<?coco-postprocess " + toString() + " ?>";
            break;

         case "btime":
            value = formatTime( getBlock().stats().getBuildTime() );
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
      getBlock().setText( value );
   }
}