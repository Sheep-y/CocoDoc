package sheepy.cocodoc.worker.task;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.CocoUtils;
import static sheepy.cocodoc.CocoUtils.formatTime;
import static sheepy.cocodoc.CocoUtils.milliToZonedDateTime;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.BlockStats;

public class TaskVar extends Task {

   @Override public Action getAction () { return Action.VAR; }

   @Override protected Predicate<List<String>> validParam() { return nonEmpty; }
   @Override protected String invalidParamMessage() { return "var() task should start with parameters mtime, btime, or cocotime."; }

   ZonedDateTime app_build_time;
   @Override protected void run () {
      if ( ! hasParams() ) {
         log( Level.INFO, "Skipping var(), no parameter" );
         return;
      }

      Block block = getBlock();
      String varname = getParam( 0 );
      Object value = null;
      log( Level.FINER, "Processing variable {0}", varname );
      if ( getDirective().getTasks().size() == 1 )
         block.setName( "var(" + varname + ")" );

      switch ( varname ) {
         case BlockStats.TIME_NOW:
            value = ZonedDateTime.now();
            break;

         case BlockStats.TIME_COCO:
            if ( app_build_time == null ) {
               long epoch = CocoUtils.getBuildTime().orElse( 0l );
               if ( epoch > 0 )
                  value = milliToZonedDateTime( epoch );
            }
            break;

         case BlockStats.TIME_LAST_MOD :
            if ( ! isPostProcess() ) {
               setPostProcess( "Last modified time" );
               return;
            } // Otherwise fallthrough

         case BlockStats.TIME_BUILD:
            // Time build is a variable, so fall through
         default:
            if ( ! block.stats().hasVar( varname ) ) {
               if ( ! isPostProcess() ) {
                  setPostProcess( "var" );
                  return;
               } else // Otherwise fallthrough
                  throwOrWarn( new CocoRunError( "Variable not found: " + varname ) );
            }
            value = block.stats().getVar( varname );
      }

      if ( value == null ) {
         value = "";
      } else if ( value instanceof ZonedDateTime ) {
         value = formatTime( (ZonedDateTime) value );
      }

      if ( value.toString().startsWith( "<?coco-postprocess " ) )
         log( Level.FINEST, "Deferred variable {0} to post-process", varname );
      else
         log( Level.FINEST, "Applied variable {0}: {1}", varname, value );

      getBlock().getText().append( value.toString() );
   }
}