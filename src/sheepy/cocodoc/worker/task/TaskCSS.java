package sheepy.cocodoc.worker.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import sheepy.cocodoc.CocoRunError;
import static sheepy.cocodoc.worker.task.Task.nonEmpty;
import static sheepy.util.collection.CollectionPredicate.noDuplicate;
import static sheepy.util.collection.CollectionPredicate.onlyContains;
import sheepy.util.concurrent.ObjectPoolMap;

public class TaskCSS extends JSTask {
   @Override public Action getAction () { return Action.CSS; }

   private static final String[] validParams = new String[]{ "less", "minify", "uglyifycss" };
   private static final Predicate<List<String>> validate = nonEmpty.and( onlyContains( Arrays.asList( validParams ) ) ).and( noDuplicate() );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "css() task takes only 'less' or 'minify' as parameter."; }

   @Override protected void run () {
      super.run();
      List<String> params = new ArrayList( getParams() );
      while ( ! params.isEmpty() ) {
         String p = params.remove( 0 ).toLowerCase();
         switch ( p ) {
            case "minify":
            case "uglyifycss":
               action();
               action = this::uglifyCSS;
               break;
            default:
               action_param.add( p );
         }
      }
      action();
   }

   public String uglifyCSS ( List<String> params ) {
      return run( "UglifyCSS", params, context -> {
         try {
            return context.js.eval( "uglifycss.processString( code )" ).toString();
         } catch ( ScriptException ex ) {
            throw new CocoRunError( "Cannot Uglify CSS", ex );
         }
      } );
   }

   @Override protected ObjectPoolMap<String, Object> getPool() { return enginePool; }
   protected static final ObjectPoolMap<String, Object> enginePool = ObjectPoolMap.create(
      ( key ) -> {
         ScriptEngine js = newJS();
         try {
            switch ( key ) {
               case "UglifyCSS" :
                  loadJS( js, "res/uglifycss/uglifycss-lib.js" );
                  break;
            }
            return js;
         } catch ( ScriptException | IOException ex ) {
            return new CocoRunError( "Cannot load " + key, ex );
         }
      } );
}