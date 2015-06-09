package sheepy.cocodoc.worker.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.CocoUtils;
import static sheepy.cocodoc.worker.task.Task.nonEmpty;
import static sheepy.util.collection.CollectionPredicate.noDuplicate;
import static sheepy.util.collection.CollectionPredicate.onlyContains;
import sheepy.util.concurrent.ObjectPoolMap;
import sheepy.util.text.Text;

public class TaskCSS extends Task {
   @Override public Action getAction () { return Action.CSS; }

   private static final String[] validParams = new String[]{ "minify", "uglyifycss" };
   private static final Predicate<List<String>> validate = nonEmpty.and( onlyContains( Arrays.asList( validParams ) ) ).and( noDuplicate() );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "css() task takes only 'minify' parameter."; }

   private Function<List<String>,String> action;
   private List<String> action_param = new ArrayList<>();

   @Override protected void run () {
      if ( ! hasParams() ) {
         log( Level.INFO, "Skipping css(), no parameter" );
         return;
      }
      if ( ! getBlock().hasData() ) {
         log( Level.INFO, "Skipping css(), no content" );
         return;
      }

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

   private void action() {
      if ( action != null ) {
         try {
            action_param.add( 0, getBlock().getText().toString() );
            String result = action.apply( action_param );
            if ( result != null )
               getBlock().setText( result );
         } catch ( CocoRunError ex ) {
            throwOrWarn( ex );
         } finally {
            action = null;
            action_param.clear();
         }
      } else {
         if ( ! action_param.isEmpty() ) {
            throwOrWarn( new CocoRunError( "css() parameters missing action: " + Text.toString( action_param ) ) );
            action_param.clear();
         }
      }
   }

   public String uglifyCSS ( List<String> params ) {
      params = new ArrayList<>( params );
      final String txt = params.remove( 0 );
      log( Level.FINER, "Uglifying CSS {1}", Text.ellipsisWithin( txt, 8 ) );

      final String key = "uglifycss";
      final Object get = enginePool.get( key );
      try {
         if ( get instanceof RuntimeException ) {
            throwOrWarn( (RuntimeException) get );
            return null;
         }

         ScriptEngine js = (ScriptEngine) get;
         js.put( "code", txt );
         log( Level.FINEST, "Uglify CSS: Loaded, now processing {0} chars", txt.length() );

         String result = js.eval( "uglifycss.processString( code )" ).toString();
         log( Level.FINEST, "Uglify CSS: {0} -> {1}", txt.length(), result.length() );

         return result;

      } catch ( ScriptException ex ) {
         throwOrWarn( new CocoRunError( "Cannot uglify css", ex ) );
         return null;

      } finally {
         enginePool.recycle( key, get );
      }
   }

   private static final ObjectPoolMap<String, Object> enginePool = ObjectPoolMap.create(
      ( key ) -> {
         ScriptEngine js = new ScriptEngineManager().getEngineByName( "nashorn" );
         try {
            js.eval( CocoUtils.getText( "res/uglifycss/uglifycss-lib.js" ) );
            return js;
         } catch ( ScriptException | IOException ex ) {
            return new CocoRunError( "Cannot load UglifyCSS", ex );
         }
      }
   );
}