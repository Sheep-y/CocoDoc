package sheepy.cocodoc.worker.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
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

   private Consumer<List<String>> action;
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
            case "uglyifyjs":
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
            action.accept( action_param );
         } catch ( CocoRunError ex ) {
            throwOrWarn( ex );
         } finally {
            action = null;
            action_param.clear();
         }
      } else {
         if ( ! action_param.isEmpty() )
            throwOrWarn( new CocoRunError( "Parameters missing action: " + Text.toString( action_param ) ) );
      }
   }

   private void uglifyCSS ( List<String> params ) {
      String txt = getBlock().getText().toString();
      log( Level.FINER, "Uglifying CSS {1}", Text.ellipsisWithin( txt, 8 ) );
      Object get = enginePool.get( "uglifycss" );
      try {
         if ( get instanceof RuntimeException ) {
            throwOrWarn( (RuntimeException) get );
            return;
         }

         ScriptEngine js = (ScriptEngine) get;
         js.put( "code", txt );
         log( Level.FINEST, "Running Uglify JS" );

         String result = js.eval( "uglifycss.processString( code )" ).toString();
         log( Level.FINEST, "Uglified: {0} -> {1}", txt.length(), result.length() );

         getBlock().setText( result );

      } catch ( ScriptException ex ) {
         throwOrWarn( new CocoRunError( "Cannot uglify css", ex ) );

      } finally {
         enginePool.recycle( "css", get );
      }
   }

   public static final ObjectPoolMap<String, Object> enginePool = ObjectPoolMap.create(
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