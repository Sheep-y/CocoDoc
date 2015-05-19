package sheepy.cocodoc.worker.task;

import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.worker.BlockStats;
import sheepy.util.collection.CollectionPredicate;

public class TaskDefine extends Task {

   @Override public Action getAction () { return Action.DEFINE; }

   private static final Predicate<List<String>> validate = CollectionPredicate.<List<String>>size( 1, 2 );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "define() task should have one to two parameters: variable name and an optional value."; }

   @Override protected void run () {
      if ( ! hasParams() ) return;
      String varname = getParam( 0 );
      if ( BlockStats.predefined.contains( varname ) ) {
         throwOrWarn( new CocoRunError( "Cannot override predefined variable \"" + varname + '"' ) );
         return;
      }
      if ( varname.length() >= 5 && varname.startsWith( "__" ) && varname.endsWith( "__" ) ) {
         throwOrWarn( new CocoRunError( "Cannot set or override internal variable \"" + varname + '"' ) );
         return;
      }
      if ( getParams().size() == 1 ) {
         log( Level.FINER, "Delete variable {0}", varname );
         getBlock().stats().setVar( varname, null );
      } else {
         log( Level.FINER, "Set variable {0} to {1}", varname, getParam( 1 ) );
         getBlock().stats().setVar( varname, getParam( 1 ) );
      }
   }
}